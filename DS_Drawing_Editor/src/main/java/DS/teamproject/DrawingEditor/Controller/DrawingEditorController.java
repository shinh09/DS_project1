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


    //------------------------------select 기능----------------------------
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

        // 드래그 영역 내에 완전히 포함된 도형 선택
        List<ShapeRecord> tempSelectedShapes = new ArrayList<>();
        for (ShapeRecord shape : shapes) {
            if (isShapeFullyInsideBounds(shape, finalStartX, finalStartY, finalEndX, finalEndY)) {
                tempSelectedShapes.add(shape);
            }
        }

        selectedShapes.clear();
        selectedShapes.addAll(tempSelectedShapes);

        highlightShapes(); // 드래그 중 선택된 도형 강조
    }

    private boolean isPointInsideRectangle(double x, double y, double startX, double startY, double endX, double endY) {
        return x >= startX && x <= endX && y >= startY && y <= endY;
    }

    private void handleCanvasRelease(MouseEvent event) {
        if ("Select".equals(currentMode)) {

            for(ShapeRecord shape : selectedShapes){
                redrawCanvas();
                highlightShapes();

            }
            // 드래그 종료 후 선택된 도형을 확정하고 시각화

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
                final double padding = 5.0; // 도형 외곽선에 패딩 추가

                switch (shape.type) {
                    case "➖ Line":
                        // 선분을 따라 점선 강조 (패딩 추가)
                        double dx = shape.endX - shape.startX;
                        double dy = shape.endY - shape.startY;
                        double length = Math.hypot(dx, dy);
                        double padX = padding * (dx / length);
                        double padY = padding * (dy / length);

                        gc.strokeLine(shape.startX - padX, shape.startY - padY, shape.endX + padX, shape.endY + padY);
                        break;

                    case "⭕ Circle":
                        // 원을 감싸는 네모에 패딩 추가
                        double centerX = shape.startX;
                        double centerY = shape.startY;
                        double width = shape.endX - shape.startX;
                        double height = shape.endY - shape.startY;
                        double size = Math.min(width, height);

                        gc.strokeRect(centerX - padding, centerY - padding, size + 2 * padding, size + 2 * padding);
                        break;

                    case "⏹ Rectangle":
                        // 사각형 외곽선에 패딩 추가
                        gc.strokeRect(shape.startX - padding, shape.startY - padding,
                                (shape.endX - shape.startX) + 2 * padding,
                                (shape.endY - shape.startY) + 2 * padding);
                        break;
                }
            }
        }

        // 그룹 외곽선 그리기
        for (double[] bounds : groupBounds.values()) {
            double x = bounds[0] - 5; // 그룹 외곽선에 패딩 추가
            double y = bounds[1] - 5;
            double width = (bounds[2] - bounds[0]) + 10;
            double height = (bounds[3] - bounds[1]) + 10;
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

    private boolean isShapeFullyInsideBounds(ShapeRecord shape, double startX, double startY, double endX, double endY) {
        switch (shape.type) {
            case "➖ Line":
                return isPointInsideRectangle(shape.startX, shape.startY, startX, startY, endX, endY) &&
                        isPointInsideRectangle(shape.endX, shape.endY, startX, startY, endX, endY);

            case "⭕ Circle":
            case "⏹ Rectangle":
                return shape.startX >= startX && shape.endX <= endX &&
                        shape.startY >= startY && shape.endY <= endY;

            default:
                return false;
        }
    }

    // 점이 선 근처에 있는지 확인
    private boolean isPointNearLine(double x, double y, double x1, double y1, double x2, double y2) {
        final double TOLERANCE = 5.0;
        double distance = Math.abs((y2 - y1) * x - (x2 - x1) * y + x2 * y1 - y2 * x1) /
                Math.hypot(y2 - y1, x2 - x1);
        return distance <= TOLERANCE;
    }




// ------------------------------shape 기능-------------------------
    //Shape 컨트롤러
    @FXML
    private void showShapeMenu(MouseEvent event) {

        // Shape 모드로 전환
        activateShapeMode();

        if (event.getButton().equals(MouseButton.PRIMARY)) {
            shapeContextMenu.show(shapeButton, event.getScreenX(), event.getScreenY());
        }
    }



    private void startDrawing(MouseEvent event) {
        if (selectedShape == null) return;

        // 시작점을 사용자가 클릭한 위치로 설정하고 보정
        startX = Math.max(2, Math.min(event.getX(), drawingCanvas.getWidth() - 2));
        startY = Math.max(2, Math.min(event.getY(), drawingCanvas.getHeight() - 2));

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
    }

    private void drawShape(MouseEvent event) {
        if (selectedShape == null) return;

        // 끝점 보정: 캔버스 범위를 벗어나지 않도록 조정
        double endX = Math.max(2, Math.min(event.getX(), drawingCanvas.getWidth() - 2));
        double endY = Math.max(2, Math.min(event.getY(), drawingCanvas.getHeight() - 2));

        // 이전 도형 지우기
        gc.clearRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());
        redrawCanvas();  // 기존 도형 다시 그리기

        switch (selectedShape) {
            case "➖ Line":
                // 선은 보정 없이 원래대로 그리되, 끝점만 캔버스 내로 보정
                gc.strokeLine(startX, startY, endX, endY);
                break;
            case "⭕ Circle":
                double startXCorrected = Math.min(startX, endX);
                double startYCorrected = Math.min(startY, endY);
                double width = endX - startXCorrected;
                double height = endY - startYCorrected;
                double size = Math.min(width, height);
                gc.strokeOval(startXCorrected, startYCorrected, size, size);
                break;
            case "⏹ Rectangle":
                startXCorrected = Math.min(startX, endX);
                startYCorrected = Math.min(startY, endY);
                double rectWidth = endX - startXCorrected;
                double rectHeight = endY - startYCorrected;
                gc.strokeRect(startXCorrected, startYCorrected, rectWidth, rectHeight);
                break;
        }
    }


    private void finalizeShape(MouseEvent event) {
        if (selectedShape == null) return;

        double endX = Math.max(2, Math.min(event.getX(), drawingCanvas.getWidth() - 2));
        double endY = Math.max(2, Math.min(event.getY(), drawingCanvas.getHeight() - 2));

        gc.setStroke(currentColor);
        gc.setLineWidth(2);

        switch (selectedShape) {
            case "➖ Line":
                gc.strokeLine(startX, startY, endX, endY);
                shapes.add(new ShapeRecord(selectedShape, startX, startY, endX, endY, currentColor));
                break;
            case "⭕ Circle":
                double startXCorrected = Math.min(startX, endX);
                double startYCorrected = Math.min(startY, endY);
                double width = endX - startXCorrected;
                double height = endY - startYCorrected;
                double size = Math.min(width, height);
                gc.strokeOval(startXCorrected, startYCorrected, size, size);
                shapes.add(new ShapeRecord(selectedShape, startXCorrected, startYCorrected, startXCorrected + size, startYCorrected + size, currentColor));
                break;
            case "⏹ Rectangle":
                startXCorrected = Math.min(startX, endX);
                startYCorrected = Math.min(startY, endY);
                double rectWidth = endX - startXCorrected;
                double rectHeight = endY - startYCorrected;
                gc.strokeRect(startXCorrected, startYCorrected, rectWidth, rectHeight);
                shapes.add(new ShapeRecord(selectedShape, startXCorrected, startYCorrected, endX, endY, currentColor));
                break;
        }
    }

    private void redrawCanvas() {
        gc.clearRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());

        for (ShapeRecord shape : shapes) {
            gc.setStroke(shape.color != null ? shape.color : Color.BLACK);
            gc.setLineWidth(2);

            switch (shape.type) {
                case "➖ Line":
                    gc.strokeLine(shape.startX, shape.startY, shape.endX, shape.endY);
                    break;
                case "⭕ Circle":
                    double width = shape.endX - shape.startX;
                    double height = shape.endY - shape.startY;
                    double size = Math.min(width, height);
                    gc.strokeOval(shape.startX, shape.startY, size, size);
                    break;
                case "⏹ Rectangle":
                    gc.strokeRect(shape.startX, shape.startY, shape.endX - shape.startX, shape.endY - shape.startY);
                    break;
            }
        }

        highlightShapes(); // 선택된 도형 강조 유지
    }

    private static class ShapeRecord {
        String type;       // 도형의 타입
        double startX, startY, endX, endY;  // 도형의 좌표
        Color color;       // 도형의 색상
        int groupId = -1;  // 그룹 ID (-1: 그룹화되지 않음)

        // 기본 생성자 (Color 포함)
        ShapeRecord(String type, double startX, double startY, double endX, double endY, Color color) {
            this.type = type;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.color = color != null ? color : Color.BLACK; // null이면 기본 색상은 BLACK
        }

        // 복사 생성자
        ShapeRecord(ShapeRecord other) {
            this(other.type, other.startX, other.startY, other.endX, other.endY, other.color);
        }
    }


    //-------------------------group 구현---------------------
    private int nextGroupId = 1; // 그룹 ID 생성용 변수
    @FXML
    private void handleGroupButtonClick(MouseEvent event) {
        // 그룹 메뉴 표시
        ContextMenu groupMenu = new ContextMenu();
        MenuItem groupItem = new MenuItem("Grouping");
        groupItem.setOnAction(e -> groupSelectedShapes());
        MenuItem ungroupItem = new MenuItem("Ungrouping");
        ungroupItem.setOnAction(e -> ungroupSelectedShapes());
        groupMenu.getItems().addAll(groupItem, ungroupItem);
        groupMenu.show(groupButton, event.getScreenX(), event.getScreenY());
    }
    @FXML
    private void groupSelectedShapes() {
        if (selectedShapes.isEmpty()) {
            System.out.println("No shapes selected to group.");
            return;
        }
        // 새로운 그룹 ID 부여
        int groupId = nextGroupId++;
        for (ShapeRecord shape : selectedShapes) {
            shape.groupId = groupId;
        }
        System.out.println("Grouped " + selectedShapes.size() + " shapes into group " + groupId);
        redrawCanvas();
    }
    @FXML
    private void ungroupSelectedShapes() {
        if (selectedShapes.isEmpty()) {
            System.out.println("No shapes selected to ungroup.");
            return;
        }
        // 선택된 도형들의 그룹 해제
        for (ShapeRecord shape : selectedShapes) {
            shape.groupId = -1; // 그룹 해제
        }
        System.out.println("Ungrouped selected shapes.");
        redrawCanvas();
    }


