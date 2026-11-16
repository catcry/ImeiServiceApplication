package com.catcry.imei.service;


import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CsvExportService {

    private static final String HEADER = "IMEI";

    public String toCsv(List<String> imeis) {
        StringBuilder csv = new StringBuilder(HEADER).append("\n");
        for (String imei : imeis) {
            csv.append(imei).append("\n");
        }
        return csv.toString();
    }
}
