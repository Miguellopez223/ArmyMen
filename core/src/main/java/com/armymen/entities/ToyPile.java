package com.armymen.entities;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;

/**
 * ToyPile (montón de juguetes de plástico):
 * - Tiene una posición y un stock de plástico disponible.
 * - Un radio lógico para que las volquetas puedan "acercarse" y recolectar.
 * - Métodos para extraer plástico y chequear agotamiento.
 *
 * Render: lo haremos desde GameScreen con ShapeRenderer (círculo/ícono),
 * esta clase es solo lógica.
 */
public class ToyPile {

    private final Vector2 position;
    private final Circle area;   // para "alcance" de recolección
    private int stock;           // plástico disponible (unidades lógicas)

    /**
     * @param pos       posición en el mundo (px)
     * @param radiusPx  radio para considerar que la volqueta puede recolectar (px)
     * @param initialStock unidades de plástico disponibles
     */
    public ToyPile(Vector2 pos, float radiusPx, int initialStock) {
        this.position = new Vector2(pos);
        this.area = new Circle(pos.x, pos.y, radiusPx);
        this.stock = Math.max(0, initialStock);
    }

    /** Extrae hasta 'amount' unidades. Retorna cuánto se logró extraer. */
    public int harvest(int amount) {
        if (amount <= 0 || stock <= 0) return 0;
        int taken = Math.min(stock, amount);
        stock -= taken;
        return taken;
    }

    public boolean isDepleted() { return stock <= 0; }

    // ===== Getters =====
    public Vector2 getPosition() { return position; }
    public Circle  getArea()     { return area; }
    public int     getStock()    { return stock; }

    // ===== Helpers =====
    /** true si el punto (x,y) está dentro del área de recolección */
    public boolean contains(float x, float y) {
        return area.contains(x, y);
    }

    /** Cambia el radio del área (por si luego querés tunearlo desde fuera). */
    public void setRadius(float r) {
        area.setRadius(Math.max(1f, r));
    }
}
