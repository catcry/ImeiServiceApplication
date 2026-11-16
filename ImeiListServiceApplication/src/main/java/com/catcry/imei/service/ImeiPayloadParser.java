package com.catcry.imei.service;



import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.catcry.imei.dto.request.PayloadItem;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImeiPayloadParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Converts [{a:"username", v:"eventa"}, ...] into a map {username=eventa, ...} */
    public Map<String, String> toMap(List<PayloadItem> payload) {
        Map<String, String> map = new HashMap<>();
        if (payload != null) {
            for (PayloadItem item : payload) {
                if (item.getA() != null) {
                    map.put(item.getA(), item.getV());
                }
            }
        }
        return map;
    }

    /** imei_list is a JSON string inside a JSON string, so it is parsed a second time */
    public List<String> parseImeiList(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("imei_list is missing");
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException("imei_list must be a JSON array of strings");
        }
    }
}
