package com.armymen.screens;

import com.armymen.MainGame;
import com.armymen.world.Unit;
import com.armymen.world.SoldierBasic;
import com.armymen.world.Building;
import com.armymen.world.Bulldozer;
import com.armymen.world.PlasticTruck;
import com.armymen.world.Tank;
import com.armymen.world.Fortin;
import com.armymen.world.EnemyPatrol;
import com.armymen.world.MineSweeper;
import com.armymen.entities.ToyPile;
import com.armymen.entities.Mine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;


import java.util.ArrayList;
import java.util.List;

/**
 * RTS base + Recolección + Garaje (Volquetas y Tanques) + Fortines + Minas:
 * - Mover: click derecho.
 * - Construcción con Bulldozer: [1]=HQ, [2]=Depósito, [3]=Garaje, [4]=Fortín  +  Shift + click derecho.
 *     • Para HQ/Depósito/Garaje: el bulldozer va al punto y, al llegar, inicia la construcción (5 s).
 *     • Fortín se coloca instantáneo.
 * - HQ produce: [S]=Soldado, [R]=Busca-minas (si el HQ está completado).
 * - GARAGE produce: [V]=Volqueta, [T]=Tanque (si el Garaje está completado).
 * - Volqueta: Ctrl + click derecho sobre un ToyPile para asignar recolección.
 * - Depósito: recién al completarse sirve como ancla de descarga.
 * - Fortín: defensa fija 180°, gira y dispara.
 * - Minas: explotan / se desarman (2s) con overlay de progreso.
 * - Fin de partida: VICTORIA si no quedan enemigos; DERROTA si te quedás sin edificios/fortines.
 *   ENTER para reiniciar.
 */
public class GameScreen implements Screen {

    // ====== Estado del juego ======
    private enum GameState { RUNNING, VICTORY, DEFEAT }
    private GameState gameState = GameState.RUNNING;

    private final MainGame game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shape;
    private BitmapFont font;

    // --- Context UI (panel según selección) ---
    private Building selectedBuildingUI = null;



    // ===== Unidades visibles =====
    private final Array<Unit> playerUnits = new Array<>();
    private final Array<Unit> enemyUnits  = new Array<>();
    private final Array<Unit> selectedUnits = new Array<>();

    // ===== Soldados/Disparadores =====
    private final Array<SoldierBasic> playerSoldiers = new Array<>();
    private final Array<SoldierBasic> enemySoldiers  = new Array<>();

    // ===== Fortines =====
    private final Array<Fortin> playerFortins = new Array<>();

    // ===== Minas =====
    private final Array<Mine> mines = new Array<>();
    private final Array<ExplosionFX> explosions = new Array<>();

    // ===== Bulldozer y Edificios =====
    private final Array<Building> buildings = new Array<>();
    private Bulldozer bulldozer;
    private final Array<Building> enemyBuildings = new Array<>(); // NUEVO: enemigos


    // Tipos lógicos de edificio/colocación
    private enum BType { HQ, STORAGE, GARAGE, FORTIN }
    private static class ProdOrder {
        String kind;     // "soldier" | "sweeper" | "truck" | "tank"
        float timeLeft;  // s
        ProdOrder(String k, float t){ kind = k; timeLeft = t; }
    }
    private static class BData {
        Building b;
        BType type;
        final Array<ProdOrder> queue = new Array<>();
        ProdOrder current = null; // en producción
        BData(Building b, BType t){ this.b = b; this.type = t; }
    }
    private final Array<BData> bdata = new Array<>();
    private BType currentBuildType = BType.HQ;  // teclas 1/2/3/4

    // ===== Parámetros de construcción diferida =====
    private static final float BUILD_TIME = 5.0f;         // segundos
    private static final float BUILD_TIME_FORTIN = 3.0f; // construcción de fortín

    private static final float BUILD_START_RADIUS = 34f;  // distancia para “llegó el bulldozer”

    private static class BuildOrder {
        BType type;
        Vector2 pos = new Vector2();
        int cost;
        BuildOrder(BType t, Vector2 p, int cost){ this.type = t; if (p!=null) this.pos.set(p); this.cost = cost; }
    }
    private BuildOrder pendingBuild = null; // una a la vez (simple)

    // ===== ToyPiles & Volquetas =====
    private final Array<ToyPile> toyPiles = new Array<>();
    private final Array<PlasticTruck> trucks = new Array<>();

    // ===== Selección por arrastre =====
    private boolean selecting = false;
    private Vector2 selectStart = new Vector2();
    private Vector2 selectEnd   = new Vector2();

    // ===== Proyectiles =====
    private final List<Projectile> projectiles = new ArrayList<>();

    // ===== Recursos =====
    private int plastic = 300; // stock inicial

    // Costos (plástico)
    private static final int COST_HQ       = 120;
    private static final int COST_STORAGE  = 80;
    private static final int COST_GARAGE   = 150;
    private static final int COST_SOLDIER  = 50;
    private static final int COST_TRUCK    = 120;
    private static final int COST_TANK     = 200;
    private static final int COST_FORTIN   = 110;
    private static final int COST_SWEEPER  = 90;

    // ===== Cost helper para producción diferida =====
    private int getCostFor(BType type, String kind) {
        if (type == BType.HQ) {
            if ("soldier".equals(kind))  return COST_SOLDIER;
            if ("sweeper".equals(kind))  return COST_SWEEPER;
        } else if (type == BType.GARAGE) {
            if ("truck".equals(kind))    return COST_TRUCK;
            if ("tank".equals(kind))     return COST_TANK;
        }
        return 0;
    }


    // Footprints/colisiones para colocación
    private static final float BUILD_FOOTPRINT = 64f;          // HQ/Storage/Garage usan 64x64
    private static final float FORTIN_FOOTPRINT_RADIUS = 18f;  // radio aprox (hex de ~16)
    private static final float TOYPILE_BLOCK_MARGIN = 8f;      // margen extra contra ToyPiles
    private static final float BUILD_BLOCK_MARGIN   = 4f;      // margen mínimo entre estructuras

    // Producción (tiempos)
    private static final float TIME_SOLDIER = 2.0f; // s por soldado
    private static final float TIME_TRUCK   = 3.5f; // s por volqueta
    private static final float TIME_TANK    = 4.5f; // s por tanque
    private static final float TIME_SWEEPER = 2.5f; // s por busca-minas

    private static final boolean DEBUG_ATTACK_RANGES = true;

    // ---------------- Proyectil ----------------
    private static class Projectile {
        Vector2 pos = new Vector2();
        Vector2 vel = new Vector2();
        float speed = 520f;
        float radius = 4f;
        float ttl = 1.6f;
        boolean fromPlayer;
        int damage = 35;
        Projectile(Vector2 from, Vector2 to, boolean fromPlayer) {
            this.pos.set(from);
            this.vel.set(to).sub(from).nor();
            if (this.vel.isZero(0.0001f)) this.vel.set(1, 0);
            this.fromPlayer = fromPlayer;
        }
        void update(float dt) { pos.mulAdd(vel, speed * dt); ttl -= dt; }
        boolean expired() { return ttl <= 0f; }
    }

    // ---------------- FX de explosión (mina) ----------------
    private static class ExplosionFX {
        final Vector2 pos = new Vector2();
        float ttl = 0.25f, max = 0.25f;
        float radius;
        ExplosionFX(Vector2 p, float r) { if (p != null) pos.set(p); radius = r; }
        void update(float dt){ ttl -= dt; }
        boolean done(){ return ttl <= 0f; }
        float alpha(){ return Math.max(0f, ttl / max); }
    }

    // ---------------- Trabajo de desarme (busca-minas) ----------------
    private static class DisarmJob {
        final Mine mine;
        final MineSweeper sweeper;
        final float total;
        float timeLeft;
        DisarmJob(Mine m, MineSweeper s, float seconds) { mine = m; sweeper = s; total = seconds; timeLeft = seconds; }
    }
    private final Array<DisarmJob> disarmJobs = new Array<>();

    private boolean isBeingDisarmed(Mine m) {
        for (int i = 0; i < disarmJobs.size; i++) if (disarmJobs.get(i).mine == m) return true;
        return false;
    }
    private DisarmJob findDisarmJob(Mine m) {
        for (int i = 0; i < disarmJobs.size; i++) {
            DisarmJob j = disarmJobs.get(i);
            if (j.mine == m) return j;
        }
        return null;
    }

    public GameScreen(MainGame game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.camera.setToOrtho(false, 1280, 720);
        this.batch = new SpriteBatch();
        this.shape = new ShapeRenderer();
        this.font  = new BitmapFont();

        initWorld();
    }

