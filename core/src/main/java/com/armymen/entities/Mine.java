package com.armymen.entities;

import com.armymen.model.Faction;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;

/**
 * Mina terrestre: se activa cuando una unidad entra en el radio de activación
 * y aplica daño muy alto (por defecto, efecto "insta-kill").
 * Visualmente es 50% más grande (scale = 1.5) que su sprite original.
 *
 * Integración típica en GameScreen (siguiente paso):
 * - Mantener List<Mine> mines;
 * - En update: por cada unidad viva, llamar mine.tryTrigger(u); si explotó, aplicar daño y marcar para remover.
 * - En draw: mine.draw(batch).
 */
public class Mine {

    // --- Estado/propiedades ---
    private final Vector2 position = new Vector2();
    private Faction owner;           // Puede ser null si es neutral
    private boolean friendlyFire = true; // Si true, daña a todos; si false, solo enemigos del owner

    private boolean armed = true;    // Mientras está armada puede detonar
    private boolean exploded = false;
    private boolean consumed = false; // Para eliminarla del mundo luego de explotar

    // --- Daño y radios ---
    private int damage = 9999;          // "Instakill" por defecto
    private float activationRadius = 22f;
    private float ExplosionRadius = 28f;    // Radio de daño (puede ser igual o mayor al de activación)

    // --- Visual ---
    private Texture texture;            // Opcional (puede ser null si usas shapes)
    private float scale = 1.5f;         // 50% más grande que el sprite
    private float drawOffsetX = 0f;     // Ajustes finos si el arte no está centrado
    private float drawOffsetY = 0f;

    public Mine(float x, float y, Faction owner) {
        this.position.set(x, y);
        this.owner = owner;
    }

    public Mine(Vector2 pos, Faction owner) {
        if (pos != null) this.position.set(pos);
        this.owner = owner;
    }

    // --- Lógica principal ---
    /**
     * Intenta activar la mina frente a una unidad.
     * Retorna true si se activó (explota en este mismo frame).
     */
    public boolean tryTrigger(Unit unit) {
        if (!armed || exploded || unit == null || !unit.isAlive()) return false;

        // Si no hay fuego amigo, solo detona contra enemigos del owner
        if (!friendlyFire && owner != null && !owner.isEnemyOf(unit.getFaction())) {
            return false;
        }

        // Distancia al centro
        if (unit.getPosition().dst2(position) <= activationRadius * activationRadius) {
            // Detona y aplica daño inmediatamente
            exploded = true;
            applyBlastTo(unit);
            return true;
        }
        return false;
    }

    /**
     * Igual que tryTrigger(Unit), pero usando SOLO la posición de la unidad.
     * @param unitPos posición de la unidad a testear
     * @param isEnemyOfOwner true si esa unidad es enemiga del owner de la mina.
     *                       Si usás fuego amigo (friendlyFire=true), este parámetro es irrelevante.
     * @return true si la mina se activó (pone exploded=true en este frame).
     */
    public boolean tryTriggerAt(com.badlogic.gdx.math.Vector2 unitPos, boolean isEnemyOfOwner) {
        if (!armed || exploded || unitPos == null) return false;

        // Si no hay fuego amigo y la mina tiene owner, solo detona contra enemigos.
        if (!friendlyFire && owner != null && !isEnemyOfOwner) {
            return false;
        }

        if (unitPos.dst2(position) <= activationRadius * activationRadius) {
            exploded = true;   // se activa ahora; el daño en área lo aplicaremos desde fuera
            return true;
        }
        return false;
    }

    /**
     * Marca manualmente la mina como consumida una vez que aplicaste el daño por tu cuenta.
     * Útil cuando el daño en área lo hace GameScreen sobre sus propias clases de Unit.
     */
    public void markConsumed() {
        consumed = true;
        armed = false;
    }
    /**
     * Aplica el daño de explosión a una unidad si está dentro del blastRadius.
     * Devuelve true si la alcanzó.
     */
    public boolean applyBlastTo(Unit unit) {
        if (unit == null || !unit.isAlive()) return false;
        if (unit.getPosition().dst2(position) <= ExplosionRadius * ExplosionRadius) {
            unit.applyDamage(damage);
            return true;
        }
        return false;
    }

    /**
     * Utilidad para aplicar el blast a múltiples unidades tras detonar.
     * Devuelve cuántas fueron alcanzadas.
     */
    public int applyBlastTo(Iterable<Unit> units) {
        if (!exploded || units == null) return 0;
        int hits = 0;
        for (Unit u : units) {
            if (applyBlastTo(u)) hits++;
        }
        // La mina se consume tras explotar
        consumed = true;
        armed = false;
        return hits;
    }

    public void update(float dt) {
        // No requiere timers por defecto; si quisieras animaciones/retardo, agrégalo aquí.
        if (exploded) {
            // Podrías agregar una ventana corta para efectos antes de marcar consumed
        }
    }

    public void draw(Batch batch) {
        if (batch == null || texture == null || consumed) return;
        // Dibujo centrado con escala 1.5x (50% más grande)
        float w = texture.getWidth();
        float h = texture.getHeight();
        float ox = (w * scale) * 0.5f;
        float oy = (h * scale) * 0.5f;
        batch.draw(
            texture,
            position.x - ox + drawOffsetX, position.y - oy + drawOffsetY,
            ox, oy,                   // origen (centro)
            w, h,
            scale, scale,             // escala 1.5x
            0f,                       // sin rotación (mina estática)
            0, 0, (int) w, (int) h,
            false, false
        );
    }

    // --- Disparo por buscaminas ---
    /** Permite desarmar la mina (ej.: por la unidad "busca minas"). */
    /** Permite desarmar la mina (ej.: por la unidad "busca minas"). */
    /** Permite desarmar la mina (ej.: por la unidad "busca minas"). */
    public void disarm() {
        this.armed = false;
        this.exploded = false;
        this.consumed = true;  // al desactivar, desaparece del mundo
    }


    public boolean isArmed() { return armed; }

    // --- Getters/Setters útiles ---
    public Vector2 getPosition() { return position; }

    public void setTexture(Texture texture) { this.texture = texture; }
    public Texture getTexture() { return texture; }

    public void setScale(float s) { this.scale = Math.max(0.1f, s); }
    public float getScale() { return scale; }

    public void setActivationRadius(float r) { this.activationRadius = Math.max(1f, r); }
    public float getActivationRadius() { return activationRadius; }

    public void setExplosionRadius(float r) { this.ExplosionRadius = Math.max(1f, r); }
    public float getExplosionRadius() { return ExplosionRadius; }

    public void setDamage(int d) { this.damage = Math.max(0, d); }
    public int getDamage() { return damage; }

    public boolean hasExploded() { return exploded; }
    public boolean isConsumed() { return consumed; }

    public void setFriendlyFire(boolean ff) { this.friendlyFire = ff; }
    public boolean isFriendlyFire() { return friendlyFire; }

    public Faction getOwner() { return owner; }
    public void setOwner(Faction owner) { this.owner = owner; }
}
