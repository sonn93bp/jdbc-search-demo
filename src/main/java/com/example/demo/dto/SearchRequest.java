package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SearchRequest {
    String refNo;
    String orderNo;
    String from;
    String to;
    String customerName;
    String status;
    Map<String, String> selectFields; // key field name, value alias

    PageRequest pageRequest;

    @Builder
    @Data
    public static class PageRequest {
        int page;
        int size;
        String sort;
        String order;
    }
}
