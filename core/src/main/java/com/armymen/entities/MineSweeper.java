package com.armymen.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

public class MineSweeper extends Unit {
    private float detectRadius = 100f; // rango para desactivar sin pisar

    public MineSweeper(Vector2 startPos) {
        super(startPos);
        this.setTexture(new Texture("sweeper.png"));
        this.speed = 180f;
        this.unitTag = "MINESWEEPER";
    }

    public float getDetectRadius() { return detectRadius; } // NEW
}
