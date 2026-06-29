package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
public class CountRecord extends BaseCount{
    private int totalSuccess;
    private int totalFailure;
}
