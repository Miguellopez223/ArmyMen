package com.armymen.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Building {

    private Vector2 position;
    private Texture texture;
    private Rectangle bounds;

    public Building(Vector2 position, String texturePath) {
        this.position = position;
        this.texture = new Texture(texturePath);
        this.bounds = new Rectangle(position.x - 40, position.y - 40, 80, 80); // tamaño base
    }

    public void render(SpriteBatch batch) {
        batch.draw(texture, position.x - 40, position.y - 40, 80, 80);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public Vector2 getPosition() {
        return position;
    }
}
