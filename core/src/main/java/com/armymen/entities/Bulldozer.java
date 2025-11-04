package com.armymen.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.armymen.entities.BuildingKind;

public class Bulldozer extends Unit {

    private boolean constructing = false;
    private boolean movingToBuild = false;
    private float buildTimer = 0f;
    private float buildTime = 3f;
    private Vector2 pendingBuildPos;
    private Vector2 buildApproachPos;
    private BuildingKind pendingKind = BuildingKind.DEPOT; // NUEVO

    private static BuildListener buildListener;

    public Bulldozer(Vector2 startPos) {
        super(startPos);
        this.setTexture(new Texture("bulldozer.png"));
        this.unitTag = "BULLDOZER";
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        if (movingToBuild && buildApproachPos != null) {
            if (position.dst(buildApproachPos) < 8f) {
                movingToBuild = false;
                startBuilding();
            }
        }
        if (constructing) {
            buildTimer += delta;
            if (buildTimer >= buildTime) {
                constructing = false;
                buildTimer = 0f;
                finishBuilding();
            }
        }
    }

    /** Orden de construir un tipo de edificio en pos */
    public void orderBuild(Vector2 pos, BuildingKind kind) {
        if (!constructing && !movingToBuild) {
            pendingBuildPos = pos.cpy();
            pendingKind = kind;               // NUEVO
            buildApproachPos = new Vector2(pos.x, pos.y - 80);
            setTarget(buildApproachPos);
            movingToBuild = true;
        }
    }

    private void startBuilding() {
        constructing = true;
        buildTimer = 0f;
    }

    private void finishBuilding() {
        if (pendingBuildPos != null && buildListener != null) {
            String tex = "building_storage.png";
            if (pendingKind == BuildingKind.HQ) tex = "building_hq.png";
            else if (pendingKind == BuildingKind.GARAGE) tex = "building_garage.png";
            else tex = "building_storage.png"; // DEPOT por defecto

            Building newBuilding = new Building(pendingBuildPos, tex, pendingKind);
            buildListener.onBuildingCreated(newBuilding);

            pendingBuildPos = null;
            buildApproachPos = null;
        }
    }

    public boolean isConstructing() { return constructing; }

    public interface BuildListener {
        void onBuildingCreated(Building building);
    }
    public static void setBuildListener(BuildListener listener) {
        buildListener = listener;
    }
}
