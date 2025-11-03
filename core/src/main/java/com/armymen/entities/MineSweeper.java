package com.armymen.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

public class MineSweeper extends Unit {

    public MineSweeper(Vector2 startPos) {
        super(startPos);
        // Asegúrate de tener sweeper.png (puedes reutilizar soldier.png si no lo tienes)
        this.setTexture(new Texture("sweeper.png"));
        // Opcionalmente, que el buscaminas sea un poco más lento
        this.speed = 180f;
    }
}
