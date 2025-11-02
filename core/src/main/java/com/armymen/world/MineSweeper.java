package com.armymen.world;

import com.armymen.entities.Mine;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

/**
 * Unidad "Busca Minas".
 * - Hereda movimiento/comportamiento base de Unit.
 * - Puede desactivar minas dentro de un radio (disarmRadius).
 */
public class MineSweeper extends Unit {

    /** Radio en el que desactiva minas (por defecto parecido al blast/activation). */
    private float disarmRadius = 30f;

    /** Si está activo, cuando esté lo suficientemente cerca intentará desarmar automáticamente. */
    private boolean autoDisarm = true;

    public MineSweeper(Vector2 pos) { super(pos); }
    public MineSweeper(float x, float y) { super(new Vector2(x, y)); }

    /**
     * Intenta desarmar la mina si está en rango.
     * @return true si la mina fue desactivada en este llamado.
     */
    public boolean tryDisarm(Mine mine) {
        if (mine == null || !mine.isArmed()) return false;
        float r2 = disarmRadius * disarmRadius;
        if (getPosition().dst2(mine.getPosition()) <= r2) {
            mine.disarm();
            return true;
        }
        return false;
    }

    // --- Getters/Setters específicos ---
    public float getDisarmRadius() { return disarmRadius; }
    public void setDisarmRadius(float disarmRadius) { this.disarmRadius = Math.max(1f, disarmRadius); }

    public boolean isAutoDisarm() { return autoDisarm; }
    public void setAutoDisarm(boolean autoDisarm) { this.autoDisarm = autoDisarm; }

    // Mantiene la firma de tu Unit: render(SpriteBatch)
    @Override
    public void render(SpriteBatch batch) {
        super.render(batch);
        // Luego podemos dibujar visual del radio de desarme con ShapeRenderer desde GameScreen.
    }
}
