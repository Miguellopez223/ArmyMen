package com.armymen.entities;

import com.armymen.systems.ResourceManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class PlasticTruck extends Unit {

    // Estado simple de la volqueta
    private enum TruckState { IDLE, GOING_TO_PILE, LOADING, GOING_TO_DEPOT, UNLOADING }
    private TruckState state = TruckState.IDLE;

    // Carga
    private int capacity = 50;   // máxima carga por viaje
    private int load = 0;

    // Objetivos actuales
    private ToyPile targetPile = null;
    private Building targetDepot = null;

    // Timers para "acciones" (simular tiempo de cargar/descargar)
    private float actionTimer = 0f;
    private float loadTime = 1.2f;       // segundos para cargar
    private float unloadTime = 0.8f;     // segundos para descargar

    public PlasticTruck(Vector2 startPos) {
        super(startPos);
        this.setTexture(new Texture("truck.png"));  // crea/coloca truck.png
        this.speed = 220f; // un poco más rápido que soldado
    }

    /** Lógica económica: llamar desde GameScreen.update() SOLO para PlasticTruck */
    public void updateEconomy(float delta,
                              Array<ToyPile> piles,
                              Array<Building> buildings,
                              ResourceManager rm) {

        switch (state) {
            case IDLE:
                // Buscar pila con material
                targetPile = findNearestPileWithMaterial(piles);
                if (targetPile != null) {
                    setTarget(targetPile.getPosition());
                    state = TruckState.GOING_TO_PILE;
                }
                break;

            case GOING_TO_PILE:
                // Llegamos si estamos cerca
                if (position.dst(targetPile.getPosition()) <= targetPile.getRadius() + 8f) {
                    actionTimer = 0f;
                    state = TruckState.LOADING;
                }
                break;

            case LOADING:
                actionTimer += delta;
                if (actionTimer >= loadTime) {
                    actionTimer = 0f;

                    // Cargar hasta completar capacidad o agotar pila
                    int need = capacity - load;
                    int loaded = targetPile.take(need);
                    load += loaded;

                    // Si la pila ya quedó vacía, la marcamos para eliminar
                    if (!targetPile.hasMaterial()) {
                        targetPile = null;
                    }

                    // Elegir DEPOT para descargar
                    targetDepot = findNearestDepot(buildings);
                    if (targetDepot != null && load > 0) {
                        setTarget(targetDepot.getPosition());
                        state = TruckState.GOING_TO_DEPOT;
                    } else {
                        // Sin depot o no cargamos nada → volver a IDLE para reintentar
                        state = TruckState.IDLE;
                    }
                }
                break;

            case GOING_TO_DEPOT:
                if (position.dst(targetDepot.getPosition()) <= 64f) { // margen sencillo
                    actionTimer = 0f;
                    state = TruckState.UNLOADING;
                }
                break;

            case UNLOADING:
                actionTimer += delta;
                if (actionTimer >= unloadTime) {
                    actionTimer = 0f;
                    if (load > 0) {
                        rm.add(load);    // ¡Aquí sube el plástico automáticamente!
                        load = 0;
                    }
                    // volver a buscar más material
                    state = TruckState.IDLE;
                }
                break;
        }
    }

    private ToyPile findNearestPileWithMaterial(Array<ToyPile> piles) {
        ToyPile best = null;
        float bestDist = Float.MAX_VALUE;
        for (int i = 0; i < piles.size; i++) {
            ToyPile p = piles.get(i);
            if (!p.hasMaterial()) continue;
            float d = position.dst(p.getPosition());
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    private Building findNearestDepot(Array<Building> buildings) {
        Building best = null;
        float bestDist = Float.MAX_VALUE;
        for (int i = 0; i < buildings.size; i++) {
            Building b = buildings.get(i);
            if (b.getKind() != BuildingKind.DEPOT) continue;
            float d = position.dst(b.getPosition());
            if (d < bestDist) {
                bestDist = d;
                best = b;
            }
        }
        return best;
    }
}
