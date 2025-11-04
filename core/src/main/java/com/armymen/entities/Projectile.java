package com.armymen.entities;

import com.badlogic.gdx.math.Vector2;

/** Proyectil simple: viaja hacia un objetivo (unidad o edificio) y aplica daño al impactar. */
public class Projectile {
    private final Vector2 pos;
    private final Vector2 vel;       // dirección normalizada * speed
    private final float speed;
    private final float radius = 4f; // radio visual / hitbox simple
    private final float damage;
    private final boolean enemyBullet;

    // objetivo (uno de los dos será no-nulo)
    private Unit targetUnit;
    private Building targetBuilding;

    private boolean alive = true;

    public Projectile(Vector2 from, Vector2 to, float speed, float damage, boolean enemyBullet,
                      Unit targetUnit, Building targetBuilding) {
        this.pos = new Vector2(from);
        Vector2 dir = new Vector2(to).sub(from);
        if (dir.isZero(0.0001f)) dir.set(1, 0);
        dir.nor();
        this.vel = dir;
        this.speed = speed;
        this.damage = damage;
        this.enemyBullet = enemyBullet;
        this.targetUnit = targetUnit;
        this.targetBuilding = targetBuilding;
    }

    /** Avanza el proyectil y chequea impacto. Devuelve true si sigue vivo. */
    public boolean update(float dt) {
        if (!alive) return false;

        // mover
        pos.mulAdd(vel, speed * dt);

        // posición actual del objetivo (si ya murió o se destruyó, el proyectil se descarta)
        Vector2 targetPos = null;
        if (targetUnit != null) {
            if (targetUnit.isDead()) { alive = false; return false; }
            targetPos = targetUnit.getPosition();
        } else if (targetBuilding != null) {
            if (targetBuilding.isDestroyed()) { alive = false; return false; }
            targetPos = targetBuilding.getPosition();
        }

        // impacto (umbral simple)
        if (targetPos != null && pos.dst2(targetPos) <= (radius + 10f) * (radius + 10f)) {
            if (targetUnit != null) targetUnit.damage(damage);
            else if (targetBuilding != null) targetBuilding.damage(damage);
            alive = false;
            return false;
        }

        return true;
    }

    public Vector2 getPos() { return pos; }
    public float getRadius() { return radius; }
    public boolean isEnemyBullet() { return enemyBullet; }
    public boolean isAlive() { return alive; }
}
