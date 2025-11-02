package com.armymen.world;

import com.badlogic.gdx.math.Vector2;

/**
 * Soldado básico con:
 * - Rango y cooldown de ataque (como antes).
 * - Vida (HP) y utilidades para daño/curación.
 *
 * NOTA: El dibujo de la barra de vida y la aplicación del daño la haremos en GameScreen.
 */
public class SoldierBasic extends Unit {

    // ----- Ataque -----
    private float attackRange = 260f;      // px
    private float attackCooldown = 0.60f;  // s entre disparos
    private float attackTimer = 0f;        // contador interno

    // ----- Vida -----
    private int maxHp = 100;
    private int hp    = 1000;

    public SoldierBasic(Vector2 pos) {
        super(pos);
    }

    public SoldierBasic(float x, float y) {
        super(new Vector2(x, y));
    }

    // ===== Ataque =====
    public void tickAttackTimer(float dt) {
        if (attackTimer > 0f) attackTimer -= dt;
    }

    public boolean canShoot() {
        return attackTimer <= 0f;
    }

    public void onShotFired() {
        this.attackTimer = attackCooldown;
    }

    public boolean isTargetInRange(Unit target) {
        if (target == null) return false;
        float r = attackRange;
        return this.getPosition().dst2(target.getPosition()) <= r * r;
    }

    // ===== Vida / Daño =====
    public int  getHp()        { return hp; }
    public int  getMaxHp()     { return maxHp; }
    public void setMaxHp(int v){ this.maxHp = Math.max(1, v); this.hp = Math.min(this.hp, this.maxHp); }
    public void heal(int v)    { if (v > 0) this.hp = Math.min(this.maxHp, this.hp + v); }

    /** Aplica daño. @return true si quedó muerto (hp <= 0). */
    public boolean takeDamage(int dmg) {
        if (dmg < 0) return false;
        this.hp -= dmg;
        return this.hp <= 0;
    }

    // ===== Getters/Setters de ataque (por si querés tunear) =====
    public float getAttackRange() { return attackRange; }
    public void  setAttackRange(float r) { this.attackRange = Math.max(1f, r); }

    public float getAttackCooldown() { return attackCooldown; }
    public void  setAttackCooldown(float s) { this.attackCooldown = Math.max(0.05f, s); }
}
