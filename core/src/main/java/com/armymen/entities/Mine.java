package com.armymen.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class Mine {
    private Vector2 position;
    private float radius = 30f;
    private Texture tex;

    public Mine(Vector2 position) {
        this.position = new Vector2(position);
        // Asegúrate de tener mine.png en assets (puede ser un ícono simple)
        this.tex = new Texture("mine.png");
    }

    public void render(SpriteBatch batch) {
        if (tex == null) return;
        // Evita crashear si alguien llama fuera de begin()
        if (!batch.isDrawing()) return;
        batch.draw(tex, position.x - 16, position.y - 16, 32, 32);
    }

    public Vector2 getPosition() { return position; }
    public float getRadius() { return radius; }
}
