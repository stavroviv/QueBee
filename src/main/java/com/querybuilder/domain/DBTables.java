package com.querybuilder.domain;

import javafx.scene.control.TreeItem;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class DBTables {
    private Map<String, List<String>> dbElements = new HashMap<>();
    private TreeItem<TableRow> root = new TreeItem<>();
}
