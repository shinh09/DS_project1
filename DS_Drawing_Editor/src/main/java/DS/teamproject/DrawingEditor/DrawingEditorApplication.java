package DS.teamproject.DrawingEditor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DrawingEditorApplication extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("drawingeditor2.fxml"));

        // 창 크기 설정
        primaryStage.setTitle("Drawing Editor");
        primaryStage.setScene(new Scene(root, 640, 515)); // 너비와 높이를 원하는 크기로 설정
        primaryStage.setResizable(false); // 창 크기 조절 비활성화
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
