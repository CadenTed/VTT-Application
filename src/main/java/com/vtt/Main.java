package com.vtt;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    // Battlemap settings
    private static final int GRID_SIZE = 40; // Size of each grid square in pixels
    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 600;

    private Canvas canvas;
    private GraphicsContext gc;

    // Game state - this is where we store all our tokens!
    private List<Token> tokens = new ArrayList<>();
    private TokenType currentTokenType = TokenType.PLAYER;
    private Label statusLabel;


    // Token class to store token data
    static class Token {
        int gridX, gridY;
        TokenType type;

        Token(int gridX, int gridY, TokenType type) {
            this.gridX = gridX;
            this.gridY = gridY;
            this.type = type;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        // Create the main layout
        BorderPane root = new BorderPane();

        // Create status panel
        VBox statusPanel = createStatusPanel();
        root.setTop(statusPanel);

        // Create the canvas - this is your drawing surface
        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D(); // This is what you draw with

        // Set up mouse click handling
        canvas.setOnMouseClicked(this::onMouseClicked);
        canvas.setOnMouseMoved(this::onMouseMoved);

        // Add some example tokens to start
        tokens.add(new Token(2, 1, TokenType.PLAYER));
        tokens.add(new Token(5, 3, TokenType.MONSTER));
        tokens.add(new Token(8, 2, TokenType.NPC));

        // Initial draw
        drawBattlemap();

        // Add canvas to the layout
        root.setCenter(canvas);

        // Create and show the scene
        Scene scene = new Scene(root, CANVAS_WIDTH + 20, CANVAS_HEIGHT + 80);
        primaryStage.setTitle("VTT Battlemap - Click to Place Tokens!");
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("Battlemap ready! Left-click to place tokens, right-click to cycle token types!");
    }

    private VBox createStatusPanel() {
        VBox panel = new VBox(5);
        panel.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0;");

        Label instructions = new Label("Left-click: Place token | Right-click: Change token type | Shift+click: Remove token");
        statusLabel = new Label("Current token: " + currentTokenType.name());
        statusLabel.setStyle("-fx-font-weight: bold;");

        panel.getChildren().addAll(instructions, statusLabel);
        return panel;
    }

    private void drawBattlemap() {
        // Clear the canvas
        gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Set background color
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Draw the grid
        drawGrid();

        // Draw all tokens from our game state
        for (Token token : tokens) {
            drawToken(token);
        }
    }

    private void drawGrid() {
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);

        // Draw vertical lines
        for (int x = 0; x <= CANVAS_WIDTH; x += GRID_SIZE) {
            gc.strokeLine(x, 0, x, CANVAS_HEIGHT);
        }

        // Draw horizontal lines
        for (int y = 0; y <= CANVAS_HEIGHT; y += GRID_SIZE) {
            gc.strokeLine(0, y, CANVAS_WIDTH, y);
        }

        // Optional: Number the grid squares for reference
        gc.setFill(Color.DARKBLUE);
        gc.setFont(javafx.scene.text.Font.font(10));

        for (int x = 0; x < CANVAS_WIDTH / GRID_SIZE; x++) {
            for (int y = 0; y < CANVAS_HEIGHT / GRID_SIZE; y++) {
                String coords = x + "," + y;
                gc.fillText(coords, x * GRID_SIZE + 2, y * GRID_SIZE + 12);
            }
        }
    }

    private void drawToken(Token token) {
        // Convert grid coordinates to pixel coordinates (top-left of grid square)
        double pixelX = token.gridX * GRID_SIZE;
        double pixelY = token.gridY * GRID_SIZE;

        // Center the token in the grid square
        double centerX = pixelX + GRID_SIZE / 2.0;
        double centerY = pixelY + GRID_SIZE / 2.0;

        // Draw the token circle
        gc.setFill(token.type.getColor());
        gc.fillOval(centerX - 15, centerY - 15, 30, 30);

        // Add a border (darker version of the same color)
        gc.setStroke(token.type.getColor().darker());
        gc.setLineWidth(2);
        gc.strokeOval(centerX - 15, centerY - 15, 30, 30);

        // Add the token label
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font(12));
        gc.fillText(token.type.getLabel(), centerX - 4, centerY + 4);
    }

    private void onMouseMoved(MouseEvent event) {
        // Show preview of where token would be placed
        int gridX = (int) (event.getX() / GRID_SIZE);
        int gridY = (int) (event.getY() / GRID_SIZE);

        // Redraw everything
        drawBattlemap();

        // Draw preview token (semi-transparent)
        if (gridX >= 0 && gridX < CANVAS_WIDTH / GRID_SIZE &&
                gridY >= 0 && gridY < CANVAS_HEIGHT / GRID_SIZE &&
                !hasTokenAt(gridX, gridY)) {

            drawPreviewToken(gridX, gridY);
        }
    }

    private void drawPreviewToken(int gridX, int gridY) {
        double pixelX = gridX * GRID_SIZE;
        double pixelY = gridY * GRID_SIZE;
        double centerX = pixelX + GRID_SIZE / 2.0;
        double centerY = pixelY + GRID_SIZE / 2.0;

        // Draw semi-transparent preview
        Color previewColor = Color.color(
                currentTokenType.getColor().getRed(),
                currentTokenType.getColor().getGreen(),
                currentTokenType.getColor().getBlue(),
                0.5); // 50% transparency

        gc.setFill(previewColor);
        gc.fillOval(centerX - 15, centerY - 15, 30, 30);

        gc.setStroke(currentTokenType.getColor().darker());
        gc.setLineWidth(1);
        gc.strokeOval(centerX - 15, centerY - 15, 30, 30);
    }

    private boolean hasTokenAt(int gridX, int gridY) {
        return tokens.stream().anyMatch(token ->
                token.gridX == gridX && token.gridY == gridY);
    }

    private Token getTokenAt(int gridX, int gridY) {
        return tokens.stream()
                .filter(token -> token.gridX == gridX && token.gridY == gridY)
                .findFirst()
                .orElse(null);
    }

    private void onMouseClicked(MouseEvent event) {
        // Convert mouse coordinates to grid coordinates
        int gridX = (int) (event.getX() / GRID_SIZE);
        int gridY = (int) (event.getY() / GRID_SIZE);

        // Make sure we're within the grid bounds
        if (gridX < 0 || gridX >= CANVAS_WIDTH / GRID_SIZE ||
                gridY < 0 || gridY >= CANVAS_HEIGHT / GRID_SIZE) {
            return;
        }

        if (event.isShiftDown()) {
            // Shift + click: Remove token
            Token tokenToRemove = getTokenAt(gridX, gridY);
            if (tokenToRemove != null) {
                tokens.remove(tokenToRemove);
                System.out.printf("Removed %s token from (%d, %d)%n",
                        tokenToRemove.type.name(), gridX, gridY);
            }
        } else if (event.isSecondaryButtonDown()) {
            // Right-click: Cycle token type
            TokenType[] types = TokenType.values();
            int currentIndex = currentTokenType.ordinal();
            currentTokenType = types[(currentIndex + 1) % types.length];
            statusLabel.setText("Current token: " + currentTokenType.name());
            System.out.println("Switched to: " + currentTokenType.name());
        } else {
            // Left-click: Place token
            if (!hasTokenAt(gridX, gridY)) {
                Token newToken = new Token(gridX, gridY, currentTokenType);
                tokens.add(newToken);
                System.out.printf("Placed %s token at (%d, %d)%n",
                        currentTokenType.name(), gridX, gridY);
            } else {
                System.out.printf("Square (%d, %d) already occupied!%n", gridX, gridY);
            }
        }

        // Redraw the battlemap
        drawBattlemap();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

/*
 * LEARNING NOTES:
 *
 * 1. Game State Management:
 *    - List<Token> stores all tokens on the battlemap
 *    - Each Token has gridX, gridY coordinates and a type
 *    - Separation of data (tokens list) and presentation (drawing)
 *
 * 2. Mouse Event Handling:
 *    - Left-click: Place new token
 *    - Right-click: Cycle through token types
 *    - Shift+click: Remove token
 *    - Mouse move: Show preview of where token would be placed
 *
 * 3. Token System:
 *    - Enum for token types (PLAYER, MONSTER, NPC, OBJECT)
 *    - Each type has color and label
 *    - Easy to add new token types
 *
 * 4. Visual Feedback:
 *    - Preview tokens (semi-transparent) on mouse hover
 *    - Status panel shows current token type
 *    - Console output for debugging
 *
 * 5. Collision Detection:
 *    - hasTokenAt() prevents placing tokens on occupied squares
 *    - getTokenAt() finds existing tokens for removal
 *
 * 6. JavaFX UI Components:
 *    - BorderPane for layout
 *    - VBox for status panel
 *    - Labels for instructions and current state
 *
 * FEATURES IMPLEMENTED:
 * ✅ Grid-based battlemap
 * ✅ Click to place tokens
 * ✅ Multiple token types
 * ✅ Token removal
 * ✅ Preview system
 * ✅ Collision detection
 *
 * NEXT FEATURES TO ADD:
 * - Drag and drop token movement
 * - Save/load battlemap state
 * - Import custom token images
 * - Zoom and pan functionality
 * - MCP integration for PDF rulebooks
 * - Initiative tracker
 * - Measurements and rulers
 */