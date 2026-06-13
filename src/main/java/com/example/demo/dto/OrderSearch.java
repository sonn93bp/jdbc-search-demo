package com.example.demo.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class OrderSearch {
    private String refNo;
    private String orderNo;
    private String customerName;
    private LocalDateTime orderDate;
    private String status;
}
