package com.catcry.imei.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestEnvelope {

    private List<RequestItem> request;

    public List<RequestItem> getRequest() {
        return request;
    }

    public void setRequest(List<RequestItem> request) {
        this.request = request;
    }
}
