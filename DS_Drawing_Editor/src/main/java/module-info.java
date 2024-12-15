module com.example.ds_drawing_editor {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.desktop;

    opens com.example.ds_drawing_editor to javafx.fxml;
    exports com.example.ds_drawing_editor;
    exports com.example.ds_drawing_editor.Controller;
    opens com.example.ds_drawing_editor.Controller to javafx.fxml;
}