    // ===== Inicialización / Reinicio =====
    private void initWorld() {
        gameState = GameState.RUNNING;

        playerUnits.clear();
        enemyUnits.clear();
        selectedUnits.clear();
        playerSoldiers.clear();
        enemySoldiers.clear();
        playerFortins.clear();
        mines.clear();
        explosions.clear();
        buildings.clear();
        bdata.clear();
        toyPiles.clear();
        trucks.clear();
        projectiles.clear();
        disarmJobs.clear();

        plastic = 300;
        currentBuildType = BType.HQ;
        pendingBuild = null;

        // ===== Aliados =====
        spawnPlayerSoldier(500, 360);
        spawnPlayerSoldier(500, 420);

        // Bulldozer inicial
        bulldozer = new Bulldozer(new Vector2(520, 480));
        playerUnits.add(bulldozer);

        // Edificios iniciales del jugador (HQ + Garaje)
        Building hq0 = new Building(new Vector2(600, 520), "building_storage.png");
        buildings.add(hq0);
        bdata.add(new BData(hq0, BType.HQ));

        Building gar0 = new Building(new Vector2(660, 480), "building_storage.png");
        buildings.add(gar0);
        bdata.add(new BData(gar0, BType.GARAGE));

        // ====== Base enemiga ======
        spawnEnemyBase();


        // ===== Enemigos estáticos =====
        spawnEnemySoldier(880, 360);
        spawnEnemySoldier(880, 420);

        // ===== Patrullas enemigas =====
        spawnEnemyPatrol(
            1000, 440,
            new Vector2(1000, 440),
            new Vector2(1200, 440),
            new Vector2(1200, 600),
            new Vector2(1000, 600)
        );
        spawnEnemyPatrol(
            920, 260,
            new Vector2(920, 260),
            new Vector2(1120, 260),
            new Vector2(1120, 360),
            new Vector2(920, 360)
        );
        spawnEnemyPatrol(
            1060, 420,
            new Vector2(1060, 420),
            new Vector2(1160, 500),
            new Vector2(980, 520)
        );

        // ===== ToyPiles =====
        toyPiles.add(new ToyPile(new Vector2(360, 620), 28f, 600));
        toyPiles.add(new ToyPile(new Vector2(1120, 560), 28f, 600));
        toyPiles.add(new ToyPile(new Vector2(820, 240), 28f, 600));

        // ===== Volqueta inicial =====
        spawnTruck(560, 520);

        // ===== Minas (neutrales / FF ON) =====
        mines.add(new Mine(new Vector2(940, 380), null));
        mines.add(new Mine(new Vector2(1040, 300), null));
        mines.add(new Mine(new Vector2(780, 520), null));
    }

    private void restartGame() {
        initWorld();
    }

    // ---------- Spawns ----------
    private void spawnPlayerSoldier(float x, float y) {
        SoldierBasic s = new SoldierBasic(new Vector2(x, y));
        playerUnits.add(s);
        playerSoldiers.add(s);
    }
    private void spawnEnemySoldier(float x, float y) {
        SoldierBasic s = new SoldierBasic(new Vector2(x, y));
        enemyUnits.add(s);
        enemySoldiers.add(s);
    }
    private void spawnTruck(float x, float y) {
        PlasticTruck t = new PlasticTruck(new Vector2(x, y));
        Vector2 anchor = getStorageAnchorPoint(); // solo depósitos completados
        if (anchor != null) t.setStorageAnchor(anchor);
        t.setResourceSink(amount -> plastic += amount);   // sumar al HUD
        playerUnits.add(t);
        trucks.add(t);
    }
    private void spawnTank(float x, float y) {
        Tank t = new Tank(new Vector2(x, y));
        playerUnits.add(t);
        playerSoldiers.add(t); // dispara como soldado pero con stats de tanque
    }
    private void spawnMineSweeper(float x, float y) {
        MineSweeper ms = new MineSweeper(new Vector2(x, y));
        playerUnits.add(ms); // no va a playerSoldiers (no dispara)
    }
    // Crea un soldado enemigo que patrulla en bucle los waypoints dados.
    private void spawnEnemyPatrol(float startX, float startY, Vector2... waypoints) {
        com.badlogic.gdx.utils.Array<Vector2> path = new com.badlogic.gdx.utils.Array<>();
        for (Vector2 v : waypoints) path.add(new Vector2(v)); // copia defensiva
        EnemyPatrol p = new EnemyPatrol(new Vector2(startX, startY), path);
        enemyUnits.add(p);
        enemySoldiers.add(p); // dispara como enemigo estándar
    }
    // Base inicial del enemigo: HQ + Garaje (sin obra)
    private void spawnEnemyBase() {
        // Usamos las mismas texturas placeholder que venís usando
        Building enemyHq  = new Building(new Vector2(1100, 520), "building_storage.png");
        Building enemyGar = new Building(new Vector2(1160, 480), "building_storage.png");

        enemyBuildings.add(enemyHq);
        enemyBuildings.add(enemyGar);
    }

    // ===================== RENDER =====================
    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        // Edificios
        for (Building b : buildings) b.render(batch);
        for (Building b : enemyBuildings) b.render(batch);

        // Unidades
        for (Unit u : playerUnits)   u.render(batch);
        for (Unit u : enemyUnits)    u.render(batch);
        // HUD
        drawHUD();
        batch.end();

        // ToyPiles
        drawToyPiles();

        // Minas + FX
        drawMinesAndExplosions();

        // Fortines: visual (hexágono, rango y arco)
        drawFortinsVisual();

        // Barras de vida
        drawHealthBars();

        drawBuildingHealthBars();


        // Proyectiles
        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (Projectile p : projectiles) {
            shape.setColor(p.fromPlayer ? Color.FOREST : Color.GOLD);
            shape.circle(p.pos.x, p.pos.y, p.radius);
        }
        shape.end();

        // Rangos (debug) de soldados/tanques y radio de MineSweeper
        if (DEBUG_ATTACK_RANGES) {
            shape.setProjectionMatrix(camera.combined);
            shape.begin(ShapeRenderer.ShapeType.Line);
            shape.setColor(new Color(0f, 0.5f, 0f, 0.6f)); // aliados
            for (SoldierBasic s : playerSoldiers)
                shape.circle(s.getPosition().x, s.getPosition().y, s.getAttackRange());
            shape.setColor(new Color(1f, 0.5f, 0f, 0.6f)); // enemigos
            for (SoldierBasic s : enemySoldiers)
                shape.circle(s.getPosition().x, s.getPosition().y, s.getAttackRange());
            // Radio de desarme de MineSweeper
            shape.setColor(new Color(0.2f, 0.2f, 0.2f, 0.6f));
            for (int i = 0; i < playerUnits.size; i++) {
                Unit u = playerUnits.get(i);
                if (u instanceof MineSweeper) {
                    MineSweeper ms = (MineSweeper) u;
                    shape.circle(ms.getPosition().x, ms.getPosition().y, ms.getDisarmRadius());
                }
            }
            shape.end();
        }

        // Selección
        if (selecting) {
            shape.setProjectionMatrix(camera.combined);
            shape.begin(ShapeRenderer.ShapeType.Line);
            shape.setColor(Color.BLUE);
            Rectangle rect = getSelectionRectangle();
            shape.rect(rect.x, rect.y, rect.width, rect.height);
            shape.end();
        }

