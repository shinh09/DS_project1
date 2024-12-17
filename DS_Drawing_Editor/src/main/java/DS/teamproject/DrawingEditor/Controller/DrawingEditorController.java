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

        double dragEndX = Math.max(2, Math.min(event.getX(), drawingCanvas.getWidth() - 2));
        double dragEndY = Math.max(2, Math.min(event.getY(), drawingCanvas.getHeight() - 2));

        // 드래그 범위 계산 및 저장
        finalStartX = Math.min(dragStartX, dragEndX);
        finalStartY = Math.min(dragStartY, dragEndY);
        finalEndX = Math.max(dragStartX, dragEndX);
        finalEndY = Math.max(dragStartY, dragEndY);

        // 드래그 범위를 초록색 점선으로 표시
        redrawCanvas();
        gc.setStroke(Color.web("#33FF04"));
        gc.setLineWidth(2);
        gc.setLineDashes(10);
        gc.strokeRect(finalStartX, finalStartY, finalEndX - finalStartX, finalEndY - finalStartY);
        gc.setLineDashes(null);

        // 범위 내에 포함된 도형 선택
        selectedShapes.clear();
        for (ShapeRecord shape : shapes) {
            if (isShapeInsideBounds(shape, finalStartX, finalStartY, finalEndX, finalEndY)) {
                selectedShapes.add(shape);
            }
        }

        highlightShapes(); // 드래그 중에도 선택된 도형 강조
    }

    private void handleCanvasRelease(MouseEvent event) {
        if ("Select".equals(currentMode)) {
            // 드래그 중에 선택된 도형을 유지하고 화면을 다시 그립니다.
            redrawCanvas();
            highlightShapes(); // 선택된 도형에 초록색 점선을 유지합니다.
        }
    }

    private void highlightShapes() {
        gc.setStroke(Color.web("#33FF04"));
        gc.setLineWidth(2);
        gc.setLineDashes(10); // 점선 패턴 설정 (10 픽셀 간격)

        final double padding = 5; // 도형 테두리를 살짝 크게 만들기 위한 패딩 값

        for (ShapeRecord shape : selectedShapes) {
            switch (shape.type) {
                case "➖ Line":
                    double dx = shape.endX - shape.startX;
                    double dy = shape.endY - shape.startY;
                    double length = Math.hypot(dx, dy);
                    double padX = padding * (dx / length);
                    double padY = padding * (dy / length);

                    gc.strokeLine(shape.startX - padX, shape.startY - padY, shape.endX + padX, shape.endY + padY);
                    break;

                case "⭕ Circle":
                    double width = shape.endX - shape.startX;
                    double height = shape.endY - shape.startY;
                    double size = Math.min(width, height);
                    gc.strokeRect(shape.startX, shape.startY, size, size);
                    break;

                case "⏹ Rectangle":
                    gc.strokeRect(shape.startX - padding, shape.startY - padding,
                            shape.endX - shape.startX + 2 * padding,
                            shape.endY - shape.startY + 2 * padding);
                    break;
            }
        }

        gc.setLineDashes(null); // 점선 패턴 해제
        gc.setStroke(Color.BLACK); // 기본 색상으로 되돌림
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
        final double PADDING = 2.0; // 선택 범위를 조금만 넓히기 위한 패딩 값

        switch (shape.type) {
            case "➖ Line":
                // 선의 시작점과 끝점이 드래그 범위 안에 있는지 확인 (패딩 적용)
                return isPointInBounds(shape.startX, shape.startY, startX - PADDING, startY - PADDING, endX + PADDING, endY + PADDING) &&
                        isPointInBounds(shape.endX, shape.endY, startX - PADDING, startY - PADDING, endX + PADDING, endY + PADDING);

            case "⭕ Circle":
            case "⏹ Rectangle":
                // 사각형 또는 원의 경계 상자가 드래그 범위 안에 있는지 확인 (패딩 적용)
                return shape.startX >= startX - PADDING && shape.endX <= endX + PADDING &&
                        shape.startY >= startY - PADDING && shape.endY <= endY + PADDING;

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
        String type;
        double startX, startY, endX, endY;
        Color color;

        // 기본 생성자 (Color 포함)
        ShapeRecord(String type, double startX, double startY, double endX, double endY, Color color) {
            this.type = type;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.color = color != null ? color : Color.BLACK; // null이면 기본 색상은 BLACK
        }

        // Color를 제외한 기존 생성자 (기본 색상 BLACK 적용)
        ShapeRecord(String type, double startX, double startY, double endX, double endY) {
            this(type, startX, startY, endX, endY, Color.BLACK);
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

}



