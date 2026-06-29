package com.example.demo.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

@Getter
@Setter
@Builder
public class AdvanceResult<T> {
    private int totalSuccess;
    private int totalFailure;
    Page<T> page;
}
