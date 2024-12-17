package DS.teamproject.DrawingEditor.Controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DrawingEditorController {

    @FXML
    private Button shapeButton;

    @FXML
    private Button selectButton;

    @FXML
    private Button moveButton;

    @FXML
    private Button copyButton;

    @FXML
    private Button pasteButton;


    @FXML
    private Button colorButton;


    @FXML
    private ContextMenu colorContextMenu;

    @FXML
    private ColorPicker colorPicker;

    private Color currentColor = Color.BLACK; // 기본 색상은 검정색

    @FXML
    private Button groupButton;

    @FXML
    private Button undoRedoButton;

    // 현재 선택된 기능을 추적하는 변수
    private String currentMode = null;



    @FXML
    private ContextMenu shapeContextMenu;

    @FXML
    private Canvas drawingCanvas;

    private GraphicsContext gc;

    private String selectedShape = null;

    private double startX, startY;  // 이동을 위한 마우스 좌표 보정값

    private List<ShapeRecord> shapes = new ArrayList<>();

    private List<ShapeRecord> clipboard = new ArrayList<>();
    private MouseEvent event;

    //저장기능
    private Button saveButton;
    private Button saveAsButton;
    private Button loadButton;
    private File currentFile = null; // 현재 저장된 파일 경로
    private Stage primaryStage;



    @FXML
    public void initialize() {

        // ColorPicker의 기본 색상을 검정색으로 설정
        colorPicker.setValue(Color.BLACK);

        // 실행 확인
        if (drawingCanvas == null) {
            System.out.println("drawingCanvas is null!");
        } else {
            System.out.println("drawingCanvas initialized successfully.");
            gc = drawingCanvas.getGraphicsContext2D();
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
        }

        // 초기 모드 설정
        resetMode();

        // 그룹화 메뉴 이벤트 설정
        if (groupButton != null) {
            groupButton.setOnMouseClicked(event -> {
                ContextMenu groupMenu = new ContextMenu();
                MenuItem groupItem = new MenuItem("Grouping");
                groupItem.setOnAction(e -> groupSelectedShapes());
                MenuItem ungroupItem = new MenuItem("Ungrouping");
                ungroupItem.setOnAction(e -> ungroupSelectedShapes());
                groupMenu.getItems().addAll(groupItem, ungroupItem);
                groupMenu.show(groupButton, event.getScreenX(), event.getScreenY());
            });
        }


        // ColorPicker 이벤트 핸들러 설정
        colorPicker.setOnAction(event -> handleColorChange());

        // 도형 선택 확인 메세지 띄우게
        for (MenuItem item : shapeContextMenu.getItems()) {
            item.setOnAction(event -> {
                selectedShape = item.getText();
                System.out.println("Selected Shape: " + selectedShape);
            });
        }


    }

    private void resetMode() {
        // 모든 이벤트 핸들러 초기화
        drawingCanvas.setOnMousePressed(null);
        drawingCanvas.setOnMouseDragged(null);
        drawingCanvas.setOnMouseReleased(null);
        drawingCanvas.setOnMouseClicked(null);

        // 현재 모드 초기화
        currentMode = null;
    }

    //Select r구현 컨트롤러
    @FXML
    private void handleSelectButtonClick(MouseEvent event) {
        resetMode();
        currentMode = "Select";
        System.out.println("Select mode activated");


        // Select 모드에 필요한 이벤트 핸들러 설정
        drawingCanvas.setOnMousePressed(this::startSelect);
        drawingCanvas.setOnMouseDragged(this::performDragSelect);
        drawingCanvas.setOnMouseReleased(this::handleCanvasRelease);
        drawingCanvas.setOnMouseClicked(this::handleCanvasClick);
    }

    @FXML
    private void activateShapeMode() {
        resetMode();
        currentMode = "Shape";
        System.out.println("Shape mode activated");

        // Shape 모드에 필요한 이벤트 핸들러 설정
        drawingCanvas.setOnMousePressed(this::startDrawing);
        drawingCanvas.setOnMouseDragged(this::drawShape);
        drawingCanvas.setOnMouseReleased(this::finalizeShape);
    }


    //select 컨트롤러

    private List<ShapeRecord> selectedShapes = new ArrayList<>();

    private double dragStartX, dragStartY;

    private void handleCanvasClick(MouseEvent event) {
        if (!"Select".equals(currentMode)) return;

        double clickX = event.getX();
        double clickY = event.getY();

        selectedShapes.clear();

        int clickedGroupId = -1;

        // 그룹 외곽선 먼저 검사
        for (ShapeRecord shape : shapes) {
            if (shape.groupId != -1 && isPointInsideRectangle(clickX, clickY, shape.startX, shape.startY, shape.endX, shape.endY)) {
                clickedGroupId = shape.groupId;
                break;
            }
        }

        if (clickedGroupId != -1) {
            // 그룹 전체 선택
            for (ShapeRecord shape : shapes) {
                if (shape.groupId == clickedGroupId) {
                    selectedShapes.add(shape);
                }
            }
        } else {
            // 개별 도형 선택
            for (ShapeRecord shape : shapes) {
                if (isPointInsideShape(clickX, clickY, shape)) {
                    selectedShapes.add(shape);
                    break;
                }
            }
        }

        for (int i = shapes.size() - 1; i >= 0; i--) {
            ShapeRecord shape = shapes.get(i);
            if (isPointInsideShape(clickX, clickY, shape)) {
                selectedShapes.add(shape);
                break;
            }
        }

        redrawCanvas();
        highlightShapes();
    }

















    // Select 모드: 드래그로 다중 선택
    private double finalStartX, finalStartY, finalEndX, finalEndY;

    // Select 모드: 드래그로 다중 선택
    private void startSelect(MouseEvent event) {
        if (!"Select".equals(currentMode)) return;

        // 시작점을 도형 두께를 고려해 보정
        dragStartX = Math.max(2, Math.min(event.getX(), drawingCanvas.getWidth() - 2));
        dragStartY = Math.max(2, Math.min(event.getY(), drawingCanvas.getHeight() - 2));

        selectedShapes.clear();
    }



    private void performDragSelect(MouseEvent event) {
        if (!"Select".equals(currentMode)) return;

        // 드래그 끝점 보정
        double dragEndX = Math.max(2, Math.min(event.getX(), drawingCanvas.getWidth() - 2));
        double dragEndY = Math.max(2, Math.min(event.getY(), drawingCanvas.getHeight() - 2));

        // 드래그 영역 계산
        finalStartX = Math.min(dragStartX, dragEndX);
        finalStartY = Math.min(dragStartY, dragEndY);
        finalEndX = Math.max(dragStartX, dragEndX);
        finalEndY = Math.max(dragStartY, dragEndY);

        // 드래그 영역 시각화
        redrawCanvas();
        gc.setStroke(Color.web("#33FF04"));
        gc.setLineWidth(2);
        gc.setLineDashes(10);
        gc.strokeRect(finalStartX, finalStartY, finalEndX - finalStartX, finalEndY - finalStartY);
        gc.setLineDashes(null);

        // 드래그 영역 내에 완전히 포함된 도형 감지
        selectedShapes.clear();
        for (ShapeRecord shape : shapes) {
            if (isShapeFullyInsideBounds(shape, finalStartX, finalStartY, finalEndX, finalEndY)) {
                selectedShapes.add(shape);
            }
        }

        highlightShapes(); // 드래그 중에도 선택된 도형 강조
    }

    private boolean isShapeFullyInsideBounds(ShapeRecord shape, double startX, double startY, double endX, double endY) {
        switch (shape.type) {
            case "➖ Line":
                // 선의 시작점과 끝점이 모두 드래그 영역 안에 있어야 함
                return isPointInsideRectangle(shape.startX, shape.startY, startX, startY, endX, endY) &&
                        isPointInsideRectangle(shape.endX, shape.endY, startX, startY, endX, endY);

            case "⭕ Circle":
            case "⏹ Rectangle":
                // 사각형 또는 원의 경계가 드래그 영역에 완전히 포함되어야 함
                return shape.startX >= startX && shape.endX <= endX &&
                        shape.startY >= startY && shape.endY <= endY;

            default:
                return false;
        }
    }

    private boolean isPointInsideRectangle(double x, double y, double startX, double startY, double endX, double endY) {
        return x >= startX && x <= endX && y >= startY && y <= endY;
    }

    private void handleCanvasRelease(MouseEvent event) {
        if ("Select".equals(currentMode)) {
            // 드래그 종료 시 선택된 도형 유지
            redrawCanvas();
            highlightShapes();
        }
    }

    private void highlightShapes() {
        gc.setStroke(Color.web("#33FF04")); // 점선 색상
        gc.setLineWidth(2);
        gc.setLineDashes(10); // 점선 설정

        Map<Integer, double[]> groupBounds = new HashMap<>();

        for (ShapeRecord shape : selectedShapes) {
            if (shape.groupId != -1) {
                // 그룹 경계 계산
                groupBounds.putIfAbsent(shape.groupId, new double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_VALUE});
                double[] bounds = groupBounds.get(shape.groupId);

                bounds[0] = Math.min(bounds[0], shape.startX); // minX
                bounds[1] = Math.min(bounds[1], shape.startY); // minY
                bounds[2] = Math.max(bounds[2], shape.endX);   // maxX
                bounds[3] = Math.max(bounds[3], shape.endY);   // maxY
            } else {
                // 개별 도형 강조
                switch (shape.type) {
                    case "➖ Line":
                        // 선분을 따라 점선 강조
                        gc.strokeLine(shape.startX, shape.startY, shape.endX, shape.endY);
                        break;

                    case "⭕ Circle":
                        double centerX = shape.startX;
                        double centerY = shape.startY;
                        double width = shape.endX - shape.startX;
                        double height = shape.endY - shape.startY;
                        double size = Math.min(width, height);
                        gc.strokeOval(centerX, centerY, size, size);
                        break;

                    case "⏹ Rectangle":
                        gc.strokeRect(shape.startX, shape.startY, shape.endX - shape.startX, shape.endY - shape.startY);
                        break;
                }
            }
        }

        // 그룹 외곽선 그리기
        for (double[] bounds : groupBounds.values()) {
            double x = bounds[0];
            double y = bounds[1];
            double width = bounds[2] - bounds[0];
            double height = bounds[3] - bounds[1];
            gc.strokeRect(x, y, width, height);
        }


        gc.setLineDashes(null);
        gc.setStroke(Color.BLACK);
    }

    // 점이 드래그 범위 안에 있는지 확인
    private boolean isPointInBounds(double x, double y, double startX, double startY, double endX, double endY) {
        return x >= startX && x <= endX && y >= startY && y <= endY;
    }


    // 점이 도형 안에 있는지 확인
    private boolean isPointInsideShape(double x, double y, ShapeRecord shape) {
        switch (shape.type) {
            case "➖ Line":
                return isPointNearLine(x, y, shape.startX, shape.startY, shape.endX, shape.endY);
            case "⭕ Circle":
            case "⏹ Rectangle":
                return x >= shape.startX && x <= shape.endX && y >= shape.startY && y <= shape.endY;
            default:
                return false;
        }
    }

    private boolean isShapeInsideBounds(ShapeRecord shape, double startX, double startY, double endX, double endY) {
        // 도형의 좌표가 드래그 영역 안에 있는지 확인
        return isPointInBounds(shape.startX, shape.startY, startX, startY, endX, endY) &&
                isPointInBounds(shape.endX, shape.endY, startX, startY, endX, endY);
    }

    // 점이 선 근처에 있는지 확인
    private boolean isPointNearLine(double x, double y, double x1, double y1, double x2, double y2) {
        final double TOLERANCE = 5.0;
        double distance = Math.abs((y2 - y1) * x - (x2 - x1) * y + x2 * y1 - y2 * x1) /
                Math.hypot(y2 - y1, x2 - x1);
        return distance <= TOLERANCE;
    }

