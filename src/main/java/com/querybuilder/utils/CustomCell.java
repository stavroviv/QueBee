package com.querybuilder.utils;


import com.querybuilder.domain.TableRow;
import com.querybuilder.querypart.FromTables;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TreeTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.querybuilder.utils.Utils.isEmptySearchText;

public class CustomCell extends TreeTableCell<TableRow, TableRow> {
    private final ImageView element = getImage("/images/element.png");
    private final ImageView table = getImage("/images/table.png");
    private final ImageView nestedQuery = getImage("/images/nestedQuery.png");
    private final ImageView cteRoot = getImage("/images/cte_group.png");
    private final ImageView cte = getImage("/images/cte.png");
    FromTables controller;

    public CustomCell() {
    }

    public CustomCell(FromTables controller) {
        this.controller = controller;
    }

    private static ImageView getImage(String resourcePath) {
        return new ImageView(new Image(Utils.class.getResourceAsStream(resourcePath)));
    }

    @Override
    protected void updateItem(TableRow item, boolean empty) {
        super.updateItem(item, empty);
        setItem(this, item, empty);
    }

    protected void setItem(TreeTableCell<TableRow, TableRow> cell, TableRow item, boolean empty) {
        if (item == null || empty) {
            cell.setText(null);
            cell.setGraphic(null);
            return;
        }

        // icons
        Node graphic;
        if (item.isCteRoot()) {
            graphic = cteRoot;
        } else if (item.isCte()) {
            graphic = cte;
        } else if (item.isNested()) {
            graphic = nestedQuery;
        } else {
            graphic = item.isRoot() ? table : element;
        }

        if (controller != null && setFoundText(cell, item, graphic)) {
            return;
        }

        cell.setText(item.getName());
        cell.setGraphic(graphic);
    }

    private boolean setFoundText(TreeTableCell<TableRow, TableRow> cell, TableRow item, Node graphic) {
        String text = controller.getSearchField().getText();
        if (isEmptySearchText(text)) {
            return false;
        }

        HBox box = new HBox();
        box.getChildren().add(graphic);

        int curr = 0;
        String str = item.getName();
        Pattern pattern = Pattern.compile("(?i)" + text);
        Matcher matcher = pattern.matcher(str);

        while (matcher.find()) {

            String substring = str.substring(curr, matcher.start());
            if (!substring.isEmpty()) {
                Label text1 = new Label(substring);
                box.getChildren().add(text1);
            }

            Label textFound = new Label(str.substring(matcher.start(), matcher.end()));
            textFound.setStyle("-fx-text-fill: rgba(0,55,213,0.86);-fx-font-weight: bold;");
            box.getChildren().add(textFound);

            curr = matcher.end();
        }

        String substring = str.substring(curr);
        if (!substring.isEmpty()) {
            Label text1 = new Label(substring);
            box.getChildren().add(text1);
        }

        cell.setGraphic(box);
        cell.setText(" "); // not correct tree output without empty text

        return true;
    }
}
