package com.querybuilder.home.lab.utils;


import com.querybuilder.home.lab.domain.TableRow;
import javafx.scene.control.TreeTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class CustomCell extends TreeTableCell<TableRow, TableRow> {
    private final ImageView element = getImage("/myToolWindow/element.png");
    private final ImageView table = getImage("/myToolWindow/table.png");
    private final ImageView nestedQuery = getImage("/myToolWindow/nestedQuery.png");

    private static ImageView getImage(String resourcePath) {
        return new ImageView(new Image(Utils.class.getResourceAsStream(resourcePath)));
    }

    @Override
    protected void updateItem(TableRow item, boolean empty) {
        super.updateItem(item, empty);
        setItem(this, item, empty);
    }

    public void setItem(TreeTableCell<TableRow, TableRow> cell, TableRow item, boolean empty) {
        cell.setText(empty ? null : item.getName());
        // icons
        if (empty) {
            cell.setGraphic(null);
        } else if (item.isNested()) {
            cell.setGraphic(nestedQuery);
        } else {
            cell.setGraphic(item.isRoot() ? table : element);
        }
    }
}
