package com.querybuilder.domain.qparts;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class FullQuery {
    private Map<String, OneCte> cteMap = new HashMap<>();
}
