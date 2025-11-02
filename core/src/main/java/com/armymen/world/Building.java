package com.armymen.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Edificio simple con estado de construcción.
 * - Puede estar "en construcción" y completar en un tiempo dado.
 * - Mientras está en construcción, se dibuja con alpha reducido.
 * - Mantiene compatibilidad: getPosition(), getBounds(), render(SpriteBatch).
 */
public class Building {

    private final Vector2 position = new Vector2();
    private final Rectangle bounds;

    private final Texture texture;
    private final float width;
    private final float height;

    // --- Construcción ---
    private boolean underConstruction = false;
    private float buildTotal = 0f;      // segundos
    private float buildTimeLeft = 0f;   // segundos

    public Building(Vector2 pos, String textureFile) {
        if (pos != null) this.position.set(pos);
        this.texture = (textureFile != null && !textureFile.isEmpty())
            ? new Texture(Gdx.files.internal(textureFile))
            : null;

        if (texture != null) {
            this.width  = texture.getWidth();
            this.height = texture.getHeight();
        } else {
            // fallback por si no hay textura
            this.width = 64f;
            this.height = 64f;
        }
        this.bounds = new Rectangle(position.x - width / 2f, position.y - height / 2f, width, height);
    }

    /** Inicia la construcción por la cantidad de segundos indicada. */
    public void startConstruction(float seconds) {
        this.underConstruction = true;
        this.buildTotal = Math.max(0f, seconds);
        this.buildTimeLeft = this.buildTotal;
    }

    /** Llamar cada frame si querés que avance la construcción. */
    public void update(float dt) {
        // bounds sigue centrado en la posición por si movés el edificio (habitualmente no)
        bounds.setPosition(position.x - width / 2f, position.y - height / 2f);

        if (underConstruction) {
            buildTimeLeft -= dt;
            if (buildTimeLeft <= 0f) {
                buildTimeLeft = 0f;
                underConstruction = false; // edificio completado
            }
        }
    }

    /** 0..1 de progreso de construcción (1 = completado). */
    public float getBuildProgress01() {
        if (!underConstruction || buildTotal <= 0f) return 1f;
        return Math.max(0f, Math.min(1f, 1f - (buildTimeLeft / buildTotal)));
    }

    public boolean isUnderConstruction() { return underConstruction; }

    public Vector2 getPosition() { return position; }

    public Rectangle getBounds() { return bounds; }

    public void render(SpriteBatch batch) {
        if (batch == null || texture == null) return;

        // Dibujamos centrado
        float x = position.x - width / 2f;
        float y = position.y - height / 2f;

        // Si está en construcción, bajar la opacidad para dar feedback visual
        Color old = batch.getColor();
        if (underConstruction) {
            batch.setColor(old.r, old.g, old.b, 0.45f);
        }

        batch.draw(texture, x, y);

        if (underConstruction) {
            batch.setColor(old);
        }
    }
}