        // Aro rojo a seleccionados
        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.RED);
        for (Unit u : selectedUnits)
            shape.circle(u.getPosition().x, u.getPosition().y, 25);
        shape.end();

        // Progreso sobre HQ y Garaje (si no están en obra)
        drawProductionOverBuildings();

        // Carga sobre volquetas
        drawTruckCargoBars();

        drawContextPanel();


        // Overlay de Victoria/Derrota
        if (gameState != GameState.RUNNING) drawGameOverOverlay();

        // “Blueprint” del pedido de obra (fantasma simple)
        drawPendingBuildGhost();
    }

    // ===================== HUD =====================
    private void drawHUD() {
        String buildStr =
            (currentBuildType == BType.HQ)      ? "HQ(1)" :
                (currentBuildType == BType.STORAGE) ? "Depósito(2)" :
                    (currentBuildType == BType.GARAGE)  ? "Garaje(3)" :
                        "Fortín(4)";
        String txt = "Plástico: " + plastic +
            "    Construir: " + buildStr +
            "   [Shift+Click derecho con Bulldozer]\n" +
            "HQ: [S] Soldado (+"+COST_SOLDIER+"), [R] Busca-minas (+"+COST_SWEEPER+")   |   "
            + "GARAGE: [V] Volqueta (+"+COST_TRUCK+"), [T] Tanque (+"+COST_TANK+")   |   "
            + "FORTÍN: [4] y Shift+Click p/ colocar (+"+COST_FORTIN+")\n" +
            "Volqueta: Ctrl + Click derecho sobre un ToyPile para asignar";

        // aviso si no hay depósito
        if (!hasStorage()) {
            txt += "\n⚠ Construí un Depósito (2 + Shift+Click) para que las volquetas descarguen. Sin depósito, quedan con la carga.";
        }
        // aviso si hay obra pendiente
        if (pendingBuild != null) {
            txt += "\n🛠 Obra pendiente: " + pendingBuild.type + " en (" +
                (int)pendingBuild.pos.x + "," + (int)pendingBuild.pos.y + ").";
        }

        font.setColor(0,0,0,1);
        font.draw(batch, txt, camera.position.x - camera.viewportWidth/2f + 10,
            camera.position.y + camera.viewportHeight/2f - 10);
    }
    private void drawContextPanel() {
        // No panel si el juego no está corriendo (dejá limpio tu overlay de victoria/derrota)
        if (gameState != GameState.RUNNING) return;

        boolean show = false;
        float sx = camera.position.x - camera.viewportWidth / 2f + 12f;  // anclado a esquina inferior izq. de la pantalla
        float sy = camera.position.y - camera.viewportHeight / 2f + 12f;
        float W = 360f, H = 140f;
        com.badlogic.gdx.utils.Array<String> lines = new com.badlogic.gdx.utils.Array<>();

        if (selectedUnits.size == 1) {
            show = true;
            Unit u = selectedUnits.first();

            if (u instanceof Bulldozer) {
                lines.add("Bulldozer — constructor");
                lines.add("• Construir: [1] HQ  [2] Depósito  [3] Garaje  [4] Fortín");
                lines.add("• Colocar: Shift + Click derecho");
                if (pendingBuild != null) {
                    lines.add("• Obra pendiente en (" + (int)pendingBuild.pos.x + "," + (int)pendingBuild.pos.y + ")");
                }
            } else if (u instanceof PlasticTruck) {
                PlasticTruck t = (PlasticTruck) u;
                lines.add("Volqueta — recolección");
                lines.add("• Asignar pila: Ctrl + Click derecho sobre ToyPile");
                lines.add("• Capacidad: " + t.getCargo() + "/" + t.getCapacity());
                lines.add(hasStorage() ? "• Descarga en Depósito (auto)" : "• Sin Depósito: queda cargada");
            } else if (u instanceof MineSweeper) {
                MineSweeper ms = (MineSweeper) u;
                lines.add("Busca-minas");
                lines.add("• Desarme automático: " + (ms.isAutoDisarm() ? "ON" : "OFF") + " (toggle [X])");
                lines.add("• Radio de desarme ≈ " + (int)ms.getDisarmRadius());
            } else if (u instanceof Tank) {
                lines.add("Tanque");
                lines.add("• Mover: Click derecho");
                lines.add("• Fuego automático a enemigos/edificios en rango");
            } else if (u instanceof SoldierBasic) {
                lines.add("Soldado");
                lines.add("• Mover: Click derecho");
                lines.add("• Fuego automático a enemigos/edificios en rango");
            } else {
                lines.add("Unidad");
                lines.add("• Mover: Click derecho");
            }

        } else if (selectedUnits.size > 1) {
            show = true;
            lines.add("Grupo (" + selectedUnits.size + ")");
            lines.add("• Mover en formación: Click derecho");

        } else if (selectedBuildingUI != null) {
            show = true;
            BData bd = getBDataFor(selectedBuildingUI);
            String tipo = "Edificio";
            if (bd != null) {
                if (bd.type == BType.HQ) tipo = "Cuartel General";
                else if (bd.type == BType.GARAGE) tipo = "Garaje";
                else if (bd.type == BType.STORAGE) tipo = "Depósito";
            }
            lines.add(tipo);

            if (bd == null) {
                lines.add("• (Edificio enemigo o sin datos)");
            } else if (bd.type == BType.HQ) {
                lines.add("• Producir: [S] Soldado ("+COST_SOLDIER+")  [R] Busca-minas ("+COST_SWEEPER+")");
                lines.add("• Cola: " + bd.queue.size + (bd.current != null ? " (1 en curso)" : ""));
            } else if (bd.type == BType.GARAGE) {
                lines.add("• Producir: [V] Volqueta ("+COST_TRUCK+")  [T] Tanque ("+COST_TANK+")");
                lines.add("• Cola: " + bd.queue.size + (bd.current != null ? " (1 en curso)" : ""));
            } else if (bd.type == BType.STORAGE) {
                lines.add("• Ancla de descarga de volquetas.");
            }
        }

        if (!show) return;

        // Alto dinámico según cantidad de líneas
        H = 28f + lines.size * 18f + 10f;

        // Fondo
        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(new Color(0f, 0f, 0f, 0.25f));
        shape.rect(sx, sy, W, H);
        shape.end();

        // Borde
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(new Color(0f, 0f, 0f, 0.8f));
        shape.rect(sx, sy, W, H);
        shape.end();

        // Texto
        batch.begin();
        font.setColor(Color.WHITE);
        float tx = sx + 12f;
        float ty = sy + H - 12f;
        for (int i = 0; i < lines.size; i++) {
            font.draw(batch, lines.get(i), tx, ty - i * 18f);
        }
        batch.end();
    }


    // ===================== ToyPiles =====================
    private void drawToyPiles() {
        shape.setProjectionMatrix(camera.combined);

        // círculos
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (ToyPile p : toyPiles) {
            shape.setColor(new Color(0.93f, 0.62f, 0.12f, 1f)); // naranja
            shape.circle(p.getPosition().x, p.getPosition().y, 12f);
        }
        shape.end();

        // radio/área
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(new Color(0.93f, 0.62f, 0.12f, 0.4f));
        for (ToyPile p : toyPiles) {
            shape.circle(p.getPosition().x, p.getPosition().y, p.getArea().radius);
        }
        shape.end();

        // texto de stock
        batch.begin();
        font.setColor(0,0,0,1);
        for (ToyPile p : toyPiles) {
            font.draw(batch, String.valueOf(p.getStock()),
                p.getPosition().x, p.getPosition().y + 24f, 0, Align.center, false);
        }
        batch.end();
    }

    // ===================== MINAS (dibujar) =====================
    private void drawMinesAndExplosions() {
        shape.setProjectionMatrix(camera.combined);

        // Minas activas: cuerpo negro + overlay gris si se está desarmando
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < mines.size; i++) {
            Mine m = mines.get(i);
            if (m.isConsumed()) continue;

            if (!m.hasExploded()) {
                shape.setColor(Color.BLACK);
                shape.circle(m.getPosition().x, m.getPosition().y, 10f);

                DisarmJob job = findDisarmJob(m);
                if (job != null) {
                    shape.setColor(new Color(0.6f, 0.6f, 0.6f, 0.35f));
                    shape.circle(m.getPosition().x, m.getPosition().y, 14f);
                }
            }
        }
        shape.end();

        // Aro de activación y progreso de desarme (arco)
        shape.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < mines.size; i++) {
            Mine m = mines.get(i);
            if (m.isConsumed() || m.hasExploded()) continue;

            shape.setColor(new Color(0f, 0f, 0f, 0.25f));
            shape.circle(m.getPosition().x, m.getPosition().y, m.getActivationRadius());

            DisarmJob job = findDisarmJob(m);
            if (job != null && job.total > 0f) {
                float progress = Math.max(0f, Math.min(1f, 1f - (job.timeLeft / job.total)));
                float rArc = m.getActivationRadius() + 4f;
                shape.setColor(new Color(0.15f, 0.7f, 0.25f, 0.9f));
                shape.arc(m.getPosition().x, m.getPosition().y, rArc, 90f, -360f * progress);
            }
        }
        shape.end();

        // Onda de explosión (FX)
        shape.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < explosions.size; i++) {
            ExplosionFX fx = explosions.get(i);
            float a = fx.alpha();
            shape.setColor(1f, 0.2f, 0f, Math.max(0.05f, a));
            float r = fx.radius * (1f + (1f - a) * 0.6f);
            shape.circle(fx.pos.x, fx.pos.y, r);
        }
        shape.end();
    }

    // ===================== HP BARS =====================
    private void drawHealthBars() {
        float W = 36f, H = 6f, YOFF = 28f;

        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (SoldierBasic s : playerSoldiers) drawOneHpBar(s, W, H, YOFF);
        for (SoldierBasic s : enemySoldiers)  drawOneHpBar(s, W, H, YOFF);
        // ---- HP del Bulldozer (jugador) ----
        if (bulldozer != null) {
            Vector2 pz = bulldozer.getPosition();
            float xz = pz.x - W/2f, yz = pz.y + YOFF;
            float ratioZ = Math.max(0f, Math.min(1f, (float)bulldozer.getHp() / (float)bulldozer.getMaxHp()));
            // fondo
            shape.setColor(new Color(0f, 0f, 0f, 0.25f));
            shape.rect(xz, yz, W, H);
            // barra (celeste para distinguir)
            shape.setColor(new Color(0.1f, 0.6f, 0.9f, 0.95f));
            shape.rect(xz, yz, W * ratioZ, H);
        }
        // ---- HP del Mine Sweeper (jugador) ----
        for (int i = 0; i < playerUnits.size; i++) {
            Unit u = playerUnits.get(i);
            if (u instanceof MineSweeper) {
                MineSweeper ms = (MineSweeper) u;
                Vector2 p = ms.getPosition();
                float x = p.x - W/2f, y = p.y + YOFF;
                float ratio = Math.max(0f, Math.min(1f, (float)ms.getHp() / (float)ms.getMaxHp()));
                // fondo
                shape.setColor(new Color(0f, 0f, 0f, 0.25f));
                shape.rect(x, y, W, H);
                // barra (cian verdoso para distinguirlo)
                shape.setColor(new Color(0.2f, 0.8f, 0.8f, 0.95f));
                shape.rect(x, y, W * ratio, H);
            }
        }


        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.BLACK);
        for (SoldierBasic s : playerSoldiers) {
            Vector2 p = s.getPosition();
            shape.rect(p.x - W/2f, p.y + YOFF, W, H);
        }
        for (SoldierBasic s : enemySoldiers) {
            Vector2 p = s.getPosition();
            shape.rect(p.x - W/2f, p.y + YOFF, W, H);
        }
        // contorno barra del Bulldozer
        if (bulldozer != null) {
            Vector2 pz = bulldozer.getPosition();
            shape.rect(pz.x - W/2f, pz.y + YOFF, W, H);
        }
        // contorno barra del Mine Sweeper
        for (int i = 0; i < playerUnits.size; i++) {
            Unit u = playerUnits.get(i);
            if (u instanceof MineSweeper) {
                Vector2 p = u.getPosition();
                shape.rect(p.x - W/2f, p.y + YOFF, W, H);
            }
        }


        shape.end();
    }
    private void drawOneHpBar(SoldierBasic s, float W, float H, float YOFF) {
        Vector2 p = s.getPosition();
        float x = p.x - W/2f, y = p.y + YOFF;
        float ratio = Math.max(0f, Math.min(1f, (float)s.getHp() / (float)s.getMaxHp()));
        shape.setColor(new Color(0.7f, 0f, 0f, 0.8f));
        shape.rect(x, y, W, H);
        shape.setColor(new Color(0f, 0.7f, 0f, 0.9f));
        shape.rect(x, y, W * ratio, H);
    }

    // Indicador simple de carga sobre volquetas
    private void drawTruckCargoBars() {
        float W = 30f, H = 5f, Y = 22f;

        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (PlasticTruck t : trucks) {
            Vector2 p = t.getPosition();
            shape.setColor(new Color(0.2f,0.2f,0.2f,0.5f));
            shape.rect(p.x - W/2f, p.y + Y, W, H);
            float ratio = (float)t.getCargo() / (float)t.getCapacity();
            shape.setColor(new Color(0.15f,0.5f,0.9f,0.9f));
            shape.rect(p.x - W/2f, p.y + Y, W * ratio, H);
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.BLACK);
        for (PlasticTruck t : trucks) {
            Vector2 p = t.getPosition();
            shape.rect(p.x - W/2f, p.y + Y, W, H);
        }
        shape.end();
    }

    // ===================== UPDATE =====================
    private void update(float delta) {
        // Si terminó la partida, sólo escuchamos ENTER para reiniciar
        if (gameState != GameState.RUNNING) {
            handleCamera(delta);
            handleGameOverInput();  // ENTER para reiniciar
            return;
        }



        handleCamera(delta);
        handleInput();

        for (Unit u : playerUnits) u.update(delta);
        for (Unit u : enemyUnits)  u.update(delta);

        // Edificios: avanzar construcción
        for (int i = 0; i < buildings.size; i++) {
            buildings.get(i).update(delta);
        }

        // Si hay un pedido de obra, verificar llegada del bulldozer y arrancar construcción
        updateBulldozerBuild();

        // Primero: busca-minas desactivan si están cerca
        updateMineSweepers();
        updateDisarmJobs(delta);

        // Minas
        updateMines(delta);

        // Disparo PASIVO (soldados y tanques)
        passiveShoot(playerSoldiers, enemyUnits, enemyBuildings, true);   // tus soldados atacan unidades o edificios enemigos
        passiveShoot(enemySoldiers,  playerUnits, buildings,      false); // enemigos atacan tus unidades o edificios


        // Fortines
        updateFortins(delta);

        // Producción en HQ y GARAGE (sólo si el edificio está completado)
        updateProduction(delta);

        // Proyectiles
        updateProjectiles(delta);

        // Edificios (timers de construcción, etc.)
        for (int i = 0; i < buildings.size; i++)      buildings.get(i).update(delta);
        for (int i = 0; i < enemyBuildings.size; i++) enemyBuildings.get(i).update(delta);


        // FX de explosión (decay)
        for (int i = 0; i < explosions.size; ) {
            ExplosionFX fx = explosions.get(i);
            fx.update(delta);
            if (fx.done()) explosions.removeIndex(i);
            else i++;
        }

        // Si aparece un Depósito completado, refrescar anclas
        updateAllTruckStorageAnchors();

        // Chequear fin de partida
        checkGameOver();
    }

    private void handleGameOverInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            restartGame();
        }
    }

    // --- Disparo pasivo: primero unidades; si no hay, edificios ---
    private void passiveShoot(Array<SoldierBasic> shooters,
                              Array<Unit> victimsSide,
                              Array<Building> victimBuildings,
                              boolean fromPlayerSide) {
        float dt = Gdx.graphics.getDeltaTime();
        for (int i = 0; i < shooters.size; i++) {
            SoldierBasic s = shooters.get(i);
            if (s == null) continue;

            s.tickAttackTimer(dt);

            // 1) intentar con unidades
            Unit targetUnit = findNearestInRange(s, victimsSide, s.getAttackRange());
            Vector2 targetPos = null;

            // 2) si no hay unidades en rango, intentar edificio
            if (targetUnit != null) {
                targetPos = targetUnit.getPosition();
            } else {
                Building tb = findNearestBuildingInRange(s.getPosition(), victimBuildings, s.getAttackRange());
                if (tb != null) targetPos = tb.getPosition();
            }

            if (targetPos != null && s.canShoot()) {
                Projectile proj = new Projectile(s.getPosition(), targetPos, fromPlayerSide);
                if (s instanceof Tank) { // buff tanque
                    proj.damage = 75;
                    proj.radius = 6f;
                }
                projectiles.add(proj);
                s.onShotFired();
            }
        }
    }
    private Building findNearestBuildingInRange(Vector2 fromPos, Array<Building> candidates, float range) {
        if (candidates == null || fromPos == null) return null;
        float bestD2 = range * range;
        Building best = null;
        for (int i = 0; i < candidates.size; i++) {
            Building b = candidates.get(i);
            if (b == null) continue;
            float d2 = fromPos.dst2(b.getPosition());
            if (d2 <= bestD2) { bestD2 = d2; best = b; }
        }
        return best;
    }


    // ===================== MINAS (lógica) =====================
    private void updateMines(float dt) {
        // Intentar detonar minas con cualquier unidad cercana
        for (int i = 0; i < mines.size; ) {
            Mine m = mines.get(i);

            if (m.isConsumed()) {
                mines.removeIndex(i);
                continue;
            }

            // Mientras está siendo desarmada, no puede detonar
            if (isBeingDisarmed(m)) { i++; continue; }

            if (!m.hasExploded()) {
                boolean triggered = false;

                // probar con unidades aliadas
                for (int j = 0; j < playerUnits.size && !triggered; j++) {
                    Unit u = playerUnits.get(j);
                    if (u == null) continue;
                    if (m.tryTriggerAt(u.getPosition(), true)) { // FF ON: "true" da igual
                        triggered = true;
                    }
                }
                // probar con unidades enemigas
                for (int j = 0; j < enemyUnits.size && !triggered; j++) {
                    Unit u = enemyUnits.get(j);
                    if (u == null) continue;
                    if (m.tryTriggerAt(u.getPosition(), true)) {
                        triggered = true;
                    }
                }

                if (triggered) {
                    // aplicar daño en área
                    applyMineBlastToUnits(m, playerUnits, true);
                    applyMineBlastToUnits(m, enemyUnits,  false);
                    applyMineBlastToFortins(m);

                    // onda/FX
                    explosions.add(new ExplosionFX(new Vector2(m.getPosition()), m.getExplosionRadius()));

                    // consumimos la mina (no se vuelve a usar)
                    m.markConsumed();
                }
            }

            i++;
        }
    }

    private void applyMineBlastToUnits(Mine m, Array<Unit> units, boolean arePlayers) {
        float r2 = m.getExplosionRadius() * m.getExplosionRadius();
        for (int i = 0; i < units.size; ) {
            Unit u = units.get(i);
            if (u == null) { i++; continue; }
            if (u.getPosition().dst2(m.getPosition()) <= r2) {
                if (u instanceof SoldierBasic) {
                    SoldierBasic sb = (SoldierBasic) u;
                    boolean dead = sb.takeDamage(m.getDamage());
                    if (dead) {
                        units.removeIndex(i);
                        if (arePlayers) playerSoldiers.removeValue(sb, true);
                        else            enemySoldiers.removeValue(sb, true);
                        selectedUnits.removeValue(sb, true);
                        if (!arePlayers) plastic += 15; // recompensa simple por matar enemigos
                        continue;
                    }
                }else if (u instanceof Bulldozer) {
                    Bulldozer bz = (Bulldozer) u;
                    boolean dead = bz.takeDamage(m.getDamage());
                    if (dead) {
                        units.removeIndex(i);
                        selectedUnits.removeValue(bz, true);
                        if (bz == bulldozer) bulldozer = null; // si era tu bulldozer
                        continue;
                    }
                }else if (u instanceof MineSweeper) {
                    MineSweeper ms = (MineSweeper) u;
                    boolean dead = ms.takeDamage(m.getDamage());
                    if (dead) {
                        units.removeIndex(i);
                        selectedUnits.removeValue(ms, true);
                        continue; // importante para no incrementar i sobre un índice ya removido
                    }
                }


            }
            i++;
        }
    }

    private void applyMineBlastToFortins(Mine m) {
        float r2 = m.getExplosionRadius() * m.getExplosionRadius();
        for (int i = 0; i < playerFortins.size; ) {
            Fortin f = playerFortins.get(i);
            if (f.getPosition().dst2(m.getPosition()) <= r2) {
                boolean dead = f.takeDamage(m.getDamage());
                if (dead) { playerFortins.removeIndex(i); continue; }
            }
            i++;
        }
    }

    private void updateMineSweepers() {
        final float DISARM_SECONDS = 2.0f;

        for (int i = 0; i < playerUnits.size; i++) {
            Unit u = playerUnits.get(i);
            if (!(u instanceof MineSweeper)) continue;
            MineSweeper ms = (MineSweeper) u;
            if (!ms.isAutoDisarm()) continue;

            for (int m = 0; m < mines.size; m++) {
                Mine mine = mines.get(m);
                if (mine.isConsumed() || !mine.isArmed()) continue;
                if (isBeingDisarmed(mine)) continue;

                if (ms.getPosition().dst2(mine.getPosition()) <= ms.getDisarmRadius() * ms.getDisarmRadius()) {
                    disarmJobs.add(new DisarmJob(mine, ms, DISARM_SECONDS));
                }
            }
        }
    }

    private void updateDisarmJobs(float dt) {
        for (int i = 0; i < disarmJobs.size; ) {
            DisarmJob job = disarmJobs.get(i);

            if (job.mine.isConsumed() || job.mine.hasExploded()) {
                disarmJobs.removeIndex(i);
                continue;
            }
            if (job.sweeper.getPosition().dst2(job.mine.getPosition()) >
                job.sweeper.getDisarmRadius() * job.sweeper.getDisarmRadius()) {
                disarmJobs.removeIndex(i);
                continue;
            }

            job.timeLeft -= dt;
            if (job.timeLeft <= 0f) {
                job.mine.disarm();
                disarmJobs.removeIndex(i);
                continue;
            }

            i++;
        }
    }

    // ===================== FORTINES =====================
    private void updateFortins(float dt) {
        for (int i = 0; i < playerFortins.size; i++) {
            Fortin f = playerFortins.get(i);
            f.tick(dt);

            Unit t = findNearestInRangeFromPos(f.getPosition(), enemyUnits, f.getAttackRange());
            if (t != null) {
                f.faceTowards(t.getPosition());
                if (f.isInArc(t.getPosition()) && f.canShoot()) {
                    Projectile p = new Projectile(f.getPosition(), t.getPosition(), true);
                    p.damage = f.getDamage();
                    p.radius = 5f;
                    projectiles.add(p);
                    f.onShotFired();
                }
            }
        }
    }

    private void drawFortinsVisual() {
        shape.setProjectionMatrix(camera.combined);

        // Rango + arco (líneas)
        shape.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < playerFortins.size; i++) {
            Fortin f = playerFortins.get(i);
            Vector2 c = f.getPosition();
            float r = f.getAttackRange();

            // círculo de rango
            shape.setColor(new Color(0.1f, 0.3f, 0.7f, 0.4f));
            shape.circle(c.x, c.y, r);

            // arco de visión (180° por defecto)
            float start = f.getFacingAngleDeg() - f.getFovDeg() * 0.5f;
            shape.setColor(new Color(0.1f, 0.3f, 0.7f, 0.9f));
            shape.arc(c.x, c.y, r, start, f.getFovDeg());

            // líneas de borde del arco
            Vector2 a = dirDeg(start).scl(r).add(c);
            Vector2 b = dirDeg(start + f.getFovDeg()).scl(r).add(c);
            shape.line(c.x, c.y, a.x, a.y);
            shape.line(c.x, c.y, b.x, b.y);

            // facing line cortita
            Vector2 tip = dirDeg(f.getFacingAngleDeg()).scl(20f).add(c);
            shape.setColor(new Color(0.1f, 0.3f, 0.7f, 1f));
            shape.line(c.x, c.y, tip.x, tip.y);
        }
        shape.end();

        // Hexágono del fortín (cuerpo)
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(new Color(0.05f,0.05f,0.05f,1f));
        for (int i = 0; i < playerFortins.size; i++) {
            drawHex(playerFortins.get(i).getPosition(), 16f);
        }
        shape.end();

        // Barra de construcción (verde) sobre el fortín si está en obra
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < playerFortins.size; i++) {
            Fortin f = playerFortins.get(i);
            if (!f.isUnderConstruction()) continue;

            float prog = f.getBuildProgress01(); // 0..1
            float W = 38f, H = 4f;
            float x = f.getPosition().x - W / 2f;
            float y = f.getPosition().y + 22f; // un poco arriba del hexágono

            // fondo gris tenue
            shape.setColor(new Color(0f, 0f, 0f, 0.25f));
            shape.rect(x, y, W, H);

            // progreso verde
            shape.setColor(new Color(0.1f, 0.8f, 0.2f, 0.95f));
            shape.rect(x, y, W * prog, H);
        }
        shape.end();
    }


    private static Vector2 dirDeg(float deg) {
        float rad = (float) Math.toRadians(deg);
        return new Vector2((float)Math.cos(rad), (float)Math.sin(rad));
    }

    private void drawHex(Vector2 center, float r) {
        float angStep = 60f;
        Vector2 prev = null;
        for (int i = 0; i <= 6; i++) {
            float ang = (i * angStep);
            float rad = (float) Math.toRadians(ang);
            Vector2 p = new Vector2(center.x + r * (float)Math.cos(rad),
                center.y + r * (float)Math.sin(rad));
            if (prev != null) shape.line(prev.x, prev.y, p.x, p.y);
            prev = p;
        }
    }

    // ===================== PRODUCCIÓN (HQ + GARAGE) =====================
    private void updateProduction(float dt) {
        for (int i = 0; i < bdata.size; i++) {
            BData d = bdata.get(i);

            // si el edificio aún está en obra, NO producir ni avanzar
            if (d.b.isUnderConstruction()) continue;

            // Si no hay algo en curso, intentamos iniciar el próximo pedido
            if (d.current == null && d.queue.size > 0) {
                ProdOrder next = d.queue.first(); // no lo sacamos aún
                int cost = getCostFor(d.type, next.kind);

                // ¿hay plástico suficiente? Si sí, cobramos y arrancamos; si no, esperamos.
                if (plastic >= cost) {
                    plastic -= cost;
                    d.current = d.queue.removeIndex(0);
                }
            }

            // Avanzar lo que esté en curso
            if (d.current != null) {
                d.current.timeLeft -= dt;
                if (d.current.timeLeft <= 0f) {
                    if (d.type == BType.HQ && "soldier".equals(d.current.kind)) {
                        Vector2 spawn = new Vector2(d.b.getPosition()).add(40, 0);
                        spawnPlayerSoldier(spawn.x, spawn.y);
                    } else if (d.type == BType.HQ && "sweeper".equals(d.current.kind)) {
                        Vector2 spawn = new Vector2(d.b.getPosition()).add(40, -30);
                        spawnMineSweeper(spawn.x, spawn.y);
                    } else if (d.type == BType.GARAGE && "truck".equals(d.current.kind)) {
                        Vector2 spawn = new Vector2(d.b.getPosition()).add(46, 0);
                        spawnTruck(spawn.x, spawn.y);
                    } else if (d.type == BType.GARAGE && "tank".equals(d.current.kind)) {
                        Vector2 spawn = new Vector2(d.b.getPosition()).add(52, 0);
                        spawnTank(spawn.x, spawn.y);
                    }
                    d.current = null; // listo para intentar otro en la próxima iteración
                }
            }
        }
    }


    private void drawProductionOverBuildings() {
        shape.setProjectionMatrix(camera.combined);
        for (int i = 0; i < bdata.size; i++) {
            BData d = bdata.get(i);
            if (d.type != BType.HQ && d.type != BType.GARAGE) continue;

            Vector2 p = d.b.getPosition();
            float W = 44f, H = 6f, Y = 36f;

            // Si el edificio está en obra, dibujar solo una franja gris y la etiqueta “OBRA”
            if (d.b.isUnderConstruction()) {
                // fondo gris obra
                shape.begin(ShapeRenderer.ShapeType.Filled);
                shape.setColor(new Color(0.5f,0.5f,0.5f,0.35f));
                shape.rect(p.x - W/2f, p.y + Y, W, H);
                shape.end();

                shape.begin(ShapeRenderer.ShapeType.Line);
                shape.setColor(Color.DARK_GRAY);
                shape.rect(p.x - W/2f, p.y + Y, W, H);
                shape.end();

                batch.begin();
                font.setColor(0,0,0,1);
                font.draw(batch, "OBRA", p.x, p.y + Y + 16f, 0, Align.center, false);
                batch.end();
                continue;
            }

            // fondo
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(new Color(0.2f,0.2f,0.2f,0.4f));
            shape.rect(p.x - W/2f, p.y + Y, W, H);

            // progreso
            float baseTime;
            if (d.type == BType.HQ) {
                if (d.current != null && "sweeper".equals(d.current.kind)) baseTime = TIME_SWEEPER;
                else baseTime = TIME_SOLDIER;
            } else { // GARAGE
                if (d.current != null && "truck".equals(d.current.kind)) baseTime = TIME_TRUCK;
                else baseTime = TIME_TANK;
            }
            float ratio = 0f;
            if (d.current != null && d.current.timeLeft > 0f) {
                ratio = Math.max(0f, Math.min(1f, 1f - (d.current.timeLeft / baseTime)));
            }
            shape.setColor(d.type == BType.HQ ? new Color(0.1f,0.7f,0.1f,0.9f)
                : new Color(0.1f,0.4f,0.9f,0.9f));
            shape.rect(p.x - W/2f, p.y + Y, W * ratio, H);
            shape.end();

            // marco
            shape.begin(ShapeRenderer.ShapeType.Line);
            shape.setColor(Color.BLACK);
            shape.rect(p.x - W/2f, p.y + Y, W, H);
            shape.end();

            // etiqueta y tamaño cola
            batch.begin();
            font.setColor(0,0,0,1);
            String tag = (d.type == BType.HQ) ? "HQ" : "G";
            font.draw(batch, tag+" Q:"+d.queue.size, p.x, p.y + Y + 16f, 0, Align.center, false);
            batch.end();
        }
    }

    // ===================== PROYECTILES =====================
    private void updateProjectiles(float dt) {
        for (int i = 0; i < projectiles.size(); i++) projectiles.get(i).update(dt);

        for (int i = 0; i < projectiles.size(); ) {
            Projectile p = projectiles.get(i);
            boolean removeP = false;

            // 1) choque con unidades
            Array<Unit> victims = p.fromPlayer ? enemyUnits : playerUnits;
            int hitIdx = indexUnitHitByProjectile(victims, p);
            if (hitIdx >= 0) {
                Unit victim = victims.get(hitIdx);
                if (victim instanceof SoldierBasic) {
                    SoldierBasic sv = (SoldierBasic) victim;
                    boolean dead = sv.takeDamage(p.damage);
                    if (dead) {
                        victims.removeIndex(hitIdx);
                        if (p.fromPlayer) enemySoldiers.removeValue(sv, true);
                        else              playerSoldiers.removeValue(sv, true);
                        selectedUnits.removeValue(sv, true);
                        if (p.fromPlayer) plastic += 15; // recompensa simple
                    }
                } else if (victim instanceof Bulldozer) {
                    Bulldozer bz = (Bulldozer) victim;
                    boolean dead = bz.takeDamage(p.damage);
                    if (dead) {
                        victims.removeIndex(hitIdx);
                        selectedUnits.removeValue(bz, true);
                        if (bz == bulldozer) bulldozer = null; // limpiar ref si era tu dozer
                    }
                } else if (victim instanceof MineSweeper) {
                    MineSweeper ms = (MineSweeper) victim;
                    boolean dead = ms.takeDamage(p.damage);
                    if (dead) {
                        victims.removeIndex(hitIdx);
                        selectedUnits.removeValue(ms, true);
                    }
                }


                removeP = true;
            }

            // 2) choque con FORTINES del jugador (si el disparo es enemigo)
            if (!removeP && !p.fromPlayer) {
                int fi = indexFortinHitByProjectile(playerFortins, p);
                if (fi >= 0) {
                    Fortin f = playerFortins.get(fi);
                    boolean dead = f.takeDamage(p.damage);
                    if (dead) playerFortins.removeIndex(fi);
                    removeP = true;
                }
            }

            // 3) choque con EDIFICIOS (ambos bandos)
            if (!removeP) {
                Array<Building> bTargets = p.fromPlayer ? enemyBuildings : buildings;
                int bi = indexBuildingHitByProjectile(bTargets, p);
                if (bi >= 0) {
                    Building b = bTargets.get(bi);
                    boolean dead = b.takeDamage(p.damage);
                    if (dead) {
                        bTargets.removeIndex(bi);
                        if (bTargets == buildings) {
                            // limpiar BData si es edificio del jugador
                            removeBDataFor(b);
                            // re-actualizar anclas de volquetas (por si se fue el depósito/HQ)
                            updateAllTruckStorageAnchors();
                        }
                    }
                    removeP = true;
                }
            }

            if (!removeP && p.expired()) removeP = true;
            if (removeP) projectiles.remove(i); else i++;
        }
    }
    private int indexBuildingHitByProjectile(Array<Building> blds, Projectile p) {
        if (blds == null) return -1;
        float r = p.radius;
        for (int j = 0; j < blds.size; j++) {
            Building b = blds.get(j);
            if (b == null) continue;
            if (circleIntersectsRect(p.pos.x, p.pos.y, r, b.getBounds())) return j;
        }
        return -1;
    }

    private boolean circleIntersectsRect(float cx, float cy, float radius, Rectangle rect) {
        float nx = Math.max(rect.x, Math.min(cx, rect.x + rect.width));
        float ny = Math.max(rect.y, Math.min(cy, rect.y + rect.height));
        float dx = cx - nx;
        float dy = cy - ny;
        return (dx * dx + dy * dy) <= radius * radius;
    }
    private void removeBDataFor(Building dead) {
        for (int i = 0; i < bdata.size; ) {
            if (bdata.get(i).b == dead) bdata.removeIndex(i);
            else i++;
        }
    }
    private void drawBuildingHealthBars() {
        float W = 44f, H = 6f, YOFF = 36f;

        // Jugador (verde)
        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < buildings.size; i++) {
            Building b = buildings.get(i);
            float ratio = Math.max(0f, Math.min(1f, (float)b.getHp() / (float)b.getMaxHp()));
            float x = b.getPosition().x - W/2f, y = b.getPosition().y + YOFF;

            shape.setColor(new Color(0f,0f,0f,0.25f));
            shape.rect(x, y, W, H);
            shape.setColor(new Color(0.1f, 0.8f, 0.2f, 0.95f));
            shape.rect(x, y, W * ratio, H);
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.BLACK);
        for (int i = 0; i < buildings.size; i++) {
            Building b = buildings.get(i);
            float x = b.getPosition().x - W/2f, y = b.getPosition().y + YOFF;
            shape.rect(x, y, W, H);
        }
        shape.end();

        // Enemigo (naranja/rojo)
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < enemyBuildings.size; i++) {
            Building b = enemyBuildings.get(i);
            float ratio = Math.max(0f, Math.min(1f, (float)b.getHp() / (float)b.getMaxHp()));
            float x = b.getPosition().x - W/2f, y = b.getPosition().y + YOFF;

            shape.setColor(new Color(0f,0f,0f,0.25f));
            shape.rect(x, y, W, H);
            shape.setColor(new Color(0.95f, 0.4f, 0.1f, 0.95f));
            shape.rect(x, y, W * ratio, H);
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.BLACK);
        for (int i = 0; i < enemyBuildings.size; i++) {
            Building b = enemyBuildings.get(i);
            float x = b.getPosition().x - W/2f, y = b.getPosition().y + YOFF;
            shape.rect(x, y, W, H);
        }
        shape.end();
    }


    private int indexUnitHitByProjectile(Array<Unit> units, Projectile p) {
        float r2 = p.radius * p.radius + 20f * 20f; // cuerpo ~20
        for (int j = 0; j < units.size; j++) {
            Unit u = units.get(j);
            if (u == null) continue;
            if (u.getPosition().dst2(p.pos) <= r2) return j;
        }
        return -1;
    }

    private int indexFortinHitByProjectile(Array<Fortin> forts, Projectile p) {
        float r2 = p.radius * p.radius + 18f * 18f; // radio corporal aprox del fortín
        for (int j = 0; j < forts.size; j++) {
            Fortin f = forts.get(j);
            if (f.getPosition().dst2(p.pos) <= r2) return j;
        }
        return -1;
    }

    private Unit findNearestInRange(Unit from, Array<Unit> candidates, float range) {
        float bestD2 = range * range; Unit best = null;
        for (int i = 0; i < candidates.size; i++) {
            Unit u = candidates.get(i); if (u == null) continue;
            float d2 = from.getPosition().dst2(u.getPosition());
            if (d2 <= bestD2) { bestD2 = d2; best = u; }
        }
        return best;
    }

    private Unit findNearestInRangeFromPos(Vector2 fromPos, Array<Unit> candidates, float range) {
        float bestD2 = range * range; Unit best = null;
        for (int i = 0; i < candidates.size; i++) {
            Unit u = candidates.get(i); if (u == null) continue;
            float d2 = fromPos.dst2(u.getPosition());
            if (d2 <= bestD2) { bestD2 = d2; best = u; }
        }
        return best;
    }
    // ======== Placement helpers (no superponer con pilas ni estructuras) ========
    private Rectangle prospectiveBuildingBounds(Vector2 center) {
        float s = BUILD_FOOTPRINT;
        return new Rectangle(center.x - s/2f, center.y - s/2f, s, s);
    }

    private boolean rectOverlapsRect(Rectangle a, Rectangle b) {
        return a.overlaps(b);
    }
    private BData getBDataFor(Building b) {
        for (int i = 0; i < bdata.size; i++) {
            if (bdata.get(i).b == b) return bdata.get(i);
        }
        return null;
    }

    private Building findBuildingAtPoint(Vector2 pos) {
        for (int i = 0; i < buildings.size; i++) {
            if (buildings.get(i).getBounds().contains(pos)) return buildings.get(i);
        }
        // (opcional) permitir “ver” edificios enemigos
        for (int i = 0; i < enemyBuildings.size; i++) {
            if (enemyBuildings.get(i).getBounds().contains(pos)) return enemyBuildings.get(i);
        }
        return null;
    }


    private boolean rectOverlapsCircle(Rectangle r, Vector2 c, float radius) {
        float nx = Math.max(r.x, Math.min(c.x, r.x + r.width));
        float ny = Math.max(r.y, Math.min(c.y, r.y + r.height));
        float dx = c.x - nx;
        float dy = c.y - ny;
        return (dx*dx + dy*dy) <= radius*radius;
    }

    /** Devuelve true si SE PUEDE colocar en 'pos' el tipo dado (sin pisar nada). */
    private boolean canPlaceAt(BType type, Vector2 pos) {
        if (pos == null) return false;

        // 1) No sobre ToyPiles
        if (type == BType.FORTIN) {
            float rr = FORTIN_FOOTPRINT_RADIUS + TOYPILE_BLOCK_MARGIN;
            for (int i = 0; i < toyPiles.size; i++) {
                ToyPile p = toyPiles.get(i);
                float blockR = p.getArea().radius + rr;
                if (p.getPosition().dst2(pos) <= blockR * blockR) return false;
            }
        } else { // HQ/Storage/Garage (rect vs circle)
            Rectangle pb = prospectiveBuildingBounds(pos);
            for (int i = 0; i < toyPiles.size; i++) {
                ToyPile p = toyPiles.get(i);
                float r = p.getArea().radius + TOYPILE_BLOCK_MARGIN;
                if (rectOverlapsCircle(pb, p.getPosition(), r)) return false;
            }
        }

        // 2) No sobre edificios existentes
        if (type == BType.FORTIN) {
            // Fortín (círculo) contra edificios (rectángulos)
            float rr = FORTIN_FOOTPRINT_RADIUS + BUILD_BLOCK_MARGIN;
            for (int i = 0; i < buildings.size; i++) {
                if (rectOverlapsCircle(buildings.get(i).getBounds(), pos, rr)) return false;
            }
        } else {
            // Edificio (rect) contra edificios (rect)
            Rectangle pb = prospectiveBuildingBounds(pos);
            for (int i = 0; i < buildings.size; i++) {
                Rectangle b = buildings.get(i).getBounds();
                // margen: expandimos el edificio existente
                Rectangle expanded = new Rectangle(b.x - BUILD_BLOCK_MARGIN, b.y - BUILD_BLOCK_MARGIN,
                    b.width + 2*BUILD_BLOCK_MARGIN, b.height + 2*BUILD_BLOCK_MARGIN);
                if (rectOverlapsRect(pb, expanded)) return false;
            }
        }

        // 3) No sobre fortines existentes
        if (type == BType.FORTIN) {
            // círculo-círculo
            float minDist = (FORTIN_FOOTPRINT_RADIUS*2f) + BUILD_BLOCK_MARGIN;
            float minD2 = minDist * minDist;
            for (int i = 0; i < playerFortins.size; i++) {
                if (playerFortins.get(i).getPosition().dst2(pos) <= minD2) return false;
            }
        } else {
            // rect contra círculo
            Rectangle pb = prospectiveBuildingBounds(pos);
            float rr = FORTIN_FOOTPRINT_RADIUS + BUILD_BLOCK_MARGIN;
            for (int i = 0; i < playerFortins.size; i++) {
                if (rectOverlapsCircle(pb, playerFortins.get(i).getPosition(), rr)) return false;
            }
        }

        // 4) Límites del mapa (simple)
        float MAP_WIDTH = 2000, MAP_HEIGHT = 2000;
        if (type == BType.FORTIN) {
            if (pos.x < FORTIN_FOOTPRINT_RADIUS || pos.y < FORTIN_FOOTPRINT_RADIUS) return false;
            if (pos.x > MAP_WIDTH - FORTIN_FOOTPRINT_RADIUS || pos.y > MAP_HEIGHT - FORTIN_FOOTPRINT_RADIUS) return false;
        } else {
            Rectangle pb = prospectiveBuildingBounds(pos);
            if (pb.x < 0 || pb.y < 0 || pb.x + pb.width > MAP_WIDTH || pb.y + pb.height > MAP_HEIGHT) return false;
        }
        // 5) también evitar edificios enemigos
        if (type == BType.FORTIN) {
            float rr = FORTIN_FOOTPRINT_RADIUS + BUILD_BLOCK_MARGIN;
            for (int i = 0; i < enemyBuildings.size; i++) {
                if (rectOverlapsCircle(enemyBuildings.get(i).getBounds(), pos, rr)) return false;
            }
        } else {
            Rectangle pb = prospectiveBuildingBounds(pos);
            for (int i = 0; i < enemyBuildings.size; i++) {
                Rectangle b = enemyBuildings.get(i).getBounds();
                Rectangle expanded = new Rectangle(b.x - BUILD_BLOCK_MARGIN, b.y - BUILD_BLOCK_MARGIN,
                    b.width + 2*BUILD_BLOCK_MARGIN, b.height + 2*BUILD_BLOCK_MARGIN);
                if (rectOverlapsRect(pb, expanded)) return false;
            }
        }


        return true;
    }

    // ===================== INPUT & CÁMARA =====================
    private void handleCamera(float delta) {
        float speed = 400 * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))    camera.position.y += speed;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))  camera.position.y -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))  camera.position.x -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) camera.position.x += speed;

        float MAP_WIDTH = 2000, MAP_HEIGHT = 2000;
        float halfW = camera.viewportWidth / 2, halfH = camera.viewportHeight / 2;
        camera.position.x = Math.max(halfW, Math.min(MAP_WIDTH - halfW, camera.position.x));
        camera.position.y = Math.max(halfH, Math.min(MAP_HEIGHT - halfH, camera.position.y));
        camera.update();
    }

    private void handleInput() {
        // Cambiar tipo de edificio a construir
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) currentBuildType = BType.HQ;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) currentBuildType = BType.STORAGE;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) currentBuildType = BType.GARAGE;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) currentBuildType = BType.FORTIN;

        // HQ -> Soldado (cola acepta sin plástico; se cobra al iniciar producción)
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            BData hq = firstOfType(BType.HQ);
            if (hq != null && !hq.b.isUnderConstruction()) {
                hq.queue.add(new ProdOrder("soldier", TIME_SOLDIER));
            }
        }


        // HQ -> Busca-minas (cola acepta sin plástico; se cobra al iniciar producción)
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            BData hq = firstOfType(BType.HQ);
            if (hq != null && !hq.b.isUnderConstruction()) {
                hq.queue.add(new ProdOrder("sweeper", TIME_SWEEPER));
            }
        }

        // GARAGE -> Volqueta (cola acepta sin plástico; se cobra al iniciar producción)
        if (Gdx.input.isKeyJustPressed(Input.Keys.V)) {
            BData gar = firstOfType(BType.GARAGE);
            if (gar != null && !gar.b.isUnderConstruction()) {
                gar.queue.add(new ProdOrder("truck", TIME_TRUCK));
            }
        }
        // Toggle desarme automático del Busca-minas
        if (Gdx.input.isKeyJustPressed(Input.Keys.X)
            && selectedUnits.size == 1
            && selectedUnits.first() instanceof MineSweeper) {
            MineSweeper ms = (MineSweeper) selectedUnits.first();
            ms.setAutoDisarm(!ms.isAutoDisarm());
        }




        // GARAGE -> Tanque (cola acepta sin plástico; se cobra al iniciar producción)
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            BData gar = firstOfType(BType.GARAGE);
            if (gar != null && !gar.b.isUnderConstruction()) {
                gar.queue.add(new ProdOrder("tank", TIME_TANK));
            }
        }


        // Selección
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            selectStart = screenToWorld(Gdx.input.getX(), Gdx.input.getY());
            selecting = true;
        }
        if (selecting) selectEnd = screenToWorld(Gdx.input.getX(), Gdx.input.getY());

        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT) && selecting) {
            selecting = false;
            selectedUnits.clear();
            selectedBuildingUI = null;

            Rectangle selection = getSelectionRectangle();

            // 1) unidades
            for (Unit u : playerUnits) {
                if (selection.contains(u.getPosition())) selectedUnits.add(u);
            }

            // 2) si no hay unidades y fue “click” (rectángulo muy chico), probamos edificios
            float tiny = 6f;
            boolean wasClick = selection.width < tiny && selection.height < tiny;
            if (selectedUnits.size == 0 && wasClick) {
                Vector2 clickWorld = new Vector2(selection.x + selection.width / 2f,
                    selection.y + selection.height / 2f);
                selectedBuildingUI = findBuildingAtPoint(clickWorld);
            }
        }


        // Clic derecho: construir / asignar volqueta / mover en formación
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            Vector2 click = screenToWorld(Gdx.input.getX(), Gdx.input.getY());
            boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
            boolean ctrl  = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);

            // Construcción con bulldozer
            if (shift && selectedUnits.size == 1 && selectedUnits.first() instanceof Bulldozer) {
                tryBuildAt(click);
                return;
            }

            // Asignación de Volqueta
            if (ctrl && hasSelectedTruck()) {
                ToyPile targetPile = findToyPileAt(click);
                if (targetPile != null) {
                    for (int i = 0; i < selectedUnits.size; i++) {
                        Unit u = selectedUnits.get(i);
                        if (u instanceof PlasticTruck) {
                            PlasticTruck t = (PlasticTruck) u;
                            t.setStorageAnchor(getStorageAnchorPoint()); // puede ser null si no hay depósito completado
                            t.assignToyPile(targetPile);
                        }
                    }
                    return;
                }
            }

            // Movimiento normal en formación
            if (selectedUnits.size > 0) {
                float spacing = 50f;
                int cols = (int) Math.ceil(Math.sqrt(selectedUnits.size));
                int i = 0;
                for (Unit u : selectedUnits) {
                    int row = i / cols, col = i % cols;
                    float offX = (col - cols / 2f) * spacing;
                    float offY = (row - cols / 2f) * spacing;
                    Vector2 unitDest = new Vector2(click.x + offX, click.y + offY);
                    u.setTarget(unitDest);
                    i++;
                }
            }
        }
    }

    private boolean hasSelectedTruck() {
        for (int i = 0; i < selectedUnits.size; i++) if (selectedUnits.get(i) instanceof PlasticTruck) return true;
        return false;
    }

    private ToyPile findToyPileAt(Vector2 pos) {
        ToyPile nearest = null;
        float best = Float.MAX_VALUE;
        for (int i = 0; i < toyPiles.size; i++) {
            ToyPile p = toyPiles.get(i);
            if (p.contains(pos.x, pos.y)) return p;
            float d = p.getPosition().dst2(pos);
            float lim = (p.getArea().radius + 16f);
            if (d <= lim * lim && d < best) { best = d; nearest = p; }
        }
        return nearest;
    }

    // Construcción con bulldozer (diferida) con validación anti-superposición
    private void tryBuildAt(Vector2 dest) {

        // Validación de sitio según tipo
        if (!canPlaceAt(currentBuildType, dest)) return;

        // Programar obra y mover bulldozer
        if (currentBuildType == BType.FORTIN) {
            if (plastic < COST_FORTIN) return;
            pendingBuild = new BuildOrder(BType.FORTIN, new Vector2(dest), COST_FORTIN);
            if (bulldozer != null) bulldozer.setTarget(new Vector2(dest));
            return;
        }

        int cost = (currentBuildType == BType.HQ) ? COST_HQ :
            (currentBuildType == BType.STORAGE ? COST_STORAGE : COST_GARAGE);
        if (plastic < cost) return;

        pendingBuild = new BuildOrder(currentBuildType, new Vector2(dest), cost);
        if (bulldozer != null) bulldozer.setTarget(new Vector2(dest));
    }



    // Si el dozer llegó al punto, crear si sigue siendo válido (sin pisar nada)
    private void updateBulldozerBuild() {
        if (pendingBuild == null || bulldozer == null) return;

        if (bulldozer.getPosition().dst2(pendingBuild.pos) <= BUILD_START_RADIUS * BUILD_START_RADIUS) {

            // Revalidar sitio al llegar (pudo aparecer algo)
            if (!canPlaceAt(pendingBuild.type, pendingBuild.pos)) {
                // Sitio ya no es válido => cancelar sin cobrar
                pendingBuild = null;
                return;
            }

            if (pendingBuild.type == BType.FORTIN) {
                Fortin fort = new Fortin(new Vector2(pendingBuild.pos));
                fort.startConstruction(BUILD_TIME_FORTIN);
                playerFortins.add(fort);
                plastic -= pendingBuild.cost;
                pendingBuild = null;
                return;
            }

            Building nb = new Building(new Vector2(pendingBuild.pos), "building_storage.png");
            nb.startConstruction(BUILD_TIME); // 5 s
            buildings.add(nb);
            bdata.add(new BData(nb, pendingBuild.type));

            plastic -= pendingBuild.cost;
            pendingBuild = null;
        }
    }



    private void drawPendingBuildGhost() {
        if (pendingBuild == null) return;
        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(new Color(0.1f, 0.6f, 1f, 0.8f));
        // cuadrado simple fantasma de ~64x64 (placeholder)
        float s = 32f;
        shape.rect(pendingBuild.pos.x - s, pendingBuild.pos.y - s, s*2, s*2);
        shape.end();
    }

    private void updateAllTruckStorageAnchors() {
        Vector2 anchor = getStorageAnchorPoint();      // solo depósitos COMPLETADOS
        if (anchor == null) return;
        for (int i = 0; i < trucks.size; i++) trucks.get(i).setStorageAnchor(anchor);
    }

    // Devuelve POS de depósito completado (no en obra), si existe
    private Vector2 getStorageAnchorPoint() {
        for (int i = 0; i < bdata.size; i++) {
            BData d = bdata.get(i);
            if (d.type == BType.STORAGE && !d.b.isUnderConstruction()) {
                return new Vector2(d.b.getPosition());
            }
        }
        return null;
    }

    private boolean hasStorage() {
        return getStorageAnchorPoint() != null;
    }

    private BData firstOfType(BType type) {
        for (int i = 0; i < bdata.size; i++) if (bdata.get(i).type == type) return bdata.get(i);
        return null;
    }

    // ===================== FIN DE PARTIDA =====================
    private void checkGameOver() {
        if (gameState != GameState.RUNNING) return;
        // Sin edificios del jugador -> derrota
        if (buildings.size == 0) {
            gameState = GameState.DEFEAT;
            return;
        }
        // Sin edificios enemigos -> victoria
        if (enemyBuildings.size == 0) {
            gameState = GameState.VICTORY;
        }
    }


    private void drawGameOverOverlay() {
        // Oscurecer pantalla
        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(new Color(0f, 0f, 0f, 0.45f));
        float w = camera.viewportWidth, h = camera.viewportHeight;
        shape.rect(camera.position.x - w/2f, camera.position.y - h/2f, w, h);
        shape.end();

        // Texto según estado
        String text = (gameState == GameState.VICTORY) ? "VICTORIA" : "DERROTA";

        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(2f);
        font.draw(batch, text,
            camera.position.x - 100f,
            camera.position.y + 10f);
        font.getData().setScale(1f);
        batch.end();
    }


    // ===================== UTILS =====================
    private Rectangle getSelectionRectangle() {
        float x = Math.min(selectStart.x, selectEnd.x);
        float y = Math.min(selectStart.y, selectEnd.y);
        return new Rectangle(x, y, Math.abs(selectStart.x - selectEnd.x), Math.abs(selectStart.y - selectEnd.y));
    }
    private Vector2 screenToWorld(int x, int y) {
        Vector3 tmp = new Vector3(x, y, 0);
        camera.unproject(tmp);
        return new Vector2(tmp.x, tmp.y);
    }

    // ===================== LIFECYCLE =====================
    @Override public void resize(int w, int h) { camera.setToOrtho(false, w, h); }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() {
        batch.dispose(); shape.dispose(); font.dispose();
    }
}
