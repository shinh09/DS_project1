package DS.teamproject.DrawingEditor.Controller;

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

import javafx.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

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
    private List<ShapeRecord> selectedShapes = new ArrayList<>();
    private List<ShapeRecord> clipboard = new ArrayList<>();



    @FXML
    public void initialize() {
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

    private double dragStartX, dragStartY;

    private void handleCanvasClick(MouseEvent event) {
        if (!"Select".equals(currentMode)) return;

        double clickX = event.getX();
        double clickY = event.getY();

        selectedShapes.clear();

        // 클릭한 좌표를 기준으로 도형 감지
        for (int i = shapes.size() - 1; i >= 0; i--) {
            ShapeRecord shape = shapes.get(i);

            if (isPointInsideShape(clickX, clickY, shape)) {
                selectedShapes.add(shape);
                break; // 가장 위의 도형 하나만 선택
            }
        }

        redrawCanvas();
        highlightShapes();
    }

    private boolean isPointInsideShape(double x, double y, ShapeRecord shape) {
        final double TOLERANCE = 5.0; // 클릭 허용 오차

        switch (shape.type) {
            case "➖ Line":
                // 선 근처에 있는지 검사
                return isPointNearLine(x, y, shape.startX, shape.startY, shape.endX, shape.endY, TOLERANCE);

            case "⭕ Circle":
                // 원 내부에 있는지 검사
                double centerX = (shape.startX + shape.endX) / 2;
                double centerY = (shape.startY + shape.endY) / 2;
                double radius = Math.abs(shape.endX - shape.startX) / 2;
                return Math.hypot(x - centerX, y - centerY) <= radius;

            case "⏹ Rectangle":
                // 사각형 내부에 있는지 검사
                return x >= shape.startX && x <= shape.endX &&
                        y >= shape.startY && y <= shape.endY;

            default:
                return false;
        }
    }

    private boolean isPointNearLine(double px, double py, double x1, double y1, double x2, double y2, double tolerance) {
        double numerator = Math.abs((y2 - y1) * px - (x2 - x1) * py + x2 * y1 - y2 * x1);
        double denominator = Math.hypot(y2 - y1, x2 - x1);
        double distance = numerator / denominator;

        return distance <= tolerance;
    }

    // Select 모드: 드래그로 다중 선택
    private void startSelect(MouseEvent event) {
        if (!"Select".equals(currentMode)) return;

        // 시작점을 도형 두께를 고려해 보정
        dragStartX = Math.max(2, Math.min(event.getX(), drawingCanvas.getWidth() - 2));
        dragStartY = Math.max(2, Math.min(event.getY(), drawingCanvas.getHeight() - 2));

        selectedShapes.clear();
    }

    private double finalStartX, finalStartY, finalEndX, finalEndY;

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
        gc.setStroke(Color.web("#33FF04"));
        gc.setLineWidth(2);
        gc.setLineDashes(10);

        for (ShapeRecord shape : selectedShapes) {
            switch (shape.type) {
                case "➖ Line":
                    gc.strokeLine(shape.startX, shape.startY, shape.endX, shape.endY);
                    break;

                case "⭕ Circle":
                case "⏹ Rectangle":
                    gc.strokeRect(shape.startX, shape.startY, shape.endX - shape.startX, shape.endY - shape.startY);
                    break;
            }
        }

        gc.setLineDashes(null); // 점선 해제
        gc.setStroke(Color.BLACK); // 기본 색상으로 되돌림
    }

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

        // 끝점 보정: 캔버스 범위를 벗어나지 않도록 조정
        double endX = Math.max(2, Math.min(event.getX(), drawingCanvas.getWidth() - 2));
        double endY = Math.max(2, Math.min(event.getY(), drawingCanvas.getHeight() - 2));

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);

        switch (selectedShape) {
            case "➖ Line":
                // 선은 보정 없이 원래대로 그리되, 끝점만 캔버스 내로 보정
                gc.strokeLine(startX, startY, endX, endY);
                shapes.add(new ShapeRecord(selectedShape, startX, startY, endX, endY));
                break;
            case "⭕ Circle":
                double startXCorrected = Math.min(startX, endX);
                double startYCorrected = Math.min(startY, endY);
                double width = endX - startXCorrected;
                double height = endY - startYCorrected;
                double size = Math.min(width, height);
                gc.strokeOval(startXCorrected, startYCorrected, size, size);
                shapes.add(new ShapeRecord(selectedShape, startXCorrected, startYCorrected, startXCorrected + size, startYCorrected + size));
                break;
            case "⏹ Rectangle":
                startXCorrected = Math.min(startX, endX);
                startYCorrected = Math.min(startY, endY);
                double rectWidth = endX - startXCorrected;
                double rectHeight = endY - startYCorrected;
                gc.strokeRect(startXCorrected, startYCorrected, rectWidth, rectHeight);
                shapes.add(new ShapeRecord(selectedShape, startXCorrected, startYCorrected, endX, endY));
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
        String type;       // 도형의 타입 ("Line", "Circle", "Rectangle" 등)
        double startX;     // 시작 X 좌표
        double startY;     // 시작 Y 좌표
        double endX;       // 끝 X 좌표
        double endY;       // 끝 Y 좌표
        Color color;       // 도형의 색상

        // 기본 생성자 (색상을 전달하지 않는 경우)
        ShapeRecord(String type, double startX, double startY, double endX, double endY) {
            this.type = type;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.color = Color.BLACK; // 기본 색상은 검정색
        }

        // 색상을 포함하는 생성자
        ShapeRecord(String type, double startX, double startY, double endX, double endY, Color color) {
            this.type = type;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.color = (color != null) ? color : Color.BLACK; // null인 경우 기본 색상으로 설정
        }

        // 복사 생성자
        ShapeRecord(ShapeRecord other) {
            this(other.type, other.startX, other.startY, other.endX, other.endY, other.color);
        }
    }

    // color 컨트롤러
    // 선택된 도형의 색상 적용
    @FXML
    private void showColorContextMenu(MouseEvent event) {
        if (event.getButton().equals(MouseButton.PRIMARY)) {
            colorContextMenu.show(colorButton, event.getScreenX(), event.getScreenY());
        }
    }

    // move 컨트롤러
    @FXML
    private void handleMoveButtonClick(ActionEvent event) {
        if (selectedShapes.isEmpty()) {
            System.out.println("No shapes selected to move.");
            return;
        }

        resetMode();
        currentMode = "Move";
        System.out.println("Move mode activated. Drag shapes to move.");

        drawingCanvas.setOnMousePressed(this::startMove);
        drawingCanvas.setOnMouseDragged(this::performMove);
        drawingCanvas.setOnMouseReleased(this::endMoveIfClickedOutside);
    }

    private void endMoveIfClickedOutside(MouseEvent event) {
        // 빈 공간 클릭시 해제
        if (!isPointInsideSelectedShapes(event.getX(), event.getY())) {
            clearSelection();
        }
    }

    private boolean isPointInsideSelectedShapes(double x, double y) {
        for (ShapeRecord shape : selectedShapes) {
            if (x >= shape.startX && x <= shape.endX && y >= shape.startY && y <= shape.endY) {
                return true; // 선택된 도형 내부에 있음
            }
        }
        return false; // 선택된 도형 외부
    }

    private void clearSelection() {
        selectedShapes.clear(); // 선택된 도형 리스트 비우기
        currentMode = null;     // 이동 모드 해제
        redrawCanvas();         // 화면 다시 그리기 (점선 제거)
    }

    private void startMove(MouseEvent event) {
        if (selectedShapes.isEmpty()) return;

        // 드래그 시작점 저장
        dragStartX = event.getX();
        dragStartY = event.getY();

    }

    private void performMove(MouseEvent event) {
        if (selectedShapes.isEmpty()) return;

        // 드래그 이동량 계산
        double deltaX = event.getX() - dragStartX;
        double deltaY = event.getY() - dragStartY;

        // 캔버스 경계 확인
        for (ShapeRecord shape : selectedShapes) {
            double newStartX = shape.startX + deltaX;
            double newStartY = shape.startY + deltaY;
            double newEndX = shape.endX + deltaX;
            double newEndY = shape.endY + deltaY;

            // 캔버스를 벗어나면 이동 중단
            if (newStartX < 0 || newStartY < 0 || newEndX > drawingCanvas.getWidth() || newEndY > drawingCanvas.getHeight()) {
                return;
            }
        }

        // 경계 확인이 통과되면 모든 도형 이동
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
        if (selectedShapes.isEmpty()) {
            System.out.println("No shapes selected to copy.");
            return;
        }

        clipboard.clear();

        // 기준 좌표 (선택된 도형들의 최소 x, y 좌표)
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;

        for (ShapeRecord shape : selectedShapes) {
            minX = Math.min(minX, shape.startX);
            minY = Math.min(minY, shape.startY);
        }

        // 도형들의 상대 좌표를 계산하여 클립보드에 저장
        for (ShapeRecord shape : selectedShapes) {
            double relativeStartX = shape.startX - minX;
            double relativeStartY = shape.startY - minY;
            double relativeEndX = shape.endX - minX;
            double relativeEndY = shape.endY - minY;

            clipboard.add(new ShapeRecord(shape.type, relativeStartX, relativeStartY, relativeEndX, relativeEndY, shape.color));
        }

        System.out.println(clipboard.size() + " shape(s) copied to clipboard with relative positions.");
    }


    @FXML
    private void handlePasteButtonClick(ActionEvent event) {
        if (clipboard.isEmpty()) {
            System.out.println("Clipboard is empty. Nothing to paste.");
            return;
        }

        resetMode();
        currentMode = "Paste";
        System.out.println("Paste mode activated. Click on the canvas to place shapes.");

        drawingCanvas.setOnMousePressed(this::performPaste);
    }

    private void performPaste(MouseEvent event) {
        double pasteStartX = event.getX();
        double pasteStartY = event.getY();

        List<ShapeRecord> newShapes = new ArrayList<>();

        // 클립보드의 도형들을 클릭한 위치에 상대적으로 붙여넣기
        for (ShapeRecord shape : clipboard) {
            double newStartX = pasteStartX + shape.startX;
            double newStartY = pasteStartY + shape.startY;
            double newEndX = pasteStartX + shape.endX;
            double newEndY = pasteStartY + shape.endY;

            // 캔버스 경계를 벗어나지 않도록 보정
            if (newStartX < 0) {
                double diff = -newStartX;
                newStartX += diff;
                newEndX += diff;
            }
            if (newStartY < 0) {
                double diff = -newStartY;
                newStartY += diff;
                newEndY += diff;
            }
            if (newEndX > drawingCanvas.getWidth()) {
                double diff = newEndX - drawingCanvas.getWidth();
                newStartX -= diff;
                newEndX -= diff;
            }
            if (newEndY > drawingCanvas.getHeight()) {
                double diff = newEndY - drawingCanvas.getHeight();
                newStartY -= diff;
                newEndY -= diff;
            }

            // 새로운 도형 생성 및 추가
            newShapes.add(new ShapeRecord(shape.type, newStartX, newStartY, newEndX, newEndY, shape.color));
        }

        shapes.addAll(newShapes);
        redrawCanvas();
        System.out.println(newShapes.size() + " shape(s) pasted at (" + pasteStartX + ", " + pasteStartY + ").");
    }
}


