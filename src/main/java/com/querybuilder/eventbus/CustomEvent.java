package com.querybuilder.eventbus;

import com.querybuilder.domain.TableRow;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TreeItem;
import lombok.Data;

@Data
public class CustomEvent {
    private String name;
    private String data;
    private Integer currentRow;
    private ListChangeListener.Change<? extends TreeItem<TableRow>> change;
}
