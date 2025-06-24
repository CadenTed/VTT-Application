package com.vtt;

import javafx.scene.paint.Color;

public class Token {
    private int gridX, gridY;
    private TokenType type;

    public Token(int gridX, int gridY, TokenType type) {
        this.gridX = gridX;
        this.gridY = gridY;
        this.type = type;
    }

    public int getGridX() { return gridX; }
    public int getGridY() { return gridY; }
    public TokenType getType() { return type; }

    public void setGridX(int gridX) { this.gridX = gridX; }
    public void setGridY(int gridY) { this.gridY = gridY; }
    public void setType(TokenType type) { this.type = type; }

    @Override
    public String toString() {
        return String.format("%s at (%d, %d)", type.name(), gridX, gridY);
    }
}