//-------------------------------------move 기능------------------
    // move 컨트롤러
    @FXML
    private void handleMoveButtonClick(ActionEvent event) {
        resetMode(); // 기존 이벤트 핸들러 초기화
        currentMode = "Move";
        System.out.println("Move mode activated. Select and drag shapes to move.");

        // 마우스 이벤트 핸들러 설정
        drawingCanvas.setOnMousePressed(this::startMove);
        drawingCanvas.setOnMouseDragged(this::performMove);
        drawingCanvas.setOnMouseReleased(this::endMove);
    }

    private void endMove(MouseEvent event) {
        if (Math.abs(event.getX() - dragStartX) < 5 && Math.abs(event.getY() - dragStartY) < 5) {
            // 드래그가 아닌 클릭만 감지 → 빈 공간 클릭 시 선택 해제
            clearSelection();
        }
    }

    private void clearSelection() {
        selectedShapes.clear(); // 선택된 도형 리스트 비우기
        currentMode = null;     // 이동 모드 해제
        System.out.println("Selection cleared. Move mode exited.");
        redrawCanvas();         // 화면 다시 그리기 (점선 제거)
    }

    private void startMove(MouseEvent event) {
        // 클릭한 위치에 도형이 있는지 확인
        if (selectedShapes.isEmpty()) {
            for (int i = shapes.size() - 1; i >= 0; i--) {
                ShapeRecord shape = shapes.get(i);
                if (isPointInsideShape(event.getX(), event.getY(), shape)) {
                    selectedShapes.clear(); // 기존 선택 해제
                    selectedShapes.add(shape); // 단일 도형 선택
                    System.out.println("Single shape selected: " + shape.type);
                    break;
                }
            }
        } else {
            System.out.println("Multiple shapes selected for movement.");
        }

        // 드래그 시작점 저장
        dragStartX = event.getX();
        dragStartY = event.getY();

        redrawCanvas(); // 선택된 도형 강조
    }


    private void performMove(MouseEvent event) {
        if (selectedShapes.isEmpty()) return;

        // 드래그 이동량 계산
        double deltaX = event.getX() - dragStartX;
        double deltaY = event.getY() - dragStartY;

        // 선택된 모든 도형 이동
        for (ShapeRecord shape : selectedShapes) {
            shape.startX += deltaX;
            shape.startY += deltaY;
            shape.endX += deltaX;
            shape.endY += deltaY;
        }

        // 드래그 시작점 업데이트
        dragStartX = event.getX();
        dragStartY = event.getY();

        redrawCanvas(); // 화면 다시 그리기
    }

    @FXML
    private void handleCopyButtonClick(ActionEvent event) {
        resetMode();
        currentMode = "Copy";
        System.out.println("Copy mode activated. Click on a shape to copy it to the clipboard.");

        drawingCanvas.setOnMousePressed(event1 -> {
            for (int i = shapes.size() - 1; i >= 0; i--) {
                ShapeRecord shape = shapes.get(i);
                if (isPointInsideShape(event1.getX(), event1.getY(), shape)) {
                    clipboard.clear();
                    clipboard.add(new ShapeRecord(shape)); // 복사 생성자 사용
                    System.out.println("Shape copied to clipboard: " + shape.type);
                    break;
                }
            }
        });
    }


    @FXML
    private void handlePasteButtonClick(ActionEvent event) {
        resetMode();
        currentMode = "Paste";
        System.out.println("Paste mode activated. Click on the canvas to place the shape.");

        drawingCanvas.setOnMousePressed(event1 -> {
            if (clipboard.isEmpty()) {
                System.out.println("Clipboard is empty. Nothing to paste.");
                return;
            }

            ShapeRecord originalShape = clipboard.get(0); // 클립보드에 저장된 첫 번째 도형
            double offsetX = event1.getX();
            double offsetY = event1.getY();
            double width = originalShape.endX - originalShape.startX;
            double height = originalShape.endY - originalShape.startY;

            // 새 위치에 도형 생성
            ShapeRecord newShape = new ShapeRecord(
                    originalShape.type,
                    offsetX,
                    offsetY,
                    offsetX + width,
                    offsetY + height,
                    originalShape.color
            );

            shapes.add(newShape); // 도형 리스트에 추가
            System.out.println("Shape pasted at: (" + offsetX + ", " + offsetY + ")");
            redrawCanvas();
        });
    }



