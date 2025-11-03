package com.armymen.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class ToyPile {
    private Vector2 position;
    private int amount;           // plástico disponible en esta pila
    private float radius = 32f;
    private Texture tex;

    public ToyPile(Vector2 position, int initialAmount) {
        this.position = new Vector2(position);
        this.amount = initialAmount;
        // Pon una imagen simple (p.ej. un montoncito). Si no la tienes, crea una cuadrada cualquiera.
        this.tex = new Texture("toy_pile.png");
    }

    public void render(SpriteBatch batch) {
        if (tex != null) {
            batch.draw(tex, position.x - 24, position.y - 24, 48, 48);
        }
    }

    public boolean hasMaterial() { return amount > 0; }

    /** Quita hasta "take" unidades de plástico y retorna cuánto se pudo retirar. */
    public int take(int take) {
        if (take <= 0) return 0;
        int real = Math.min(take, amount);
        amount -= real;
        return real;
    }

    public Vector2 getPosition() { return position; }
    public float getRadius() { return radius; }
    public int getAmount() { return amount; }
}
