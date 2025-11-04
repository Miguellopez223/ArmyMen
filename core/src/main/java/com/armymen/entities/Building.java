// Building.java
package com.armymen.entities;

import com.armymen.systems.CreationQueue;
import com.armymen.systems.ResourceManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Building {

    // --- Config ---
    public static final int COST_HQ = 100;
    public static final int COST_DEPOT = 50;
    public static final int COST_GARAGE = 120;

    public static final int COST_SOLDIER = 30;
    public static final float TIME_SOLDIER = 1.0f;

    public static final int COST_SWEEPER = 40;
    public static final float TIME_SWEEPER = 1.2f;

    public static final int COST_TRUCK = 50;
    public static final int COST_FORTIN = 80;
    public static final float TIME_TRUCK = 1.2f;

    public static final int COST_TANK = 90;
    public static final float TIME_TANK = 1.6f;

    private Vector2 position;
    private Texture texture;
    private Rectangle bounds;
    private BuildingKind kind;

    // --- Bando y vida ---
    private boolean enemy = false;   // true si es edificio enemigo (tinte rojo)
    private float hp = 300f;         // vida simple de edificios

    // Solo HQ y GARAGE usan cola
    private CreationQueue queue = null;
    private float creationTimer = 0f;
    private float incomeTimer = 0f;

    public Building(Vector2 position, String texturePath) {
        this(position, texturePath, BuildingKind.DEPOT, false);
    }

    public Building(Vector2 position, String texturePath, BuildingKind kind) {
        this(position, texturePath, kind, false);
    }

    // NUEVO: constructor con “enemy”
    public Building(Vector2 position, String texturePath, BuildingKind kind, boolean enemy) {
        this.position = position;
        this.texture = new Texture(texturePath);
        this.bounds = new Rectangle(position.x - 40, position.y - 40, 80, 80);
        this.kind = kind;
        this.enemy = enemy;

        if (kind == BuildingKind.HQ || kind == BuildingKind.GARAGE) {
            queue = new CreationQueue();
        }
    }

    public void render(SpriteBatch batch) {
        if (enemy) {
            batch.setColor(1f, 0f, 0f, 1f);
            batch.draw(texture, position.x - 40, position.y - 40, 80, 80);
            batch.setColor(1f, 1f, 1f, 1f);
        } else {
            batch.draw(texture, position.x - 40, position.y - 40, 80, 80);
        }
    }

    public void update(float dt, Array<Unit> allUnits, ResourceManager rm) {
        // Economía pasiva de DEPOT SOLO si es del jugador
        if (kind == BuildingKind.DEPOT && !enemy) {
            incomeTimer += dt;
            if (incomeTimer >= 1.0f) {
                rm.add(5);
                incomeTimer = 0f;
            }
            return;
        }

        if (queue == null) return; // HQ/GARAGE del jugador producen
        if (enemy) return; // edificios enemigos no producen (sin IA)

        if (creationTimer > 0f) {
            creationTimer -= dt;
            if (creationTimer <= 0f) {
                tryCreateOne(allUnits, rm);
            }
            return;
        }

        if (queue.hasOrders()) {
            String next = queue.peek();
            if (hasResourcesFor(next, rm)) {
                spendFor(next, rm);
                creationTimer = timeFor(next);
            }
        }
    }

    // --- Combate muy simple para edificios ---
    public void damage(float amount) { hp -= amount; }
    public boolean isDestroyed() { return hp <= 0f; }

    // ... (resto igual)
    private boolean hasResourcesFor(String type, ResourceManager rm) {
        if (type.equals("SOLDIER")) return rm.getPlastic() >= COST_SOLDIER;
        if (type.equals("SWEEPER")) return rm.getPlastic() >= COST_SWEEPER;
        if (type.equals("TRUCK"))   return rm.getPlastic() >= COST_TRUCK;
        if (type.equals("TANK"))    return rm.getPlastic() >= COST_TANK;
        return false;
    }
    private void spendFor(String type, ResourceManager rm) {
        if (type.equals("SOLDIER")) rm.spend(COST_SOLDIER);
        else if (type.equals("SWEEPER")) rm.spend(COST_SWEEPER);
        else if (type.equals("TRUCK")) rm.spend(COST_TRUCK);
        else if (type.equals("TANK")) rm.spend(COST_TANK);
    }
    private float timeFor(String type) {
        if (type.equals("SOLDIER")) return TIME_SOLDIER;
        if (type.equals("SWEEPER")) return TIME_SWEEPER;
        if (type.equals("TRUCK"))   return TIME_TRUCK;
        if (type.equals("TANK"))    return TIME_TANK;
        return 1.0f;
    }
    private void tryCreateOne(Array<Unit> allUnits, ResourceManager rm) {
        if (!queue.hasOrders()) return;
        String type = queue.peek();

        Vector2 spawn = new Vector2(position.x + 50, position.y + 50);
        Unit u;
        if (type.equals("SOLDIER")) {
            u = new Unit(spawn);
            u.setTexture(new Texture("soldier.png"));
            u.setTag("SOLDIER");
        } else if (type.equals("SWEEPER")) {
            u = new MineSweeper(spawn);
        } else if (type.equals("TRUCK")) {
            u = new PlasticTruck(spawn);
        } else { // TANK
            u = new Unit(spawn);
            u.setTexture(new Texture("tank.png"));
            u.setTag("TANK");
            u.attackDamage = 20f;     // los tanques pegan más
            u.attackRange = 150f;
            u.hp = 160f;
        }
        allUnits.add(u);

        queue.poll();
        creationTimer = 0f;
    }

    // --- API cola ---
    public boolean hasQueue() { return queue != null; }
    public CreationQueue getQueue() { return queue; }

    // --- Getters ---
    public Rectangle getBounds() { return bounds; }
    public Vector2 getPosition() { return position; }
    public BuildingKind getKind() { return kind; }

    public boolean isEnemy() { return enemy; }
    public void setEnemy(boolean enemy) { this.enemy = enemy; }
}
