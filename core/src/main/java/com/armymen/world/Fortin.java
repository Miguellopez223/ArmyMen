package com.armymen.world;

import com.badlogic.gdx.math.Vector2;

/**
 * Fortín: defensa fija con arco de 180°, que ahora soporta estado de construcción.
 * Mientras está en construcción NO dispara.
 */
public class Fortin {

    // --- Posición / orientación ---
    private final Vector2 position = new Vector2();
    private float facingAngleDeg = 0f; // 0 = +X, antihorario
    private float fovDeg = 180f;

    // --- Combate ---
    private int maxHp = 150;
    private int hp = maxHp;
    private float attackRange = 260f;
    private int damage = 30;

    private float fireCooldown = 0f;
    private float fireDelay = 0.8f; // tiempo entre disparos

    // --- Construcción ---
    private boolean underConstruction = false;
    private float buildTotal = 0f;      // s
    private float buildTimeLeft = 0f;   // s

    public Fortin(Vector2 pos) {
        if (pos != null) this.position.set(pos);
    }

    // =============== Construcción ===============
    /** Inicia la construcción por N segundos. */
    public void startConstruction(float seconds) {
        this.underConstruction = true;
        this.buildTotal = Math.max(0f, seconds);
        this.buildTimeLeft = this.buildTotal;
    }

    /** ¿Está en obra? */
    public boolean isUnderConstruction() { return underConstruction; }

    /** Progreso 0..1 (1 = completado). */
    public float getBuildProgress01() {
        if (!underConstruction || buildTotal <= 0f) return 1f;
        return Math.max(0f, Math.min(1f, 1f - (buildTimeLeft / buildTotal)));
    }

    // =============== Ciclo de vida / combate ===============
    /** Avanza timers; si está en obra, descuenta el tiempo de construcción. */
    public void tick(float dt) {
        // Construcción primero
        if (underConstruction) {
            buildTimeLeft -= dt;
            if (buildTimeLeft <= 0f) {
                buildTimeLeft = 0f;
                underConstruction = false;
            }
        }
        // CD de disparo (igual corre, pero no se permite disparar si underConstruction)
        if (fireCooldown > 0f) fireCooldown -= dt;
    }

    /** Sólo puede disparar si no está en construcción y no está en cooldown. */
    public boolean canShoot() {
        if (underConstruction) return false;
        return fireCooldown <= 0f;
    }

    /** Llamar cuando dispara para reiniciar cooldown. */
    public void onShotFired() {
        fireCooldown = fireDelay;
    }

    /** Aplica daño; devuelve true si muere. */
    public boolean takeDamage(int dmg) {
        hp -= Math.max(0, dmg);
        return hp <= 0;
    }

    // =============== Geometría de tiro ===============
    /** Actualiza el ángulo de mira hacia un objetivo. */
    public void faceTowards(Vector2 targetPos) {
        if (targetPos == null) return;
        float dx = targetPos.x - position.x;
        float dy = targetPos.y - position.y;
        this.facingAngleDeg = (float)Math.toDegrees(Math.atan2(dy, dx));
    }

    /** ¿El punto está dentro del arco de visión actual? */
    public boolean isInArc(Vector2 targetPos) {
        if (targetPos == null) return false;
        // vector al objetivo
        float dx = targetPos.x - position.x;
        float dy = targetPos.y - position.y;
        float ang = (float)Math.toDegrees(Math.atan2(dy, dx));

        float diff = normalizeDeg(ang - facingAngleDeg);
        float half = fovDeg * 0.5f;
        return (diff >= -half && diff <= half);
    }

    private static float normalizeDeg(float a) {
        float r = a % 360f;
        if (r > 180f) r -= 360f;
        if (r < -180f) r += 360f;
        return r;
    }

    // =============== Getters ===============
    public Vector2 getPosition() { return position; }
    public float getFacingAngleDeg() { return facingAngleDeg; }
    public float getFovDeg() { return fovDeg; }
    public float getAttackRange() { return attackRange; }
    public int getDamage() { return damage; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
}
