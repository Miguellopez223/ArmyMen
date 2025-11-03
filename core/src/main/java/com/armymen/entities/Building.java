package com.armymen.entities;

import com.armymen.systems.CreationQueue;
import com.armymen.systems.ResourceManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Building {

    // --- Config costos y tiempos (ajústalos si quieres)
    public static final int COST_HQ = 100;
    public static final int COST_DEPOT = 50;
    public static final int COST_GARAGE = 120;

    // Unidades (costos/tiempos)
    public static final int COST_SOLDIER = 30;
    public static final float TIME_SOLDIER = 1.0f;

    public static final int COST_SWEEPER = 40;
    public static final float TIME_SWEEPER = 1.2f;

    public static final int COST_TRUCK = 50;
    public static final float TIME_TRUCK = 1.2f;

    public static final int COST_TANK = 90;
    public static final float TIME_TANK = 1.6f;

    private Vector2 position;
    private Texture texture;
    private Rectangle bounds;

    private BuildingKind kind;

    // Solo HQ y GARAGE usan cola
    private CreationQueue queue = null;
    private float creationTimer = 0f;

    public Building(Vector2 position, String texturePath) {
        this(position, texturePath, BuildingKind.DEPOT);
    }

    public Building(Vector2 position, String texturePath, BuildingKind kind) {
        this.position = position;
        this.texture = new Texture(texturePath);
        this.bounds = new Rectangle(position.x - 40, position.y - 40, 80, 80);
        this.kind = kind;

        if (kind == BuildingKind.HQ || kind == BuildingKind.GARAGE) {
            queue = new CreationQueue();
        }
    }

    public void render(SpriteBatch batch) {
        batch.draw(texture, position.x - 40, position.y - 40, 80, 80);
    }

    public void update(float dt, Array<Unit> allUnits, ResourceManager rm) {
        if (queue == null) return; // depósitos no producen

        if (creationTimer > 0f) {
            creationTimer -= dt;
            if (creationTimer <= 0f) {
                // Timer terminó: intentar crear usando el primer pedido si hay recursos
                tryCreateOne(allUnits, rm);
            }
            return;
        }

        // Si no hay producción en curso, intenta empezar la siguiente si hay recursos
        if (queue.hasOrders()) {
            String next = queue.peek();
            if (hasResourcesFor(next, rm)) {
                // Cobrar al inicio de la producción
                spendFor(next, rm);
                creationTimer = timeFor(next);
            }
        }
    }

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

        // Crear unidad simple (si no tienes sprites, todas usan soldier.png)
        Vector2 spawn = new Vector2(position.x + 50, position.y + 50);
        Unit u;
        if (type.equals("SOLDIER")) {
            u = new Unit(spawn);
        } else if (type.equals("SWEEPER")) {
            u = new Unit(spawn);     // placeholder; luego puedes hacer una subclase MineSweeper
            u.setTexture(new Texture("soldier.png"));
        } else if (type.equals("TRUCK")) {
            u = new Unit(spawn);     // placeholder; luego harás Truck con lógica de recolección
            u.setTexture(new Texture("soldier.png"));
        } else { // TANK
            u = new Unit(spawn);
            u.setTexture(new Texture("soldier.png"));
        }
        allUnits.add(u);

        queue.poll();        // sacar de la cola
        creationTimer = 0f;  // listo para siguiente
    }

    // --- API cola ---
    public boolean hasQueue() { return queue != null; }
    public CreationQueue getQueue() { return queue; }

    // --- Getters ---
    public Rectangle getBounds() { return bounds; }
    public Vector2 getPosition() { return position; }
    public BuildingKind getKind() { return kind; }
}
