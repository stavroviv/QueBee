package com.querybuilder.domain;

import javafx.scene.control.TreeItem;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DBTables {
    private Map<String, List<String>> dbElements;
    private TreeItem<TableRow> root;
}
