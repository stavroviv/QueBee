package com.querybuilder.domain;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class AliasRow {
    private String name;
    private String alias;
    private Map<String, String> values; // column name - column value
    private Map<String, Long> ids;
    private long id;

    public AliasRow(String name, String alias) {
        this.name = name;
        this.alias = alias;
        values = new HashMap<>();
        ids = new HashMap<>();
    }
}
