package com.armymen.world;

import com.badlogic.gdx.math.Vector2;

/**
 * Bulldozer con puntos de vida (más que un soldado).
 * Puede recibir daño y morir.
 */
public class Bulldozer extends Unit {

    // Asumiendo que un soldado ~100 HP, le damos "unos cuantos más"
    private int maxHp = 180;
    private int hp = maxHp;

    public Bulldozer(Vector2 pos) {
        super(pos);
    }

    /** Aplica daño. Devuelve true si muere en esta llamada. */
    public boolean takeDamage(int dmg) {
        if (dmg < 0) dmg = 0;
        hp -= dmg;
        if (hp < 0) hp = 0;
        return hp == 0;
    }

    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }

    /** Opcional: por si querés curarlo en algún evento. */
    public void healToFull() { hp = maxHp; }
}
