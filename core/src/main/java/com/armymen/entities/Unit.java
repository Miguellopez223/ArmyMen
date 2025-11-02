package com.armymen.entities;

import com.armymen.model.Faction;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;

public class Unit {

    // --- Transform / Movimiento ---
    protected final Vector2 position = new Vector2();
    protected final Vector2 target = new Vector2();
    protected boolean hasTarget = false;
    protected float speed = 120f;       // px/seg
    protected float rotationDeg = 0f;   // apunta hacia donde se mueve

    // --- Visual / Picking ---
    protected Texture texture;
    protected float radius = 16f;       // selección/colisión simple
    protected boolean selected = false;

    // --- Juego / Combate ---
    protected Faction faction = Faction.TEAM_A;
    protected int maxHealth = 100;
    protected int health = 100;
    protected int damage = 10;
    protected float attackRange = 120f;
    protected float attackCooldown = 0.6f; // seg
    protected float attackTimer = 0f;

    public Unit(float x, float y) { this.position.set(x, y); }
    public Unit(Vector2 pos) { if (pos != null) this.position.set(pos); }

    public void update(float dt) {
        if (hasTarget) {
            Vector2 dir = new Vector2(target).sub(position);
            float len = dir.len();
            if (len > 1e-3f) {
                dir.scl(1f / len);
                position.mulAdd(dir, speed * dt);
                rotationDeg = dir.angleDeg();

                float stopDist = speed * dt;
                if (position.dst2(target) <= stopDist * stopDist) {
                    position.set(target);
                    hasTarget = false;
                }
            } else {
                hasTarget = false;
            }
        }
        if (attackTimer > 0f) attackTimer -= dt;
    }

    public void draw(Batch batch) {
        if (texture == null) return;
        float w = texture.getWidth(), h = texture.getHeight();
        float ox = w * 0.5f, oy = h * 0.5f;
        batch.draw(texture,
            position.x - ox, position.y - oy,
            ox, oy, w, h,
            1f, 1f, rotationDeg,
            0, 0, (int) w, (int) h,
            false, false
        );
    }

    // --- Picking / selección ---
    public boolean contains(Vector2 p) { return p != null && p.dst2(position) <= radius * radius; }
    public void setSelected(boolean s) { this.selected = s; }
    public boolean isSelected() { return selected; }

    // --- Destino / movimiento ---
    public void setTarget(Vector2 t) { if (t == null) { hasTarget = false; return; } this.target.set(t); hasTarget = true; }
    public Vector2 getTarget() { return hasTarget ? target : null; }

    // --- Getters/Setters transform ---
    public Vector2 getPosition() { return position; }
    public void setPosition(float x, float y) { this.position.set(x, y); }
    public float getRotationDeg() { return rotationDeg; }

    public float getRadius() { return radius; }
    public void setRadius(float r) { this.radius = Math.max(0f, r); }

    public void setSpeed(float s) { this.speed = Math.max(0f, s); }
    public float getSpeed() { return speed; }

    // --- Visual ---
    public void setTexture(Texture t) { this.texture = t; }
    public Texture getTexture() { return texture; }

    // --- Facción / relaciones ---
    public Faction getFaction() { return faction; }
    public void setFaction(Faction f) { if (f != null) this.faction = f; }
    public boolean isEnemy(Unit other) { return other != null && this.faction != null && this.faction != other.faction; }

    // --- Vida / combate ---
    public int getMaxHealth() { return maxHealth; }
    public int getHealth() { return health; }
    public boolean isAlive() { return health > 0; }

    public void healToFull() { this.health = this.maxHealth; }
    public void applyDamage(int amount) {
        if (amount <= 0) return;
        this.health -= amount;
        if (this.health < 0) this.health = 0;
    }

    public int getDamage() { return damage; }
    public void setDamage(int d) { this.damage = Math.max(0, d); }

    public float getAttackRange() { return attackRange; }
    public void setAttackRange(float r) { this.attackRange = Math.max(0f, r); }

    public float getAttackCooldown() { return attackCooldown; }
    public boolean canAttack() { return attackTimer <= 0f; }
    public void resetAttackCooldown() { this.attackTimer = attackCooldown; }
}
