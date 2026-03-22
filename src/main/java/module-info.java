module com.buildmat {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires java.sql;
    requires java.desktop;
    requires org.slf4j;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    opens com.buildmat to javafx.graphics;
    opens com.buildmat.ui to javafx.graphics;
}
