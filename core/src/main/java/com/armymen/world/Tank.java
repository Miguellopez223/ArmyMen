package com.armymen.world;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

/**
 * Tank — Unidad pesada de combate.
 * Extiende SoldierBasic para reutilizar lógica de daño/HUD.
 */
public class Tank extends SoldierBasic {

    // ---- Stats del tanque ----
    private int   maxHp        = 220;
    private int   hp           = maxHp;
    private float attackRange  = 260f;   // más rango que el soldado
    private float fireInterval = 1.10f;  // dispara más lento
    private float fireTimer    = 0f;

    public Tank(Vector2 pos) {
        super(new Vector2(pos)); // mantiene movimiento de Unit
        // Si tu Unit tiene setSpeed(), podrías bajarla acá (p.ej. 120f).
    }

    // ================== Combate ==================
    @Override
    public void tickAttackTimer(float dt) { fireTimer += dt; }

    @Override
    public boolean canShoot() { return fireTimer >= fireInterval; }

    @Override
    public void onShotFired() { fireTimer = 0f; }

    @Override
    public float getAttackRange() { return attackRange; }

    // ================== Vida / Daño ==================
    @Override
    public boolean takeDamage(int dmg) {
        hp -= Math.max(0, dmg);
        return hp <= 0;
    }

    @Override
    public int getHp() { return hp; }

    @Override
    public int getMaxHp() { return maxHp; }

    // ================== Render ==================
    // Usamos la misma forma de SoldierBasic/Unit (rectángulo), pero
    // conservamos el hook por si luego querés dibujar overlay/sprite propio.
    @Override
    public void render(SpriteBatch batch) {
        super.render(batch);
        // Aquí podrías dibujar un overlay (sprite del tanque) si tenés assets.
    }
}
