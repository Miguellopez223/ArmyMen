package com.armymen.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;

public class Unit {

    private Vector2 position;
    private Vector2 target;
    private float speed = 200f; // velocidad más alta
    private float radius = 25f; // más grande para verlos
    private Texture tex;

    public Unit(Vector2 startPos) {
        this.position = new Vector2(startPos);
        this.target = new Vector2(startPos);
        this.tex = new Texture("soldier.png");
    }

    public void update(float delta) {
        if (!position.epsilonEquals(target, 1f)) {
            Vector2 dir = new Vector2(target).sub(position);
            float len = dir.len();
            if (len > 1f) {
                dir.nor();
                position.mulAdd(dir, speed * delta);
            } else {
                position.set(target);
            }
        }
    }

    public void render(SpriteBatch batch) {
        // Dibuja el soldado (32x32 -> lo hacemos más grande)
        if (tex != null) {
            batch.draw(tex, position.x - 20, position.y - 20, 40, 40);
        }
    }

    public boolean contains(Vector2 point) {
        return new Circle(position, radius).contains(point);
    }

    public void setTarget(Vector2 target) { this.target.set(target); }
    public Vector2 getPosition() { return position; }
}
