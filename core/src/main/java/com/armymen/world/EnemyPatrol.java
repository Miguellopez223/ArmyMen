package com.armymen.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * EnemyPatrol — Soldado enemigo que recorre waypoints en bucle.
 *
 * - Extiende SoldierBasic para reutilizar movimiento, vida y disparo.
 * - Mantiene una lista de waypoints y va saltando al siguiente cuando llega.
 * - El disparo lo maneja GameScreen como con cualquier SoldierBasic enemigo.
 */
public class EnemyPatrol extends SoldierBasic {

    private final Array<Vector2> waypoints = new Array<>();
    private int currentIndex = 0;
    private float arriveThreshold = 10f; // distancia para considerar "llegó"

    public EnemyPatrol(Vector2 startPos, Array<Vector2> patrolPoints) {
        super(new Vector2(startPos)); // conserva API de SoldierBasic/Unit
        if (patrolPoints == null || patrolPoints.size == 0) {
            waypoints.add(new Vector2(startPos));
        } else {
            for (int i = 0; i < patrolPoints.size; i++) {
                waypoints.add(new Vector2(patrolPoints.get(i)));
            }
        }
        // objetivo inicial
        setTarget(new Vector2(waypoints.get(currentIndex)));
    }

    /** Si querés ajustar la sensibilidad de llegada. */
    public void setArriveThreshold(float t) {
        this.arriveThreshold = Math.max(1f, t);
    }

    /** Agregar un waypoint adicional. Si no tenías, se usará como destino inicial. */
    public void addWaypoint(Vector2 p) {
        if (p == null) return;
        waypoints.add(new Vector2(p));
        if (waypoints.size == 1) {
            currentIndex = 0;
            setTarget(new Vector2(waypoints.get(0)));
        }
    }

    @Override
    public void update(float dt) {
        // Movimiento base (va hacia el target actual)
        super.update(dt);

        // Chequear llegada y pasar al siguiente punto
        if (waypoints.size > 0) {
            Vector2 cur = waypoints.get(currentIndex);
            if (getPosition().dst2(cur) <= arriveThreshold * arriveThreshold) {
                currentIndex = (currentIndex + 1) % waypoints.size;
                setTarget(new Vector2(waypoints.get(currentIndex)));
            }
        }
    }
}
