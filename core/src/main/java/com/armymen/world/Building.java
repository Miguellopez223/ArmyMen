package com.armymen.world;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Edificio genérico (HQ, Depósito, Garaje).
 * - Soporta construcción diferida (underConstruction).
 * - Tiene vida/HP y puede ser destruido.
 * - Mantiene un "footprint" de 64x64 centrado en su posición para colisiones/colocación.
 *
 * NOTA: El render aquí puede ser mínimo; GameScreen ya dibuja overlays (colas/obra).
 * Si tu versión anterior cargaba texturas, podés reinsertar esa lógica sin tocar las firmas.
 */
public class Building {

    // --- Geometría/colisión ---
    private static final float SIZE = 64f;
    private static final float HALF = SIZE / 2f;

    private final Vector2 position = new Vector2();
    private final Rectangle bounds = new Rectangle();

    // --- Obra/Construcción ---
    private boolean underConstruction = false;
    private float buildTotal = 0f;
    private float buildTimeLeft = 0f;

    // --- Vida/Daño ---
    private int maxHp = 500;
    private int hp     = maxHp;

    // --- Meta/identidad visual mínima ---
    private final String textureName; // conservamos la firma original del ctor

    public Building(Vector2 pos, String textureName) {
        if (pos != null) this.position.set(pos);
        this.textureName = textureName;

        // footprint 64x64 centrado
        this.bounds.set(position.x - HALF, position.y - HALF, SIZE, SIZE);
    }

    // ================== Construcción ==================
    /** Inicia construcción por N segundos. Durante la obra el edificio no produce. */
    public void startConstruction(float seconds) {
        this.underConstruction = true;
        this.buildTotal = Math.max(0f, seconds);
        this.buildTimeLeft = this.buildTotal;
    }

    /** ¿Está en obra? */
    public boolean isUnderConstruction() { return underConstruction; }

    /** Progreso 0..1 (1 = terminado). */
    public float getBuildProgress01() {
        if (!underConstruction || buildTotal <= 0f) return 1f;
        return Math.max(0f, Math.min(1f, 1f - (buildTimeLeft / buildTotal)));
    }

    // ================== Vida/Daño ==================
    /** Aplica daño; devuelve true si el edificio quedó destruido. */
    public boolean takeDamage(int dmg) {
        if (isDestroyed()) return true;
        hp -= Math.max(0, dmg);
        return hp <= 0;
    }

    public boolean isDestroyed() { return hp <= 0; }
    public int getHp()          { return hp; }
    public int getMaxHp()       { return maxHp; }
    public void setMaxHp(int m) {
        maxHp = Math.max(1, m);
        hp = Math.min(hp, maxHp);
    }

    // ================== Ciclo de vida ==================
    public void update(float dt) {
        // Avanzar obra
        if (underConstruction) {
            buildTimeLeft -= dt;
            if (buildTimeLeft <= 0f) {
                buildTimeLeft = 0f;
                underConstruction = false;
            }
        }
        // Mantener bounds centrado en position (por si el edificio se animara)
        bounds.set(position.x - HALF, position.y - HALF, SIZE, SIZE);
    }

    // ================== Render ==================
    public void render(SpriteBatch batch) {
        // Aquí tu versión anterior quizá dibujaba textura.
        // Lo dejamos vacío para no introducir dependencias; GameScreen ya dibuja overlays.
        // Si tenías sprites, podés reinsertar tu draw(...) aquí sin cambiar firmas.
    }

    // ================== Getters útiles ==================
    public Vector2 getPosition() { return position; }
    public Rectangle getBounds() { return bounds; }
    public String getTextureName() { return textureName; }
}
