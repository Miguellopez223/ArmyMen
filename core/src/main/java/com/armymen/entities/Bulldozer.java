package com.armymen.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

public class Bulldozer extends Unit {

    private boolean constructing = false;
    private float buildTimer = 0f;
    private float buildTime = 3f; // tiempo de construcción
    private Vector2 pendingBuildPos; // posición donde se construirá

    // 🔹 Listener estático (definido correctamente aquí)
    private static BuildListener buildListener;

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
                onBuildFinished();
            }
        }
    }

    public boolean isConstructing() { return constructing; }

    public void startConstruction(Vector2 pos) {
        if (!constructing) {
            constructing = true;
            buildTimer = 0f;
            pendingBuildPos = pos.cpy();
        }
    }

    private void onBuildFinished() {
        if (pendingBuildPos != null && buildListener != null) {
            Building newBuilding = new Building(pendingBuildPos, "building_storage.png");
            buildListener.onBuildingCreated(newBuilding);
            pendingBuildPos = null;
        }
    }

    // 🔹 Interfaz y métodos estáticos para registrar listener
    public interface BuildListener {
        void onBuildingCreated(Building building);
    }

    public static void setBuildListener(BuildListener listener) {
        buildListener = listener;
    }
}
