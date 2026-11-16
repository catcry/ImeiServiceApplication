package com.catcry.imei.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestItem {

    private ControlHeader controlHeader;
    private Header header;
    private List<PayloadItem> payload;

    public ControlHeader getControlHeader() {
        return controlHeader;
    }

    public void setControlHeader(ControlHeader controlHeader) {
        this.controlHeader = controlHeader;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public List<PayloadItem> getPayload() {
        return payload;
    }

    public void setPayload(List<PayloadItem> payload) {
        this.payload = payload;
    }
}
