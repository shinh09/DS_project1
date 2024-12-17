module DS.teamproject.DrawingEditor {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;
    requires com.google.gson;

    opens DS.teamproject.DrawingEditor to javafx.fxml;
    exports DS.teamproject.DrawingEditor;
    exports DS.teamproject.DrawingEditor.Controller;
    opens DS.teamproject.DrawingEditor.Controller to javafx.fxml,com.google.gson;
}