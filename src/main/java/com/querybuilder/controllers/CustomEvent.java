package com.querybuilder.controllers;

import lombok.Data;

@Data
public class CustomEvent {
    private String name;
    private String data;
    private Integer currentRow;
}