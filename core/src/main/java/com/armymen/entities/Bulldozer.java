package com.armymen.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

public class Bulldozer extends Unit {

    private boolean constructing = false;   // si está construyendo actualmente
    private boolean movingToBuild = false;  // si está yendo al lugar de construcción
    private float buildTimer = 0f;
    private float buildTime = 3f;           // segundos para construir
    private Vector2 pendingBuildPos;        // posición real del edificio
    private Vector2 buildApproachPos;       // posición donde el bulldozer se detiene

    private static BuildListener buildListener;

    public Bulldozer(Vector2 startPos) {
        super(startPos);
        this.setTexture(new Texture("bulldozer.png"));
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        // Si está yendo al lugar de construcción
        if (movingToBuild && buildApproachPos != null) {
            if (position.dst(buildApproachPos) < 8f) { // llegó a su punto de parada
                movingToBuild = false;
                startBuilding(); // comienza la construcción
            }
        }

        // Si está construyendo
        if (constructing) {
            buildTimer += delta;
            if (buildTimer >= buildTime) {
                constructing = false;
                buildTimer = 0f;
                finishBuilding();
            }
        }
    }

    /** Llamado desde GameScreen cuando se da la orden de construir */
    public void orderBuild(Vector2 pos) {
        if (!constructing && !movingToBuild) {
            pendingBuildPos = pos.cpy(); // donde irá el edificio

            // Calcular punto de parada un poco más abajo (80 px)
            buildApproachPos = new Vector2(pos.x, pos.y - 80);

            setTarget(buildApproachPos);
            movingToBuild = true;
        }
    }

    /** Empieza la construcción cuando llega al destino */
    private void startBuilding() {
        constructing = true;
        buildTimer = 0f;
    }

    /** Termina la construcción y crea el edificio */
    private void finishBuilding() {
        if (pendingBuildPos != null && buildListener != null) {
            Building newBuilding = new Building(pendingBuildPos, "building_storage.png");
            buildListener.onBuildingCreated(newBuilding);
            pendingBuildPos = null;
            buildApproachPos = null;
        }
    }

    public boolean isConstructing() { return constructing; }

    // --- Sistema de listener para notificar al GameScreen ---
    public interface BuildListener {
        void onBuildingCreated(Building building);
    }

    public static void setBuildListener(BuildListener listener) {
        buildListener = listener;
    }
}
