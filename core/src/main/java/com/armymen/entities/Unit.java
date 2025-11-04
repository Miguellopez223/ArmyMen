// Unit.java
package com.armymen.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;

public class Unit {

    // --- Posición / movimiento (los enemigos no se moverán; el resto sí) ---
    protected Vector2 position;
    protected Vector2 target;
    protected float speed = 200f;
    protected float radius = 25f;
    protected Texture tex;
    protected String unitTag = "GENERIC";

    // --- Combate sencillo ---
    protected boolean enemy = false;        // true = enemigo (rojo), false = jugador (azul/normal)
    protected boolean stationary = false;   // si true, no se mueve (usado por enemigos)
    public float hp = 100f;
    public float attackDamage = 10f;
    public float attackRange = 200f;
    protected float attackCooldown = 0.6f;
    protected float attackTimer = 0f;


    public Unit(Vector2 startPos) {
        this.position = new Vector2(startPos);
        this.target = new Vector2(startPos);
        this.tex = new Texture("soldier.png");
    }

    public void update(float delta) {
        // Enfriamiento de ataque
        if (attackTimer > 0f) attackTimer -= delta;

        // Si es estacionario, no intenta moverse (enemigos quedan parados)
        if (stationary) return;

        // Movimiento básico
        if (!position.epsilonEquals(target, 1f)) {
            Vector2 dir = new Vector2(target).sub(position);
            float len = dir.len();
            if (len > 1f) {
                dir.nor();
                position.mulAdd(dir, speed * delta);
            } else {
                position.set(target);
            }
        }
    }

    public void render(SpriteBatch batch) {
        if (tex == null) return;

        if (enemy) {
            // TINTA SOLO ESTE DIBUJO
            batch.setColor(1f, 0f, 0f, 1f); // rojo
            batch.draw(tex, position.x - 20, position.y - 20, 40, 40);
            batch.setColor(1f, 1f, 1f, 1f); // VOLVER A BLANCO SIEMPRE
        } else {
            batch.draw(tex, position.x - 20, position.y - 20, 40, 40);
        }
    }


    public boolean contains(Vector2 point) {
        return new Circle(position, radius).contains(point);
    }

    // --- Daño / estado de vida ---
    public void damage(float amount) { hp -= amount; }
    public boolean isDead() { return hp <= 0f; }

    // --- Ataque simple con cooldown (lo invoca GameScreen) ---
    public boolean canAttack() { return attackTimer <= 0f; }
    public void commitAttack() { attackTimer = attackCooldown; }
    public float getAttackRange() { return attackRange; }
    public float getAttackDamage() { return attackDamage; }

    // --- Setters/Getters comunes ---
    public void setTexture(Texture texture) { this.tex = texture; }
    public void setTarget(Vector2 target) { this.target.set(target); }
    public Vector2 getPosition() { return position; }
    public String getTag() { return unitTag; }
    public void setTag(String tag) { this.unitTag = tag; }

    public void setEnemy(boolean enemy) { this.enemy = enemy; }
    public boolean isEnemy() { return enemy; }

    public void setStationary(boolean st) { this.stationary = st; }
    public boolean isStationary() { return stationary; }


}
