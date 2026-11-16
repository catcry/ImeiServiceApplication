package com.catcry.imei.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ImeiResponse {

    public record Envelope(List<Item> response) {}

    public record Item(ControlHeader controlHeader, Header header, Result result) {}

    public record ControlHeader(String conversationId) {}

    public record Header(String eventType, String eventSource, long timeStamp, String eventId) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Result(
            int statusCode,
            String status,
            @JsonProperty("isDuplicate") boolean isDuplicate,
            String batchId,
            String operation,
            String message,
            Long firstProcessedAt,
            Summary summary,
            List<ErrorDetail> errors) {}

    public record Summary(int requestedQty, int processedQty, int rejectedQty) {}

    public record ErrorDetail(String code, String message) {}
}
