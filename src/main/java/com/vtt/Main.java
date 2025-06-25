package com.vtt;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.Loader;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

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
    private boolean isShifting = false;

    // PDF Viewer Components
    private VBox pdfPanel;
    private ScrollPane pdfScrollPane;
    private ImageView pdfImageView;
    private Label pdfStatusLabel;
    private Button prevPageButton, nextPageButton;
    private Label pageInfoLabel;
    private TextField searchField;
    private Button searchButton;

    // PDF State

    private PDDocument currentPdfDocument;
    private PDFRenderer pdfRenderer;
    private int currentPage = 0;
    private int totalPages = 0;
    private String currentPdfName = "";

    // UI Components
    private VBox tokenSelectionPanel;
    private List<Button> tokenTypeButtons = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        pdfPanel = createPdfPanel();
        root.setLeft(pdfPanel);

        // Create canvas (center)
        VBox centerPanel = new VBox();

        // Create status panel (top)
        VBox statusPanel = createStatusPanel();
        centerPanel.getChildren().add(statusPanel);

        // Canvas
        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        setupMouseHandlers();
        centerPanel.getChildren().add(canvas);

        root.setCenter(centerPanel);

        // Token selection sidebar
        tokenSelectionPanel = createTokenSelectionPanel();
        root.setRight(tokenSelectionPanel);
        
        tokens.add(new Token(2, 1, TokenType.PLAYER));
        tokens.add(new Token(5, 3, TokenType.MONSTER));
        tokens.add(new Token(8, 2, TokenType.NPC));

        drawBattlemap();

        Scene scene = new Scene(root, 1350, 850);
        primaryStage.setTitle("VTT");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createPdfPanel() {
        VBox pdfPanel = new VBox(10);
        pdfPanel.setPadding(new Insets(10));
        pdfPanel.setPrefWidth(350);
        pdfPanel.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #cccccc; -fx-border-width: 0 1 0 0;");

        Label title = new Label("PDF Viewer");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));

        Button loadPdfButton = new Button("Load PDF");
        loadPdfButton.setOnAction(e -> loadPdfFile());

        pdfStatusLabel = new Label("No PDF Loaded");
        pdfStatusLabel.setStyle("-fx-text-fill: #666666");

        HBox searchBox = new HBox(5);
        searchField = new TextField();
        searchField.setPromptText("Search PDF content...");
        searchField.setPrefWidth(200);
        searchButton = new Button("Search");
        searchButton.setOnAction(e -> searchPdf());
        searchField.setOnAction(e -> searchPdf());

        searchBox.getChildren().addAll(searchField, searchButton);

        // Page navigation
        HBox pageControls = new HBox(10);
        prevPageButton = new Button("◀ Prev");
        nextPageButton = new Button("Next ▶");
        pageInfoLabel = new Label("Page: -/-");

        prevPageButton.setOnAction(e -> previousPage());
        nextPageButton.setOnAction(e -> nextPage());

        pageControls.getChildren().addAll(prevPageButton, pageInfoLabel, nextPageButton);

        // Disable controls initially
        disablePdfControls();

        // PDF display area
        pdfImageView = new ImageView();
        pdfImageView.setPreserveRatio(true);
        pdfImageView.setFitWidth(330);

        pdfScrollPane = new ScrollPane(pdfImageView);
        pdfScrollPane.setPrefHeight(400);
        pdfScrollPane.setStyle("-fx-background-color: white;");

        pdfPanel.getChildren().addAll(
                title,
                loadPdfButton,
                pdfStatusLabel,
                new Separator(),
                searchBox,
                pageControls,
                pdfScrollPane
        );

        return pdfPanel;
    }

    private void loadPdfFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select PDF Rulebook");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                // Close previous document if open
                if (currentPdfDocument != null) {
                    currentPdfDocument.close();
                }

                // Load new PDF
                currentPdfDocument = Loader.loadPDF(selectedFile);
                pdfRenderer = new PDFRenderer(currentPdfDocument);
                totalPages = currentPdfDocument.getNumberOfPages();
                currentPage = 0;
                currentPdfName = selectedFile.getName();

                // Update UI
                pdfStatusLabel.setText("Loaded: " + currentPdfName);
                enablePdfControls();
                displayCurrentPage();

                System.out.println("Loaded PDF: " + currentPdfName + " (" + totalPages + " pages)");

            } catch (IOException e) {
                showAlert("Error", "Failed to load PDF: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void displayCurrentPage() {
        if (currentPdfDocument == null) return;

        try {
            // Render the current page
            BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(currentPage, 150);

            // Convert BufferedImage to JavaFX Image
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", byteOutput);
            ByteArrayInputStream byteInput = new ByteArrayInputStream(byteOutput.toByteArray());
            Image fxImage = new Image(byteInput);

            // Display the image
            pdfImageView.setImage(fxImage);

            // Update page info
            pageInfoLabel.setText(String.format("Page: %d/%d", currentPage + 1, totalPages));

            // Update button states
            prevPageButton.setDisable(currentPage <= 0);
            nextPageButton.setDisable(currentPage >= totalPages - 1);

        } catch (IOException e) {
            showAlert("Error", "Failed to display page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            displayCurrentPage();
        }
    }

    private void nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            displayCurrentPage();
        }
    }

    private void searchPdf() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty() || currentPdfDocument == null) {
            return;
        }

        try {
            // Simple search implementation - you could enhance this
            boolean found = false;
            for (int pageNum = 1; pageNum < totalPages; pageNum++) {
                String pageText = currentPdfDocument.getPage(pageNum).getContents().toString();
                System.out.println(pageText.toString());
                if (pageText.toLowerCase().contains(searchText.toLowerCase())) {
                    currentPage = pageNum;
                    displayCurrentPage();
                    found = true;
                    System.out.println("Found '" + searchText + "' on page " + (pageNum + 1));
                    break;
                }
            }

            if (!found) {
                showAlert("Search", "Text '" + searchText + "' not found in remaining pages.");
            }

        } catch (Exception e) {
            showAlert("Error", "Search failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void enablePdfControls() {
        prevPageButton.setDisable(false);
        nextPageButton.setDisable(false);
        searchButton.setDisable(false);
        searchField.setDisable(false);
    }

    private void disablePdfControls() {
        prevPageButton.setDisable(true);
        nextPageButton.setDisable(true);
        searchButton.setDisable(true);
        searchField.setDisable(true);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Button createNoneSelectedButton() {
        Button button = new Button();
        button.setPrefWidth(SIDEBAR_WIDTH - 30);
        button.setPrefHeight(50);

        Circle noTokenIcon = new Circle(12);
        noTokenIcon.setFill(Color.TRANSPARENT);
        noTokenIcon.setStroke(Color.GRAY);
        noTokenIcon.setStrokeWidth(2);
        noTokenIcon.getStrokeDashArray().addAll(5d, 5d);

        Label typeLabel = new Label("None");
        typeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 10px;");

        VBox buttonContent = new VBox(5);
        buttonContent.getChildren().addAll(noTokenIcon, typeLabel);
        buttonContent.setStyle("-fx-alignment: center;");

        button.setGraphic(buttonContent);

        button.setOnAction(event -> {
            currentTokenType = null;
            statusLabel.setText("Current Token: None");
            updateButtonSelection();
            System.out.println("No token type selected");
        });

        return button;
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

        // Add "None Selected" button first
        Button noneButton = createNoneSelectedButton();
        tokenTypeButtons.add(noneButton);
        panel.getChildren().add(noneButton);

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

            boolean isSelected;
            if (i == 0) {
                // First button is "None Selected"
                isSelected = (currentTokenType == null);
            }
            else {
                TokenType buttonType = TokenType.values()[i - 1];
                isSelected = (currentTokenType == buttonType);
            }

            if (isSelected) {
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
            isShifting = true;
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
        } else if (!isDragging && currentTokenType != null) {
            // Regular click - place new token
            int gridX = (int) (event.getX() / GRID_SIZE);
            int gridY = (int) (event.getY() / GRID_SIZE);
            if (isShifting) {
                isShifting = false;
                return;
            }
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
            if (currentTokenType != null &&
                    gridX >= 0 && gridX < CANVAS_WIDTH / GRID_SIZE &&
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
//        gc.setFont(Font.font(10));
//
//        for (int x = 0; x < CANVAS_WIDTH / GRID_SIZE; x++) {
//            for (int y = 0; y < CANVAS_HEIGHT / GRID_SIZE; y++) {
//                String coords = x + "," + y;
//                gc.fillText(coords, x * GRID_SIZE + 2, y * GRID_SIZE + 12);
//            }
//        }
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