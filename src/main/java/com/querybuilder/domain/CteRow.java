package com.querybuilder.domain;

import lombok.Data;

@Data
public class CteRow {
    private String name;
    private String id;

    public CteRow(String name, String id) {
        this.name = name;
        this.id = id;
    }
}
