package com.querybuilder.home.lab;

import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.Map;

public interface DBStructure {
    TreeItem<String> getDBStructure();
    Map<String, List<String>> getDbElements();
}
