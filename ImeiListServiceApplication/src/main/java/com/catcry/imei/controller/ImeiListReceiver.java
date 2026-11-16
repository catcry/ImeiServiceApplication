package com.catcry.imei.controller;

import com.catcry.imei.dto.request.RequestEnvelope;
import com.catcry.imei.dto.request.RequestItem;
import com.catcry.imei.dto.response.ImeiResponse;
import com.catcry.imei.service.AuthenticationService;
import com.catcry.imei.service.ImeiChecksumService;
import com.catcry.imei.service.ImeiPayloadParser;
import com.catcry.imei.service.ImeiResponseFactory;
import com.catcry.imei.service.ImeiFileStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletRequest;


import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/imei")
public class ImeiListReceiver {

    private final int maxBatchSize;
    //private static final int MAX_BATCH_SIZE = 1000;
    private final ImeiFileStorageService fileStorageService;
    private static final Set<String> ALLOWED_OPERATIONS = Set.of("ADD", "DELETE");

    private final AuthenticationService authenticationService;
    private final ImeiPayloadParser payloadParser;
    private final ImeiChecksumService checksumService;
    private final ImeiResponseFactory responseFactory;

    public ImeiListReceiver(AuthenticationService authenticationService,
                            ImeiPayloadParser payloadParser,
                            ImeiChecksumService checksumService,
                            ImeiResponseFactory responseFactory,
                            ImeiFileStorageService fileStorageService,
                            @Value("${imei.batch.max-size}") int maxBatchSize) {
        this.authenticationService = authenticationService;
        this.payloadParser = payloadParser;
        this.checksumService = checksumService;
        this.responseFactory = responseFactory;
        this.fileStorageService = fileStorageService;
        this.maxBatchSize = maxBatchSize;
    }

    @PostMapping(value = "/receive", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImeiResponse.Envelope> receive(@RequestBody RequestEnvelope envelope, HttpServletRequest req) {

        if (envelope.getRequest() == null || envelope.getRequest().isEmpty()) {
            return fail(400, "INVALID_REQUEST", null, null,
                    "MISSING_REQUEST", "Missing 'request' array.");
        }

        RequestItem item = envelope.getRequest().get(0);
        String conversationId = conversationIdOf(item);

        try {
            Map<String, String> payload = payloadParser.toMap(item.getPayload());
            String batchId = payload.get("batchId");

            // 1. Authentication
            if (!authenticationService.isValid(payload.get("username"), payload.get("password"))) {
                return fail(401, "UNAUTHORIZED", conversationId, batchId,
                        "AUTH_FAILED", "Invalid username or password.");

            }
            req.setAttribute("auditUser", payload.get("username"));
            req.setAttribute("auditBatchId", batchId);

            // 2. Operation
            String operation = payload.get("operation");
            if (operation == null || !ALLOWED_OPERATIONS.contains(operation)) {
                return fail(400, "INVALID_REQUEST", conversationId, batchId,
                        "INVALID_OPERATION", "operation must be ADD or DELETE.");
            }

            // 3. Parse the IMEI list (throws IllegalArgumentException if malformed)
            List<String> imeis = payloadParser.parseImeiList(payload.get("imei_list"));

            // 4. Batch size limit
            if (imeis.size() > maxBatchSize) {
                return fail(413, "BATCH_SIZE_EXCEEDED", conversationId, batchId,
                        "LIMIT_EXCEEDED",
                        "Maximum allowed IMEIs per batch is " + maxBatchSize
                                + ". Received " + imeis.size() + ".");
            }

            // 5. qty must be a number and equal the list length
            int qty;
            try {
                qty = Integer.parseInt(payload.get("qty").trim());
            } catch (NullPointerException | NumberFormatException e) {
                return fail(400, "DATA_INTEGRITY_ERROR", conversationId, batchId,
                        "QTY_INVALID", "qty must be a number.");
            }
            if (qty != imeis.size()) {
                return fail(400, "DATA_INTEGRITY_ERROR", conversationId, batchId,
                        "QTY_MISMATCH", "Declared qty does not match imei_list length.");
            }

            // 6. Checksum must match the list
            if (!checksumService.matches(imeis, payload.get("imeiListChecksum"))) {
                return fail(400, "DATA_INTEGRITY_ERROR", conversationId, batchId,
                        "CHECKSUM_MISMATCH",
                        "Calculated SHA-256 does not match imeiListChecksum.");
            }
            // Operation must be ADD or DELETE (existing check)
            if (!"ADD".equals(operation) && !"DELETE".equals(operation)) {
                return fail(400, "INVALID_REQUEST", conversationId, batchId,
                        "INVALID_OPERATION", "operation must be ADD or DELETE");
            }

            // DELETE is not implemented yet
            if ("DELETE".equals(operation)) {
                return fail(501, "NOT_IMPLEMENTED", conversationId, batchId,
                        "UNDER_CONSTRUCTION", "under construction");
            }
            // 7. TODO (database step): look up conversationId/batchId here.
            //    - same id + same checksum already done  -> responseFactory.duplicate(...)
            //    - same id + different checksum          -> responseFactory.integrityMismatch(...)
            //    Until then, every valid request is treated as new.
            fileStorageService.save(operation, batchId, imeis);
            // 8. Success
            return ResponseEntity.ok(
                    responseFactory.success(conversationId, batchId, operation, imeis.size()));

        } catch (IllegalArgumentException e) {
            return fail(400, "INVALID_REQUEST", conversationId, null,
                    "INVALID_IMEI_LIST", e.getMessage());
        } catch (RuntimeException e) {
            return fail(500, "INTERNAL_SERVER_ERROR", conversationId, null,
                    "INTERNAL_ERROR", "Unexpected server error. Please retry later.");
        }
    }

    private ResponseEntity<ImeiResponse.Envelope> fail(int httpStatus, String status,
                                                       String conversationId, String batchId,
                                                       String code, String message) {
        return ResponseEntity.status(httpStatus).body(
                responseFactory.error(conversationId, batchId, httpStatus, status, message,
                        new ImeiResponse.ErrorDetail(code, message)));
    }

    private String conversationIdOf(RequestItem item) {
        return item.getControlHeader() != null ? item.getControlHeader().getConversationId() : null;
    }
}
