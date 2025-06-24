package com.vtt;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    // Battlemap settings
    private static final int GRID_SIZE = 40;
    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 600;
    private static final int SIDEBAR_WIDTH = 150;

    private Canvas canvas;
    private GraphicsContext gc;

    // Game state
    private List<Token> tokens = new ArrayList<>();
    private TokenType currentTokenType = TokenType.PLAYER;
    private Label statusLabel;

    // Drag and drop state
    private Token draggedToken = null;
    private double dragStartX, dragStartY;
    private double dragOffsetX, dragOffsetY;
    private boolean isDragging = false;

    // UI Components
    private VBox tokenSelectionPanel;
    private List<Button> tokenTypeButtons = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // Create status panel (top)
        VBox statusPanel = createStatusPanel();
        root.setTop(statusPanel);

        // Create canvas (center)
        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        // Set up mouse event handling
        setupMouseHandlers();

        // Create token selection sidebar (right)
        tokenSelectionPanel = createTokenSelectionPanel();
        root.setRight(tokenSelectionPanel);

        // Add some example tokens to start
        tokens.add(new Token(2, 1, TokenType.PLAYER));
        tokens.add(new Token(5, 3, TokenType.MONSTER));
        tokens.add(new Token(8, 2, TokenType.NPC));

        // Initial draw
        drawBattlemap();

        root.setCenter(canvas);

        // Create and show the scene
        Scene scene = new Scene(root, CANVAS_WIDTH + SIDEBAR_WIDTH + 40, CANVAS_HEIGHT + 80);
        primaryStage.setTitle("VTT Battlemap - Drag & Drop + Selection Bar");
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("Enhanced Battlemap ready!");
        System.out.println("- Left-click: Place token");
        System.out.println("- Drag tokens to move them");
        System.out.println("- Shift+click: Remove token");
        System.out.println("- Use sidebar to select token type");
    }

    private VBox createStatusPanel() {
        VBox panel = new VBox(5);
        panel.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0;");

        Label instructions = new Label("Left-click: Place token | Drag: Move token | Shift+click: Remove token");
        statusLabel = new Label("Current token: " + currentTokenType.getLabel());
        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        panel.getChildren().addAll(instructions, statusLabel);
        return panel;
    }

    private VBox createTokenSelectionPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #e8e8e8; -fx-border-color: #cccccc; -fx-border-width: 0 0 0 1;");
        panel.setPrefWidth(SIDEBAR_WIDTH);

        // Title
        Label title = new Label("Token Types");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        panel.getChildren().add(title);

        panel.getChildren().add(new Separator());

        // Create buttons for each token type
        for (TokenType tokenType : TokenType.values()) {
            Button tokenButton = createTokenTypeButton(tokenType);
            tokenTypeButtons.add(tokenButton);
            panel.getChildren().add(tokenButton);
        }

        // Set initial selection
        updateButtonSelection();

        return panel;
    }

    private Button createTokenTypeButton(TokenType tokenType) {
        Button button = new Button();
        button.setPrefWidth(SIDEBAR_WIDTH - 30);
        button.setPrefHeight(50);

        // Create visual representation
        Circle tokenPreview = new Circle(12);
        tokenPreview.setFill(tokenType.getColor());
        tokenPreview.setStroke(tokenType.getColor().darker());
        tokenPreview.setStrokeWidth(2);

        Label typeLabel = new Label(tokenType.name());
        typeLabel.setStyle("-fx-font-weight: bold;");

        VBox buttonContent = new VBox(5);
        buttonContent.getChildren().addAll(tokenPreview, typeLabel);
        buttonContent.setStyle("-fx-alignment: center;");

        button.setGraphic(buttonContent);

        // Handle button click
        button.setOnAction(e -> {
            currentTokenType = tokenType;
            statusLabel.setText("Current token: " + currentTokenType.getLabel());
            updateButtonSelection();
            System.out.println("Selected token type: " + tokenType.name());
        });

        return button;
    }

    private void updateButtonSelection() {
        for (int i = 0; i < tokenTypeButtons.size(); i++) {
            Button button = tokenTypeButtons.get(i);
            TokenType buttonType = TokenType.values()[i];

            if (buttonType == currentTokenType) {
                button.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-border-color: #45a049; -fx-border-width: 2;");
            } else {
                button.setStyle("-fx-background-color: #f9f9f9; -fx-text-fill: black; -fx-border-color: #ddd; -fx-border-width: 1;");
            }
        }
    }

    private void setupMouseHandlers() {
        canvas.setOnMousePressed(this::onMousePressed);
        canvas.setOnMouseDragged(this::onMouseDragged);
        canvas.setOnMouseReleased(this::onMouseReleased);
        canvas.setOnMouseMoved(this::onMouseMoved);
    }

    private void onMousePressed(MouseEvent event) {
        int gridX = (int) (event.getX() / GRID_SIZE);
        int gridY = (int) (event.getY() / GRID_SIZE);

        // Check if we're clicking on an existing token
        Token clickedToken = getTokenAt(gridX, gridY);

        if (event.isShiftDown() && clickedToken != null) {
            // Shift + click: Remove token
            tokens.remove(clickedToken);
            System.out.printf("Removed %s token from (%d, %d)%n",
                    clickedToken.getType().name(), gridX, gridY);
            drawBattlemap();
        } else if (clickedToken != null && event.getButton() == MouseButton.PRIMARY) {
            // Start dragging existing token
            draggedToken = clickedToken;
            dragStartX = event.getX();
            dragStartY = event.getY();

            // Calculate offset from token center
            double tokenCenterX = (clickedToken.getGridX() * GRID_SIZE) + GRID_SIZE / 2.0;
            double tokenCenterY = (clickedToken.getGridY() * GRID_SIZE) + GRID_SIZE / 2.0;
            dragOffsetX = event.getX() - tokenCenterX;
            dragOffsetY = event.getY() - tokenCenterY;

            isDragging = true;
            System.out.printf("Started dragging %s token from (%d, %d)%n",
                    clickedToken.getType().name(), gridX, gridY);
        }
    }

    private void onMouseDragged(MouseEvent event) {
        if (isDragging && draggedToken != null) {
            // Redraw battlemap with dragged token
            drawBattlemap();
            drawDraggedToken(event.getX(), event.getY());
        }
    }

    private void onMouseReleased(MouseEvent event) {
        if (isDragging && draggedToken != null) {
            int newGridX = (int) (event.getX() / GRID_SIZE);
            int newGridY = (int) (event.getY() / GRID_SIZE);

            // Check if drop location is valid
            if (newGridX >= 0 && newGridX < CANVAS_WIDTH / GRID_SIZE &&
                    newGridY >= 0 && newGridY < CANVAS_HEIGHT / GRID_SIZE &&
                    !hasTokenAt(newGridX, newGridY)) {

                // Move token to new position
                int oldX = draggedToken.getGridX();
                int oldY = draggedToken.getGridY();
                draggedToken.setGridX(newGridX);
                draggedToken.setGridY(newGridY);

                System.out.printf("Moved %s token from (%d, %d) to (%d, %d)%n",
                        draggedToken.getType().name(), oldX, oldY, newGridX, newGridY);
            } else {
                System.out.println("Invalid drop location - token returned to original position");
            }

            // Reset drag state
            draggedToken = null;
            isDragging = false;
            drawBattlemap();
        } else if (!isDragging) {
            // Regular click - place new token
            int gridX = (int) (event.getX() / GRID_SIZE);
            int gridY = (int) (event.getY() / GRID_SIZE);

            if (gridX >= 0 && gridX < CANVAS_WIDTH / GRID_SIZE &&
                    gridY >= 0 && gridY < CANVAS_HEIGHT / GRID_SIZE &&
                    !hasTokenAt(gridX, gridY)) {

                Token newToken = new Token(gridX, gridY, currentTokenType);
                tokens.add(newToken);
                System.out.printf("Placed %s token at (%d, %d)%n",
                        currentTokenType.name(), gridX, gridY);
                drawBattlemap();
            }
        }
    }

    private void onMouseMoved(MouseEvent event) {
        if (!isDragging) {
            int gridX = (int) (event.getX() / GRID_SIZE);
            int gridY = (int) (event.getY() / GRID_SIZE);

            drawBattlemap();

            // Show preview only if no token exists at this location
            if (gridX >= 0 && gridX < CANVAS_WIDTH / GRID_SIZE &&
                    gridY >= 0 && gridY < CANVAS_HEIGHT / GRID_SIZE &&
                    !hasTokenAt(gridX, gridY)) {

                drawPreviewToken(gridX, gridY);
            }
        }
    }

    private void drawDraggedToken(double mouseX, double mouseY) {
        // Draw the token being dragged at mouse position
        double centerX = mouseX - dragOffsetX;
        double centerY = mouseY - dragOffsetY;

        // Draw semi-transparent dragged token
        Color dragColor = Color.color(
                draggedToken.getType().getColor().getRed(),
                draggedToken.getType().getColor().getGreen(),
                draggedToken.getType().getColor().getBlue(),
                0.8);

        gc.setFill(dragColor);
        gc.fillOval(centerX - 15, centerY - 15, 30, 30);

        gc.setStroke(draggedToken.getType().getColor().darker());
        gc.setLineWidth(2);
        gc.strokeOval(centerX - 15, centerY - 15, 30, 30);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(12));
        gc.fillText(draggedToken.getType().getLabel(), centerX - 4, centerY + 4);

        // Draw drop preview
        int targetGridX = (int) (mouseX / GRID_SIZE);
        int targetGridY = (int) (mouseY / GRID_SIZE);

        if (targetGridX >= 0 && targetGridX < CANVAS_WIDTH / GRID_SIZE &&
                targetGridY >= 0 && targetGridY < CANVAS_HEIGHT / GRID_SIZE) {

            // Highlight target square
            double squareX = targetGridX * GRID_SIZE;
            double squareY = targetGridY * GRID_SIZE;

            if (!hasTokenAt(targetGridX, targetGridY)) {
                gc.setStroke(Color.LIMEGREEN);
                gc.setLineWidth(3);
                gc.strokeRect(squareX, squareY, GRID_SIZE, GRID_SIZE);
            } else {
                gc.setStroke(Color.RED);
                gc.setLineWidth(3);
                gc.strokeRect(squareX, squareY, GRID_SIZE, GRID_SIZE);
            }
        }
    }

    private void drawBattlemap() {
        gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        drawGrid();

        // Draw all tokens except the one being dragged
        for (Token token : tokens) {
            if (token != draggedToken) {
                drawToken(token);
            }
        }
    }

    private void drawGrid() {
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);

        for (int x = 0; x <= CANVAS_WIDTH; x += GRID_SIZE) {
            gc.strokeLine(x, 0, x, CANVAS_HEIGHT);
        }

        for (int y = 0; y <= CANVAS_HEIGHT; y += GRID_SIZE) {
            gc.strokeLine(0, y, CANVAS_WIDTH, y);
        }

        gc.setFill(Color.DARKBLUE);
        gc.setFont(Font.font(10));

        for (int x = 0; x < CANVAS_WIDTH / GRID_SIZE; x++) {
            for (int y = 0; y < CANVAS_HEIGHT / GRID_SIZE; y++) {
                String coords = x + "," + y;
                gc.fillText(coords, x * GRID_SIZE + 2, y * GRID_SIZE + 12);
            }
        }
    }

    private void drawToken(Token token) {
        double pixelX = token.getGridX() * GRID_SIZE;
        double pixelY = token.getGridY() * GRID_SIZE;
        double centerX = pixelX + GRID_SIZE / 2.0;
        double centerY = pixelY + GRID_SIZE / 2.0;

        gc.setFill(token.getType().getColor());
        gc.fillOval(centerX - 15, centerY - 15, 30, 30);

        gc.setStroke(token.getType().getColor().darker());
        gc.setLineWidth(2);
        gc.strokeOval(centerX - 15, centerY - 15, 30, 30);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(12));
        gc.fillText(token.getType().getLabel(), centerX - 4, centerY + 4);
    }

    private void drawPreviewToken(int gridX, int gridY) {
        double pixelX = gridX * GRID_SIZE;
        double pixelY = gridY * GRID_SIZE;
        double centerX = pixelX + GRID_SIZE / 2.0;
        double centerY = pixelY + GRID_SIZE / 2.0;

        Color previewColor = Color.color(
                currentTokenType.getColor().getRed(),
                currentTokenType.getColor().getGreen(),
                currentTokenType.getColor().getBlue(),
                0.5);

        gc.setFill(previewColor);
        gc.fillOval(centerX - 15, centerY - 15, 30, 30);

        gc.setStroke(currentTokenType.getColor().darker());
        gc.setLineWidth(1);
        gc.strokeOval(centerX - 15, centerY - 15, 30, 30);
    }

    private boolean hasTokenAt(int gridX, int gridY) {
        for (Token token : tokens) {
            if (token.getGridX() == gridX && token.getGridY() == gridY) {
                return true;
            }
        }
        return false;
    }

    private Token getTokenAt(int gridX, int gridY) {
        for (Token token : tokens) {
            if (token.getGridX() == gridX && token.getGridY() == gridY) {
                return token;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

/*
 * NEW FEATURES IMPLEMENTED:
 *
 * 1. DRAG & DROP SYSTEM:
 *    - Click and drag existing tokens to move them
 *    - Visual feedback during dragging (semi-transparent token follows mouse)
 *    - Green highlight for valid drop locations
 *    - Red highlight for invalid drop locations
 *    - Tokens snap back if dropped on invalid location
 *
 * 2. TOKEN SELECTION SIDEBAR:
 *    - Visual buttons for each token type
 *    - Shows token color and name
 *    - Selected button is highlighted in green
 *    - Click to change current token type
 *    - Much more intuitive than right-clicking
 *
 * 3. ENHANCED UI:
 *    - Professional layout with proper spacing
 *    - Clear visual hierarchy
 *    - Better status information
 *    - Larger window to accommodate sidebar
 *
 * 4. IMPROVED MOUSE HANDLING:
 *    - Separate handlers for press, drag, release, and move
 *    - Smart detection of drag vs click
 *    - Proper drag offset calculation
 *    - Collision detection during drag operations
 *
 * TECHNICAL CONCEPTS LEARNED:
 * - State management for drag operations
 * - Complex mouse event handling
 * - UI component creation and styling
 * - Event coordination between different UI elements
 * - Visual feedback systems
 *
 * NEXT POSSIBLE FEATURES:
 * - Save/load battlemap state
 * - Custom token images
 * - Zoom and pan functionality
 * - MCP integration for PDF rulebooks
 * - Undo/redo system
 * - Multi-select and group operations
 */