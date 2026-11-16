package com.catcry.imei.service;

import com.catcry.imei.dto.response.ImeiResponse;
import com.catcry.imei.dto.response.ImeiResponse.Envelope;
import com.catcry.imei.dto.response.ImeiResponse.ErrorDetail;
import com.catcry.imei.dto.response.ImeiResponse.Summary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ImeiResponseFactory {

    // Type 1: new batch, all IMEIs processed
    public Envelope success(String conversationId, String batchId, String operation, int qty) {
        return wrap(conversationId, 200, "SUCCESS", false, batchId, operation,
                "All IMEIs processed successfully.", null,
                new Summary(qty, qty, 0), List.of());
    }

    // Type 2: conversationId or batchId was already processed
    public Envelope duplicate(String conversationId, String batchId, String operation,
                              int qty, long firstProcessedAt) {
        return wrap(conversationId, 200, "DUPLICATE_IGNORED", true, batchId, operation,
                "Request ignored: conversationId and batchId have already been processed.",
                firstProcessedAt, new Summary(qty, 0, 0), List.of());
    }

    // Type 3: reused id with a different payload
    public Envelope integrityMismatch(String conversationId, String batchId) {
        return wrap(conversationId, 409, "IDEMPOTENCY_MISMATCH", true, batchId, null,
                "Reused conversationId/batchId with different payload or checksum.",
                null, null,
                List.of(new ErrorDetail("PAYLOAD_MUTATION_DETECTED",
                        "Checksum does not match the original recorded batch execution.")));
    }

    // Types 4-6 and auth/validation errors
    public Envelope error(String conversationId, String batchId, int httpStatus,
                          String status, String message, ErrorDetail... errors) {
        return wrap(conversationId, httpStatus, status, false, batchId, null,
                message, null, null, List.of(errors));
    }

    private Envelope wrap(String conversationId, int statusCode, String status,
                          boolean duplicate, String batchId, String operation,
                          String message, Long firstProcessedAt,
                          Summary summary, List<ErrorDetail> errors) {

        ImeiResponse.Header header = new ImeiResponse.Header(
                "IMEI_WHITELIST_RESPONSE",
                "IMEI_PROVISIONING_SERVICE",
                System.currentTimeMillis(),
                "EVT-SYS-" + UUID.randomUUID().toString().substring(0, 7).toUpperCase());

        ImeiResponse.Result result = new ImeiResponse.Result(
                statusCode, status, duplicate, batchId, operation, message,
                firstProcessedAt, summary, errors);

        return new Envelope(List.of(new ImeiResponse.Item(
                new ImeiResponse.ControlHeader(conversationId), header, result)));
    }
}
