package com.vtt;

import javafx.scene.paint.Color;

public enum TokenType {
    PLAYER(Color.BLUE, "P"),
    MONSTER(Color.RED, "M"),
    NPC(Color.GREEN, "N"),
    OBJECT(Color.GRAY, "O");

    private final Color color;
    private final String label;

    TokenType(Color color, String label) {
        this.color = color;
        this.label = label;
    }

    public Color getColor() { return color; }
    public String getLabel() { return label; }

}