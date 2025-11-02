package com.armymen.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class Bulldozer extends Unit {

    private boolean constructing = false;
    private float buildTimer = 0f;
    private float buildTime = 3f; // tiempo en segundos para construir

    public Bulldozer(Vector2 startPos) {
        super(startPos);
        this.setTexture(new Texture("bulldozer.png"));
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (constructing) {
            buildTimer += delta;
            if (buildTimer >= buildTime) {
                constructing = false;
                buildTimer = 0f;
            }
        }
    }

    public boolean isConstructing() { return constructing; }

    public void startConstruction() {
        constructing = true;
        buildTimer = 0f;
    }
}
