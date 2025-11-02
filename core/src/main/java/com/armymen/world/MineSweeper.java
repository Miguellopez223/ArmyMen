package com.armymen.world;

import com.armymen.entities.Mine;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

/**
 * Unidad "Busca Minas".
 * - Se mueve como Unit.
 * - Puede desactivar minas dentro de un radio (disarmRadius).
 * - Ahora tiene HP y puede morir.
 */
public class MineSweeper extends Unit {

    /** Radio en el que desactiva minas (ajustado previamente). */
    private float disarmRadius = 200f;

    /** Si está activo, cuando esté lo suficientemente cerca intentará desarmar automáticamente. */
    private boolean autoDisarm = true;

    // --- Vida ---
    // Referencia: soldado ~100 HP. Dejamos igual para empezar.
    private int maxHp = 100;
    private int hp = maxHp;

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

    // --- Daño/Muerte ---
    /** Aplica daño y devuelve true si muere en esta llamada. */
    public boolean takeDamage(int dmg) {
        if (dmg < 0) dmg = 0;
        hp -= dmg;
        if (hp < 0) hp = 0;
        return hp == 0;
    }

    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }

    // --- Getters/Setters específicos ---
    public float getDisarmRadius() { return disarmRadius; }
    public void setDisarmRadius(float disarmRadius) { this.disarmRadius = Math.max(1f, disarmRadius); }

    public boolean isAutoDisarm() { return autoDisarm; }
    public void setAutoDisarm(boolean autoDisarm) { this.autoDisarm = autoDisarm; }

    // Mantiene la firma de tu Unit: render(SpriteBatch)
    @Override
    public void render(SpriteBatch batch) {
        super.render(batch);
        // Si querés, luego podemos dibujar una barra de vida o el radio con ShapeRenderer desde GameScreen.
    }
}
