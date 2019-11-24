package com.querybuilder.home.lab;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AliasRow {
    private String name;
    private String alias;
    private List<String> values;

    public AliasRow(String name, String alias) {
        this.name = name;
        this.alias = alias;
        values = new ArrayList<>();
    }
}