///--------------------------------------color 기능 ---------------------------------
    //Color controller
    private void handleColorChange() {
        Color selectedColor = colorPicker.getValue();

        if (!selectedShapes.isEmpty()) {
            // 선택된 모든 도형의 색상 변경
            for (ShapeRecord shape : selectedShapes) {
                shape.color = selectedColor;
            }
            redrawCanvas();
        } else {
            // 선택된 도형이 없으면 다음 그릴 도형의 색상 변경
            currentColor = selectedColor;
        }
    }


    @FXML
    private void showColorContextMenu(MouseEvent event) {
        if (event.getButton().equals(MouseButton.PRIMARY)) {
            colorContextMenu.show(colorButton, event.getScreenX(), event.getScreenY());
        }
    }

    // ----------------------------------------저장 기능---------------------------------------
    // Save (저장) 기능
    @FXML
    private void handleSave() {
        if (currentFile != null) {
            saveToFile(currentFile);
        } else {
            handleSaveAs();
        }
    }

    // Save As (다른 이름으로 저장) 기능
    @FXML
    private void handleSaveAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            saveToFile(file);
            currentFile = file;
        }
    }

    // 파일을 저장하는 메서드
    private void saveToFile(File file) {
        try (Writer writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(shapes, writer);
            System.out.println("File saved: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
        }
    }

    // Load (불러오기) 기능
    @FXML
    private void handleLoad() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            loadFromFile(file);
            currentFile = file;
        }
    }

    // 파일을 불러오는 메서드
    private void loadFromFile(File file) {
        try (Reader reader = new FileReader(file)) {
            Gson gson = new Gson();
            Type shapeListType = new TypeToken<List<ShapeRecord>>() {}.getType();
            shapes = gson.fromJson(reader, shapeListType);
            redrawCanvas();
            System.out.println("File loaded: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error loading file: " + e.getMessage());
        }
    }

    // Stage 설정 메서드 (Main Application에서 호출)
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }
}


