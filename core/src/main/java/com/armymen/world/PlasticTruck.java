package com.armymen.world;

import com.armymen.entities.ToyPile;
import com.badlogic.gdx.math.Vector2;

import java.util.function.IntConsumer;

/**
 * Volqueta recolectora de plástico.
 *
 * Comportamiento:
 * - Si tiene ToyPile asignado:
 *    - Va al ToyPile, recolecta hasta llenarse (no depende de que exista Depósito).
 *    - Si está llena:
 *        * Si hay Depósito (storageAnchor != null) => va a descargar.
 *        * Si NO hay Depósito => queda esperando con la carga (no crashea).
 * - Cuando más tarde se construye un Depósito y se le asigna storageAnchor,
 *   si estaba esperando y tiene carga, automáticamente se dirige a descargar.
 *
 * Notas:
 * - Este código NO descuenta stock del ToyPile (porque tu clase ToyPile puede variar).
 *   Si querés, luego integramos la resta real del stock con el método que tengas.
 */
public class PlasticTruck extends Unit {

    // ---- Config ----
    private static final int CAPACITY = 150;
    private static final float SPEED = 160f;
    private static final float HARVEST_RATE = 45f; // unidades/seg
    private static final float PILE_RADIUS = 22f;  // distancia para recolectar
    private static final float STORAGE_RADIUS = 24f; // distancia para descargar

    // ---- Estado ----
    private enum State { IDLE, TO_PILE, HARVESTING, TO_STORAGE, WAITING_STORAGE }
    private State state = State.IDLE;

    private ToyPile assignedPile;
    private Vector2 storageAnchor; // puede ser null
    private int cargo = 0;

    // A quién “entregamos” el plástico (suma al HUD)
    private IntConsumer resourceSink;

    public PlasticTruck(Vector2 pos) {
        super(new Vector2(pos));
        // Sin setSpeed(): usamos la velocidad por defecto que maneja Unit en su update().
        // (Si luego agregamos un setter en Unit, acá podremos fijar SPEED explícitamente.)
    }


    // =============== API pública usada por GameScreen ===============
    public void assignToyPile(ToyPile pile) {
        this.assignedPile = pile;
        if (pile != null) {
            // si no está llena, ir a recolectar
            if (cargo < CAPACITY) {
                state = State.TO_PILE;
                setTarget(new Vector2(pile.getPosition()));
            } else {
                // si ya está llena, decidir
                decideAfterFull();
            }
        } else {
            // sin pile asignado
            if (cargo > 0) decideAfterFull();
            else {
                state = State.IDLE;
                // no ponemos target; queda donde esté
            }
        }
    }

    /** Puede ser null (si no hay depósito). */
    public void setStorageAnchor(Vector2 anchor) {
        this.storageAnchor = (anchor == null) ? null : new Vector2(anchor);
        // Si antes estaba esperando porque no había depósito y tiene carga -> ahora va a descargar
        if (this.storageAnchor != null && state == State.WAITING_STORAGE && cargo > 0) {
            state = State.TO_STORAGE;
            setTarget(new Vector2(this.storageAnchor));
        }
    }

    public void setResourceSink(IntConsumer sink) {
        this.resourceSink = sink;
    }

    public int getCapacity() { return CAPACITY; }
    public int getCargo() { return cargo; }

    // =============== Ciclo de vida ===============
    @Override
    public void update(float dt) {
        switch (state) {
            case IDLE: {
                // Si tenemos pile asignado y no estamos llenos, ir a recolectar
                if (assignedPile != null && cargo < CAPACITY) {
                    state = State.TO_PILE;
                    setTarget(new Vector2(assignedPile.getPosition()));
                }
            } break;

            case TO_PILE: {
                if (assignedPile == null) { state = State.IDLE; break; }
                // ¿Llegó al área del ToyPile?
                if (arrived(getPosition(), assignedPile.getPosition(), PILE_RADIUS)) {
                    state = State.HARVESTING;
                    // detenerse “sobre” el pile para recolectar
                    setTarget(new Vector2(getPosition()));
                }
            } break;

            case HARVESTING: {
                if (assignedPile == null) { state = State.IDLE; break; }
                // Recolectar
                if (cargo < CAPACITY) {
                    cargo += Math.max(1, (int)(HARVEST_RATE * dt));
                    if (cargo > CAPACITY) cargo = CAPACITY;
                    // (Aquí podríamos descontar stock real del ToyPile si tenés método, p.ej. assignedPile.take(n))
                }
                // Si se llenó => decidir
                if (cargo >= CAPACITY) {
                    decideAfterFull();
                }
            } break;

            case TO_STORAGE: {
                if (storageAnchor == null) {
                    // si nos sacaron/ no hay depósito, esperar
                    state = State.WAITING_STORAGE;
                    break;
                }
                if (arrived(getPosition(), storageAnchor, STORAGE_RADIUS)) {
                    // Descargar instantáneamente
                    if (cargo > 0 && resourceSink != null) resourceSink.accept(cargo);
                    cargo = 0;
                    // Decidir siguiente acción
                    if (assignedPile != null) {
                        state = State.TO_PILE;
                        setTarget(new Vector2(assignedPile.getPosition()));
                    } else {
                        state = State.IDLE;
                    }
                }
            } break;

            case WAITING_STORAGE: {
                // Quedarse cerca/quieto hasta que exista storageAnchor
                if (storageAnchor != null) {
                    state = State.TO_STORAGE;
                    setTarget(new Vector2(storageAnchor));
                }
            } break;
        }

        // mover según el target que haya fijado el estado
        super.update(dt);
    }

    // =============== Helpers ===============
    private void decideAfterFull() {
        if (storageAnchor != null) {
            state = State.TO_STORAGE;
            setTarget(new Vector2(storageAnchor));
        } else {
            // No hay Depósito: quedar esperando con la carga (sin crashear)
            state = State.WAITING_STORAGE;
            // target actual se mantiene para no causar NPE
            // (si querés inmovilizar por completo, podés: setTarget(new Vector2(getPosition()));
        }
    }

    /** ¿pos está dentro de eps del objetivo? */
    private static boolean arrived(Vector2 pos, Vector2 target, float eps) {
        if (pos == null || target == null) return false;
        float e2 = eps * eps;
        return pos.dst2(target) <= e2;
    }
}
