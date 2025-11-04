package com.armymen.screens;

import com.armymen.MainGame;
import com.armymen.entities.*;
import com.armymen.systems.ResourceManager;
import com.armymen.entities.PlasticTruck;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

// --- Scene2D UI ---
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.math.MathUtils;
import java.util.HashSet;


public class GameScreen implements Screen {

    // === Core ===
    private final MainGame game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shape;

    private Array<Unit> playerUnits;
    private Array<Unit> selectedUnits;
    private Array<Building> buildings;
    private Bulldozer bulldozer;
    private ResourceManager resourceManager;
    private Array<Mine> mines;
    private Array<ToyPile> toyPiles;
    private com.badlogic.gdx.utils.Array<Projectile> bullets; // NUEVO

    // === Selección con arrastre ===
    private boolean selecting = false;
    private Vector2 selectStart = new Vector2();
    private Vector2 selectEnd = new Vector2();

    // === HUD / UI ===
    private Stage stage;
    private Skin skin;
    private Label plasticLabel;
    private Label modeLabel;
    // Arriba, junto a tus colecciones:
    private Array<Unit> enemyUnits;
    private Array<Building> enemyBuildings;

    private boolean gameWon = false;

    // === HUD extra (estado) ===
    private Label unitsTitleLabel;
    private Label totalUnitsLabel;
    private Label soldierLabel;
    private Label sweeperLabel;
    private Label truckLabel;
    private Label tankLabel;
    private Label bulldozerLabel;

    private Label queuesTitleLabel;
    private Label queuesCountLabel;
    private Label queuesListLabel; // resumen corto de colas

    private ActionMode actionMode = ActionMode.NONE;

    // === Mundo grande ===
    private static final float WORLD_W = 6000f;
    private static final float WORLD_H = 6000f;

    // === Fondo ===
    private Texture bgTex;
    private static final int BG_TILE = 256; // tamaño del tile en px (ajusta a tu imagen)

    // Zona segura de “spawn” inicial para no poner minas/pilas muy cerca
    private static final Vector2 START_POS = new Vector2(700, 450); // zona inicial aprox. tus edificios/unidades
    private static final float SAFE_RADIUS = 500f;                   // nada peligroso tan cerca

    // Generación aleatoria (puedes ajustar cantidades)
    private static final int NUM_RANDOM_PILES = 40;   // cantidad de ToyPiles
    private static final int NUM_RANDOM_MINES = 70;   // cantidad de minas
    private static final int PILE_MIN_AMOUNT = 80;
    private static final int PILE_MAX_AMOUNT = 260;
    private enum ActionMode {
        NONE,
        BUILD_STORAGE,
        BUILD_HQ, BUILD_DEPOT, BUILD_GARAGE, BUILD_FORTIN,
        ENQUEUE_HQ_SOLDIER, ENQUEUE_HQ_SWEEPER,
        ENQUEUE_GAR_TRUCK, ENQUEUE_GAR_TANK
    }
    private static final float FORT_RADIUS = 260f; // radio de defensa del fortín
    // Penalidades/bonos simples
    private final int MINE_PENALTY = 20;   // lo que te resta si explota
    private final int DISARM_BONUS = 5;    // lo que ganas al desactivar con buscaminas


    public GameScreen(MainGame game) {
        this.game = game;

        // Cámara, batch, shapes
        this.camera = new OrthographicCamera();
        this.camera.setToOrtho(false, 1280, 720);
        this.batch = new SpriteBatch();
        this.shape = new ShapeRenderer();

        bgTex = new Texture("bg_grass.png"); // debe ser una textura "seamless"

        // Estado del juego
        this.buildings = new Array<>();
        this.playerUnits = new Array<>();
        this.selectedUnits = new Array<>();
        this.resourceManager = new ResourceManager(200); // plástico inicial

        // ======= Colecciones =======
        mines = new Array<>();
        toyPiles = new Array<>();

        enemyUnits = new Array<>();
        enemyBuildings = new Array<>();

        bullets = new com.badlogic.gdx.utils.Array<>();

        // ======= Generación de mundo grande =======
        generateWorld(); // <-- NUEVO: crea pilas y minas aleatoriamente
        generateEnemyCamps();

        // Entidades base
        bulldozer = new Bulldozer(new Vector2(500, 500));
        playerUnits.add(bulldozer);

        // Algunos soldados
        playerUnits.add(new Unit(new Vector2(400, 300)));
        playerUnits.add(new Unit(new Vector2(600, 350)));
        playerUnits.add(new Unit(new Vector2(800, 300)));
        playerUnits.add(new Unit(new Vector2(1000, 500)));
        playerUnits.add(new Unit(new Vector2(1200, 250)));


        playerUnits.add(new SoldierAlt(new Vector2(1300, 300)));
        playerUnits.add(new SoldierAlt(new Vector2(1350, 330)));
        playerUnits.add(new SoldierAlt(new Vector2(1400, 310)));

        // Un edificio de ejemplo
        buildings.add(new Building(new Vector2(700, 400), "building_storage.png"));


        // Listener de construcción del bulldozer
        Bulldozer.setBuildListener(new Bulldozer.BuildListener() {
            @Override
            public void onBuildingCreated(Building building) {
                buildings.add(building);
            }
        });

        // === UI ===
        createUI();

        // Multiplexor de input: primero UI, luego juego
        InputMultiplexer mux = new InputMultiplexer(stage, new com.badlogic.gdx.InputAdapter(){});
        Gdx.input.setInputProcessor(mux);
    }

    private void createUI() {
        stage = new Stage(new ScreenViewport());

        // Carga del skin (json + atlas en core/assets/)
        skin = new Skin(
            Gdx.files.internal("uiskin.json"),
            new TextureAtlas(Gdx.files.internal("uiskin.atlas"))
        );

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // ===== Panel izquierdo (acciones) =====
        Table left = new Table(skin);
        left.defaults().pad(4).fillX();

        // --- Título
        Label title = new Label("Acciones", skin, "default");
        left.add(title).row();

        // ---------- Construir (con costos) ----------
        left.add(new Label("Construir", skin)).row();

        // Tu botón original: Construir Almacén (depósito simple -50)
//        final TextButton btnBuildStorage = new TextButton("Construir Almacen (-50)", skin);
//        btnBuildStorage.addListener(new ClickListener() {
//            @Override public void clicked(InputEvent event, float x, float y) {
//                actionMode = ActionMode.BUILD_STORAGE;
//                modeLabel.setText("Modo: Construir Almacén (clic IZQ. en el mapa)");
//            }
//        });
//        left.add(btnBuildStorage).row();

        // Nuevos: HQ / Depósito / Garaje (usan costos de Building)
        final TextButton btnBuildHQ = new TextButton("HQ (-" + com.armymen.entities.Building.COST_HQ + ")", skin);
        btnBuildHQ.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                actionMode = ActionMode.BUILD_HQ;
                modeLabel.setText("Modo: Construir HQ (clic mapa)");
            }
        });
        left.add(btnBuildHQ).row();

        final TextButton btnBuildDepot = new TextButton("Deposito (-" + com.armymen.entities.Building.COST_DEPOT + ")", skin);
        btnBuildDepot.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                actionMode = ActionMode.BUILD_DEPOT;
                modeLabel.setText("Modo: Construir Depósito (clic mapa)");
            }
        });
        left.add(btnBuildDepot).row();

        final TextButton btnBuildGarage = new TextButton("Garaje (-" + com.armymen.entities.Building.COST_GARAGE + ")", skin);
        btnBuildGarage.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                actionMode = ActionMode.BUILD_GARAGE;
                modeLabel.setText("Modo: Construir Garaje (clic mapa)");
            }
        });
        left.add(btnBuildGarage).row();
        final TextButton btnBuildFortin = new TextButton("Fortin (-" + com.armymen.entities.Building.COST_FORTIN + ")", skin);
        btnBuildFortin.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                actionMode = ActionMode.BUILD_FORTIN;
                modeLabel.setText("Modo: Construir Fortín (clic mapa)");
            }
        });
        left.add(btnBuildFortin).row();

        left.add(new Label(" ", skin)).row();

        // ---------- Producción (colas) ----------
        left.add(new Label("Producir (cola)", skin)).row();

        // HQ: Soldado / BuscaMinas
        final TextButton btnHQSoldier = new TextButton("HQ: Soldado (+" + com.armymen.entities.Building.COST_SOLDIER + ")", skin);
        btnHQSoldier.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                actionMode = ActionMode.ENQUEUE_HQ_SOLDIER;
                modeLabel.setText("Modo: Encolar Soldado en HQ (clic un HQ)");
            }
        });
        left.add(btnHQSoldier).row();

        final TextButton btnHQSweeper = new TextButton("HQ: BuscaMinas (+" + com.armymen.entities.Building.COST_SWEEPER + ")", skin);
        btnHQSweeper.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                actionMode = ActionMode.ENQUEUE_HQ_SWEEPER;
                modeLabel.setText("Modo: Encolar BuscaMinas en HQ (clic un HQ)");
            }
        });
        left.add(btnHQSweeper).row();

        // Garaje: Volqueta / Tanque
        final TextButton btnGarTruck = new TextButton("Garaje: Volqueta (+" + com.armymen.entities.Building.COST_TRUCK + ")", skin);
        btnGarTruck.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                actionMode = ActionMode.ENQUEUE_GAR_TRUCK;
                modeLabel.setText("Modo: Encolar Volqueta en Garaje (clic un Garaje)");
            }
        });
        left.add(btnGarTruck).row();

        final TextButton btnGarTank = new TextButton("Garaje: Tanque (+" + com.armymen.entities.Building.COST_TANK + ")", skin);
        btnGarTank.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                actionMode = ActionMode.ENQUEUE_GAR_TANK;
                modeLabel.setText("Modo: Encolar Tanque en Garaje (clic un Garaje)");
            }
        });
        left.add(btnGarTank).row();

        left.add(new Label(" ", skin)).row();

        // --- Cancelar modo
//        final TextButton btnCancel = new TextButton("Cancelar modo", skin);
//        btnCancel.addListener(new ClickListener() {
//            @Override public void clicked(InputEvent event, float x, float y) {
//                actionMode = ActionMode.NONE;
//                modeLabel.setText("Modo: Ninguno");
//            }
//        });
//        left.add(btnCancel).row();

        // ===== Panel superior derecho (indicadores) =====
        Table top = new Table(skin);
        top.defaults().pad(6);

        plasticLabel = new Label("Plástico: 0", skin);
        modeLabel = new Label("Modo: Ninguno", skin);

        // ===== Layout general =====
        root.left().top();
        root.add(left).left().top().width(240f);

        root.add().expand(); // espacio central

        Table rightCol = new Table(skin);
        rightCol.add(plasticLabel).right().row();
        rightCol.add(modeLabel).right().row();

        root.add(rightCol).top().right().pad(8);

        // ===== Bloque: Estado (unidades y colas) =====
        rightCol.row();
        rightCol.add(new Label(" ", skin)).right().row(); // separador visual

        unitsTitleLabel = new Label("Unidades", skin);
        rightCol.add(unitsTitleLabel).right().row();

        totalUnitsLabel = new Label("Total: 0", skin);
        soldierLabel    = new Label("Soldados: 0", skin);
        sweeperLabel    = new Label("Buscaminas: 0", skin);
        truckLabel      = new Label("Volquetas: 0", skin);
        tankLabel       = new Label("Tanques: 0", skin);
        bulldozerLabel  = new Label("Bulldozers: 0", skin);

        rightCol.add(totalUnitsLabel).right().row();
        rightCol.add(soldierLabel).right().row();
        rightCol.add(sweeperLabel).right().row();
        rightCol.add(truckLabel).right().row();
        rightCol.add(tankLabel).right().row();
        rightCol.add(bulldozerLabel).right().row();

        rightCol.row();
        rightCol.add(new Label(" ", skin)).right().row(); // separador

        queuesTitleLabel = new Label("Colas de producción", skin);
        queuesCountLabel = new Label("Colas activas: 0", skin);
        queuesListLabel  = new Label("-", skin);

        rightCol.add(queuesTitleLabel).right().row();
        rightCol.add(queuesCountLabel).right().row();
        rightCol.add(queuesListLabel).right().row();
    }

    @Override
    public void render(float delta) {
        update(delta);

        // Fondo
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // === FONDO TILEADO según cámara ===
        float halfW = camera.viewportWidth * 0.5f;
        float halfH = camera.viewportHeight * 0.5f;

        // límites visibles en mundo
        float viewLeft   = camera.position.x - halfW;
        float viewRight  = camera.position.x + halfW;
        float viewBottom = camera.position.y - halfH;
        float viewTop    = camera.position.y + halfH;

        // primer tile a dibujar (desplazado al múltiplo de BG_TILE)
        int startX = (int)Math.floor(viewLeft  / BG_TILE) - 1;
        int endX   = (int)Math.ceil (viewRight / BG_TILE) + 1;
        int startY = (int)Math.floor(viewBottom/ BG_TILE) - 1;
        int endY   = (int)Math.ceil (viewTop   / BG_TILE) + 1;

        // clampa para no dibujar fuera del mundo (opcional)
        int minTileX = 0;
        int maxTileX = (int)Math.ceil(WORLD_W / BG_TILE);
        int minTileY = 0;
        int maxTileY = (int)Math.ceil(WORLD_H / BG_TILE);

        startX = Math.max(startX, minTileX);
        startY = Math.max(startY, minTileY);
        endX   = Math.min(endX,   maxTileX);
        endY   = Math.min(endY,   maxTileY);

        // pinta la grilla visible
        for (int ty = startY; ty < endY; ty++) {
            for (int tx = startX; tx < endX; tx++) {
                float x = tx * BG_TILE;
                float y = ty * BG_TILE;
                batch.draw(bgTex, x, y, BG_TILE, BG_TILE);
            }
        }

        // === (aquí ya sigues con tu dibujo actual) ===
        // 1) Edificios del jugador
        for (Building b : buildings) b.render(batch);
        // 2) ToyPiles
        for (ToyPile p : toyPiles) p.render(batch);
        // 3) Minas
        for (Mine m : mines) m.render(batch);
        // 4) Unidades jugador
        for (Unit u : playerUnits) u.render(batch);
        // 5) Unidades enemigas
        for (Unit e : enemyUnits) e.render(batch);
        // 6) Buildings enemigas
        for (Building eb : enemyBuildings) eb.render(batch);

        batch.end();

        // === Overlay de selección (ShapeRenderer) ===
        if (selecting) {
            shape.setProjectionMatrix(camera.combined);
            shape.begin(ShapeRenderer.ShapeType.Line);
            shape.setColor(Color.BLUE);
            Rectangle rect = getSelectionRectangle();
            shape.rect(rect.x, rect.y, rect.width, rect.height);
            shape.end();
        }

        // === Balas (circulitos) ===
        shape.setProjectionMatrix(camera.combined);
        shape.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < bullets.size; i++) {
            Projectile p = bullets.get(i);
            if (p.isEnemyBullet()) {
                shape.setColor(1f, 0f, 0f, 1f);   // rojo enemigo
            } else {
                shape.setColor(1f, 1f, 1f, 1f);   // blanco (o "azul" si prefieres: setColor(0f,0.5f,1f,1f))
            }
            shape.circle(p.getPos().x, p.getPos().y, p.getRadius());
        }
        shape.end();

        // Resaltar seleccionados
        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.RED);
        for (Unit u : selectedUnits)
            shape.circle(u.getPosition().x, u.getPosition().y, 25);
        shape.end();

        // === UI ===
        stage.act(delta);
        stage.draw();
    }

    private void update(float delta) {
        handleCamera(delta);
        handleInput();

        for (Unit u : playerUnits) u.update(delta);

        for (Unit e : enemyUnits) e.update(delta); // solo baja attackTimer

        // Empujón suave contra edificios (como ya lo tienes)
        for (Unit u : playerUnits) {
            for (Building b : buildings) {
                if (b.getBounds().contains(u.getPosition())) {
                    Vector2 dir = new Vector2(u.getPosition()).sub(b.getPosition()).nor();
                    u.getPosition().mulAdd(dir, 3f);
                }
            }
        }

        // Lógica de economía para volquetas
        for (int i = 0; i < playerUnits.size; i++) {
            Unit u = playerUnits.get(i);
            if (u instanceof PlasticTruck) {
                PlasticTruck t = (PlasticTruck) u;
                t.updateEconomy(delta, toyPiles, buildings, resourceManager);
            }
        }

        // --- Eliminar ToyPiles vacíos ---
        for (int i = toyPiles.size - 1; i >= 0; i--) {
            if (!toyPiles.get(i).hasMaterial()) {
                toyPiles.removeIndex(i);
            }
        }

        // Actualiza balas y limpia las que mueren
        for (int i = bullets.size - 1; i >= 0; i--) {
            Projectile p = bullets.get(i);
            boolean alive = p.update(delta);
            if (!alive) bullets.removeIndex(i);
        }

        // ======= Lógica de minas =======

        // 5.1) Primero: los MineSweeper desactivan minas en rango (sin pisarlas)
        for (int mi = mines.size - 1; mi >= 0; mi--) {
            Mine m = mines.get(mi);
            boolean removed = false;

            for (int ui = 0; ui < playerUnits.size; ui++) {
                Unit u = playerUnits.get(ui);
                if (u instanceof MineSweeper) {
                    MineSweeper s = (MineSweeper) u;
                    if (s.getPosition().dst(m.getPosition()) <= s.getDetectRadius()) {
                        resourceManager.add(DISARM_BONUS); // bonus por desactivar
                        mines.removeIndex(mi);
                        removed = true;
                        break;
                    }
                }
            }
            if (removed) continue; // ya removida, sigue con siguiente mina
        }

        // 5.15) Los Fortines desactivan minas en su radio
        for (int mi = mines.size - 1; mi >= 0; mi--) {
            Mine m = mines.get(mi);
            boolean removedByFort = false;

            for (int bi = 0; bi < buildings.size; bi++) {
                Building b = buildings.get(bi);
                if (b.getKind() != BuildingKind.FORTIN) continue;
                if (b.isEnemy()) continue; // SOLO fortín del jugador desactiva minas

                if (b.getPosition().dst(m.getPosition()) <= FORT_RADIUS) {
                    resourceManager.add(DISARM_BONUS);
                    mines.removeIndex(mi);
                    removedByFort = true;
                    break;
                }
            }
            if (removedByFort) continue;
        }

        // 5.2) Luego: si SOLDIER o TANK pisan una mina -> explotan (unidad + mina)
        for (int mi = mines.size - 1; mi >= 0; mi--) {
            Mine m = mines.get(mi);
            boolean exploded = false;

            for (int ui = playerUnits.size - 1; ui >= 0; ui--) {
                Unit u = playerUnits.get(ui);

                // Solo detonan si son SOLDIER o TANK
                String tag = u.getTag();
                if (!"SOLDIER".equals(tag) && !"TANK".equals(tag)) continue;

                float dist = u.getPosition().dst(m.getPosition());
                if (dist <= m.getRadius()) {
                    // Explota: eliminar unidad y mina
                    playerUnits.removeIndex(ui);
                    mines.removeIndex(mi);
                    exploded = true;
                    break;
                }
            }
            if (exploded) continue;
        }

        // --- COMBATE: enemigos disparan si el jugador entra en rango ---
        doCombatTick(delta);

        // HUD
        plasticLabel.setText("Plástico: " + resourceManager.getPlastic());

        // === Conteo de unidades por tipo ===
        int cSoldier = 0, cSweeper = 0, cTruck = 0, cTank = 0, cBulldozer = 0;
        for (int i = 0; i < playerUnits.size; i++) {
            Unit u = playerUnits.get(i);
            String tag = u.getTag();
            if ("SOLDIER".equals(tag)) cSoldier++;
            else if ("MINESWEEPER".equals(tag)) cSweeper++;
            else if ("TRUCK".equals(tag)) cTruck++;
            else if ("TANK".equals(tag)) cTank++;
            else if ("BULLDOZER".equals(tag)) cBulldozer++;
        }
        int totalUnits = playerUnits.size;

        totalUnitsLabel.setText("Total: " + totalUnits);
        soldierLabel.setText("Soldados: " + cSoldier);
        sweeperLabel.setText("Buscaminas: " + cSweeper);
        truckLabel.setText("Volquetas: " + cTruck);
        tankLabel.setText("Tanques: " + cTank);
        bulldozerLabel.setText("Bulldozers: " + cBulldozer);

// === Resumen de colas de producción ===
        int activeQueues = 0;
        int totalOrders = 0;
        StringBuilder sb = new StringBuilder(); // ejemplo: HQ#1[2], GARAGE#1[1], HQ#2[1]

        int hqIdx = 0, garIdx = 0, depIdx = 0;
        for (int i = 0; i < buildings.size; i++) {
            Building b = buildings.get(i);
            if (!b.hasQueue()) continue;
            int sz = b.getQueue().size();
            if (sz <= 0) continue;

            activeQueues++;
            totalOrders += sz;

            // Etiquetado simple por tipo
            String tag;
            if (b.getKind() == BuildingKind.HQ) tag = "HQ#" + (++hqIdx);
            else if (b.getKind() == BuildingKind.GARAGE) tag = "GARAGE#" + (++garIdx);
            else tag = "DEPOT#" + (++depIdx);

            if (sb.length() > 0) sb.append(", ");
            sb.append(tag).append("[").append(sz).append("]");
        }
        queuesCountLabel.setText("Colas activas: " + activeQueues + " (órdenes: " + totalOrders + ")");
        queuesListLabel.setText(sb.length() == 0 ? "-" : sb.toString());

        // Edificios (incluye ingreso pasivo del DEPOT y colas de HQ/GARAGE)
        for (Building b : buildings) {
            b.update(delta, playerUnits, resourceManager);
        }

        // Limpieza de unidades caídas
        for (int i = playerUnits.size - 1; i >= 0; i--) {
            if (playerUnits.get(i).isDead()) playerUnits.removeIndex(i);
        }
        for (int i = enemyUnits.size - 1; i >= 0; i--) {
            if (enemyUnits.get(i).isDead()) enemyUnits.removeIndex(i);
        }

        // Limpieza de edificios destruidos
        for (int i = buildings.size - 1; i >= 0; i--) {
            Building b = buildings.get(i);
            if (b.isDestroyed()) buildings.removeIndex(i);
        }
        for (int i = enemyBuildings.size - 1; i >= 0; i--) {
            Building b = enemyBuildings.get(i);
            if (b.isDestroyed()) enemyBuildings.removeIndex(i);
        }

        // Victoria: no quedan buildings enemigos
        if (!gameWon && enemyBuildings.size == 0) {
            gameWon = true;
            modeLabel.setText("¡VICTORIA! Eliminaste todas las bases enemigas.");
        }

    }

    // -------------------------------------------------------------------------------------
    // Entrada (cámara, selección, órdenes y modos de construcción)
    // -------------------------------------------------------------------------------------
    private void handleInput() {
        // IMPORTANTE: si el puntero está sobre la UI, no procesamos clicks del mundo
        if (stage.hit(Gdx.input.getX(), Gdx.input.getY(), true) != null) {
            // Aun así permitimos WASD cámara
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) ||
                Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
                return;
            }
        }

        // === Clic izquierdo: selección O confirmar acción de construcción ===
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Vector2 world = screenToWorld(Gdx.input.getX(), Gdx.input.getY());

            // --- Construcciones ---
            if (actionMode == ActionMode.BUILD_HQ || actionMode == ActionMode.BUILD_DEPOT
                || actionMode == ActionMode.BUILD_GARAGE || actionMode == ActionMode.BUILD_FORTIN) {

                boolean spaceFree = true;
                for (Building b : buildings) if (b.getBounds().contains(world)) { spaceFree = false; break; }
                if (!spaceFree) { modeLabel.setText("Espacio ocupado."); return; }

                BuildingKind kind;
                int cost;
                if (actionMode == ActionMode.BUILD_HQ) { kind = BuildingKind.HQ; cost = Building.COST_HQ; }
                else if (actionMode == ActionMode.BUILD_GARAGE) { kind = BuildingKind.GARAGE; cost = Building.COST_GARAGE; }
                else if (actionMode == ActionMode.BUILD_FORTIN) { kind = BuildingKind.FORTIN; cost = Building.COST_FORTIN; } // NUEVO
                else { kind = BuildingKind.DEPOT; cost = Building.COST_DEPOT; }

                if (resourceManager.spend(cost)) {
                    bulldozer.orderBuild(world, kind);
                    modeLabel.setText("Construyendo " + kind + "...");
                    actionMode = ActionMode.NONE;
                } else {
                    modeLabel.setText("Plástico insuficiente (" + cost + ").");
                }
                return;
            }

            // --- Encolar producción ---
            if (actionMode == ActionMode.ENQUEUE_HQ_SOLDIER || actionMode == ActionMode.ENQUEUE_HQ_SWEEPER
                || actionMode == ActionMode.ENQUEUE_GAR_TRUCK || actionMode == ActionMode.ENQUEUE_GAR_TANK) {

                // Busca edificio clickeado
                Building clicked = null;
                for (Building b : buildings) {
                    if (b.getBounds().contains(world)) { clicked = b; break; }
                }
                if (clicked == null) { modeLabel.setText("Clic sobre el edificio destino."); return; }

                // Verificación de tipo
                if ((actionMode == ActionMode.ENQUEUE_HQ_SOLDIER || actionMode == ActionMode.ENQUEUE_HQ_SWEEPER)
                    && clicked.getKind() != BuildingKind.HQ) {
                    modeLabel.setText("Debes clicar un HQ.");
                    return;
                }
                if ((actionMode == ActionMode.ENQUEUE_GAR_TRUCK || actionMode == ActionMode.ENQUEUE_GAR_TANK)
                    && clicked.getKind() != BuildingKind.GARAGE) {
                    modeLabel.setText("Debes clicar un Garaje.");
                    return;
                }

                if (!clicked.hasQueue()) { modeLabel.setText("Ese edificio no tiene cola."); return; }

                String order = (actionMode == ActionMode.ENQUEUE_HQ_SOLDIER) ? "SOLDIER"
                    : (actionMode == ActionMode.ENQUEUE_HQ_SWEEPER) ? "SWEEPER"
                    : (actionMode == ActionMode.ENQUEUE_GAR_TRUCK) ? "TRUCK"
                    : "TANK";

                clicked.getQueue().add(order);           // ¡Se puede encolar sin plástico!
                modeLabel.setText("Encolado: " + order + ". Cola ahora: " + clicked.getQueue().size());

                actionMode = ActionMode.NONE;
                return;
            }

            // Si no hay modo especial → iniciar selección por arrastre
            selectStart = world;
            selecting = true;
        }



        // Arrastre
        if (selecting) {
            selectEnd = screenToWorld(Gdx.input.getX(), Gdx.input.getY());
        }

        // Soltar clic izquierdo → cerrar selección
        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT) && selecting) {
            selecting = false;
            selectedUnits.clear();
            Rectangle selection = getSelectionRectangle();
            for (Unit u : playerUnits) {
                if (selection.contains(u.getPosition()))
                    selectedUnits.add(u);
            }
        }

        // === Clic derecho: mover unidades (formación) ===
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            if (selectedUnits.size > 0) {
                Vector2 dest = screenToWorld(Gdx.input.getX(), Gdx.input.getY());
                int i = 0;
                for (Unit u : selectedUnits) {
                    Vector2 offset = new Vector2((i % 3) * 40 - 40, (i / 3) * 40 - 40);
                    u.setTarget(dest.cpy().add(offset));
                    i++;
                }
            }
        }
    }

    private void handleCamera(float delta) {
        float baseSpeed = 700;
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT))
            baseSpeed = 1100; // boost temporal

        float speed = baseSpeed * delta;

        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) camera.position.y += speed;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) camera.position.y -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) camera.position.x -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) camera.position.x += speed;

        // Límites del mapa grande
        float halfW = camera.viewportWidth / 2f, halfH = camera.viewportHeight / 2f;

        if (camera.position.x < halfW) camera.position.x = halfW;
        if (camera.position.y < halfH) camera.position.y = halfH;
        if (camera.position.x > WORLD_W - halfW) camera.position.x = WORLD_W - halfW;
        if (camera.position.y > WORLD_H - halfH) camera.position.y = WORLD_H - halfH;

        camera.update();
    }

    private Rectangle getSelectionRectangle() {
        float x = Math.min(selectStart.x, selectEnd.x);
        float y = Math.min(selectStart.y, selectEnd.y);
        float w = Math.abs(selectStart.x - selectEnd.x);
        float h = Math.abs(selectStart.y - selectEnd.y);
        return new Rectangle(x, y, w, h);
    }

    private Vector2 screenToWorld(int x, int y) {
        Vector3 tmp = new Vector3(x, y, 0);
        camera.unproject(tmp);
        return new Vector2(tmp.x, tmp.y);
    }

    /** Genera ToyPiles y Mines aleatoriamente evitando la zona inicial. */
    private void generateWorld() {
        // Para evitar superposiciones “feas” entre objetos cercanos, guardamos celdas ocupadas muy groseramente
        HashSet<Long> usedCells = new HashSet<>();
        final float CELL = 64f; // grilla grosera para espaciar un poco

        // Helper: genera un punto random con margen y lejos del START_POS
        java.util.function.Supplier<Vector2> rndPoint = () -> {
            float margin = 80f; // evita bordes
            float x = MathUtils.random(margin, WORLD_W - margin);
            float y = MathUtils.random(margin, WORLD_H - margin);
            return new Vector2(x, y);
        };

        // Helper: celda discreta para espaciar
        java.util.function.Function<Vector2, Long> cellKey = (v) -> {
            int cx = MathUtils.floor(v.x / CELL);
            int cy = MathUtils.floor(v.y / CELL);
            return (((long) cx) << 32) | (cy & 0xffffffffL);
        };

        // 1) Generar ToyPiles
        int createdPiles = 0;
        int guard = 0;
        while (createdPiles < NUM_RANDOM_PILES && guard++ < NUM_RANDOM_PILES * 50) {
            Vector2 p = rndPoint.get();
            if (p.dst2(START_POS) < SAFE_RADIUS * SAFE_RADIUS) continue; // lejos del spawn
            Long key = cellKey.apply(p);
            if (usedCells.contains(key)) continue;
            usedCells.add(key);

            int amount = MathUtils.random(PILE_MIN_AMOUNT, PILE_MAX_AMOUNT);
            toyPiles.add(new ToyPile(p, amount));
            createdPiles++;
        }

        // 2) Generar Mines
        int createdMines = 0;
        guard = 0;
        while (createdMines < NUM_RANDOM_MINES && guard++ < NUM_RANDOM_MINES * 50) {
            Vector2 p = rndPoint.get();
            if (p.dst2(START_POS) < SAFE_RADIUS * SAFE_RADIUS) continue; // no minas en inicio
            Long key = cellKey.apply(p);
            if (usedCells.contains(key)) continue;
            usedCells.add(key);

            mines.add(new Mine(p));
            createdMines++;
        }

        // BONUS: deja unas pocas pilas cerca (pero fuera del SAFE_RADIUS) para empezar rápido
        for (int i = 0; i < 3; i++) {
            Vector2 dir = new Vector2(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f)).nor();
            Vector2 p = new Vector2(START_POS).mulAdd(dir, SAFE_RADIUS + MathUtils.random(70f, 200f));
            int amount = MathUtils.random(120, 220);
            toyPiles.add(new ToyPile(p, amount));
        }
    }

    private void doCombatTick(float delta) {
        // Enemigos disparan a jugador
        for (int i = 0; i < enemyUnits.size; i++) {
            Unit e = enemyUnits.get(i);
            // objetivo prioritario: unidades del jugador
            Unit targetU = findClosestInRange(e.getPosition(), e.getAttackRange(), playerUnits);
            if (targetU != null && e.canAttack()) {
                spawnBullet(e, targetU, true);
                e.commitAttack();
                continue;
            }
            // si no hay unidad en rango, edificios del jugador
            Building targetB = findClosestBuildingInRange(e.getPosition(), e.getAttackRange(), buildings);
            if (targetB != null && e.canAttack()) {
                spawnBullet(e, targetB, true);
                e.commitAttack();
            }
        }

        // Jugador dispara a enemigos
        for (int i = 0; i < playerUnits.size; i++) {
            Unit u = playerUnits.get(i);
            Unit targetE = findClosestInRange(u.getPosition(), u.getAttackRange(), enemyUnits);
            if (targetE != null && u.canAttack()) {
                spawnBullet(u, targetE, false);
                u.commitAttack();
                continue;
            }
            Building targetEB = findClosestBuildingInRange(u.getPosition(), u.getAttackRange(), enemyBuildings);
            if (targetEB != null && u.canAttack()) {
                spawnBullet(u, targetEB, false);
                u.commitAttack();
            }
        }
    }

    // Crea una bala hacia una unidad
    private void spawnBullet(Unit shooter, Unit target, boolean isEnemyBullet) {
        float speed = 560f; // velocidad de bala (ajustable)
        float damage = shooter.getAttackDamage();
        bullets.add(new Projectile(
            shooter.getPosition(), target.getPosition(),
            speed, damage, isEnemyBullet,
            target, null
        ));
    }

    // Crea una bala hacia un edificio
    private void spawnBullet(Unit shooter, Building target, boolean isEnemyBullet) {
        float speed = 540f;
        float damage = shooter.getAttackDamage();
        bullets.add(new Projectile(
            shooter.getPosition(), target.getPosition(),
            speed, damage, isEnemyBullet,
            null, target
        ));
    }

    private Unit findClosestInRange(Vector2 from, float range, Array<Unit> candidates) {
        Unit best = null;
        float best2 = range * range;
        for (int i = 0; i < candidates.size; i++) {
            Unit c = candidates.get(i);
            float d2 = from.dst2(c.getPosition());
            if (d2 <= best2) {
                best2 = d2;
                best = c;
            }
        }
        return best;
    }

    private Building findClosestBuildingInRange(Vector2 from, float range, Array<Building> buildingsList) {
        Building best = null;
        float best2 = range * range;
        for (int i = 0; i < buildingsList.size; i++) {
            Building b = buildingsList.get(i);
            float d2 = from.dst2(b.getPosition());
            if (d2 <= best2) {
                best2 = d2;
                best = b;
            }
        }
        return best;
    }

    // Crea 4 “campamentos” enemigos de distinta dificultad
    private void generateEnemyCamps() {
        // Helper para crear un soldado enemigo
        java.util.function.Function<Vector2, Unit> enemySoldier = (pos) -> {
            Unit s = new Unit(pos);
            s.setTag("SOLDIER");
            s.setEnemy(true);
            s.setStationary(true);
            s.hp = 100f;
            s.attackDamage = 10f;
            s.attackRange = 200f;
            return s;
        };
        // Helper tanque enemigo
        java.util.function.Function<Vector2, Unit> enemyTank = (pos) -> {
            Unit t = new Unit(pos);
            t.setTexture(new com.badlogic.gdx.graphics.Texture("tank.png"));
            t.setTag("TANK");
            t.setEnemy(true);
            t.setStationary(true);
            t.hp = 160f;
            t.attackDamage = 20f;
            t.attackRange = 200f;
            return t;
        };
        // Helper building enemigo
        java.util.function.BiFunction<Vector2, BuildingKind, Building> enemyBuild = (pos, kind) -> {
            String tex = "building_storage.png";
            if (kind == BuildingKind.HQ) tex = "building_hq.png";
            else if (kind == BuildingKind.GARAGE) tex = "building_garage.png";
            else if (kind == BuildingKind.FORTIN) tex = "building_fortin.png";
            Building b = new Building(pos, tex, kind, true);
            return b;
        };

        // 1) Campamento fácil: solo soldados
        placeSoldierGroup(new Vector2(2200, 5200), 6, enemySoldier);

        // 2) Soldados + Fortín + minas
        placeSoldierGroup(new Vector2(4800, 4600), 8, enemySoldier);
        enemyBuildings.add(enemyBuild.apply(new Vector2(4800, 4600), BuildingKind.FORTIN));
        // minas alrededor
        circleMines(new Vector2(4800, 4600), 220f, 8);

        // 3) Soldados + Fortín + Tanques
        placeSoldierGroup(new Vector2(5200, 2200), 10, enemySoldier);
        enemyUnits.add(enemyTank.apply(new Vector2(5260, 2200)));
        enemyUnits.add(enemyTank.apply(new Vector2(5140, 2240)));
        enemyBuildings.add(enemyBuild.apply(new Vector2(5200, 2200), BuildingKind.FORTIN));

        // 4) “Completo”: soldados + tanques + minas + buildings (HQ, DEPOT, GARAGE)
        Vector2 base = new Vector2(3400, 3400);
        placeSoldierGroup(base.cpy().add(0, 160), 10, enemySoldier);
        enemyUnits.add(enemyTank.apply(base.cpy().add(120, -40)));
        enemyUnits.add(enemyTank.apply(base.cpy().add(-120, -40)));
        enemyBuildings.add(enemyBuild.apply(base.cpy().add(0, 0), BuildingKind.HQ));
        enemyBuildings.add(enemyBuild.apply(base.cpy().add(160, 0), BuildingKind.DEPOT));
        enemyBuildings.add(enemyBuild.apply(base.cpy().add(-160, 0), BuildingKind.GARAGE));
        circleMines(base, 260f, 10);
    }

    // Coloca N soldados en una pequeña formación alrededor de “center”
    private void placeSoldierGroup(Vector2 center, int count,
                                   java.util.function.Function<Vector2, Unit> maker) {
        int cols = 4;
        int spacing = 40;
        for (int i = 0; i < count; i++) {
            int r = i / cols;
            int c = i % cols;
            Vector2 pos = new Vector2(center.x + (c - (cols/2)) * spacing,
                center.y + (r - 1) * spacing);
            enemyUnits.add(maker.apply(pos));
        }
    }

    // Pone minas en círculo para “defensa” del campamento
    private void circleMines(Vector2 center, float radius, int count) {
        for (int i = 0; i < count; i++) {
            float ang = (float)(2 * Math.PI * i / count);
            Vector2 p = new Vector2(center.x + radius * (float)Math.cos(ang),
                center.y + radius * (float)Math.sin(ang));
            mines.add(new Mine(p));
        }
    }


    @Override public void resize(int w, int h) {
        camera.setToOrtho(false, w, h);
        stage.getViewport().update(w, h, true);
    }

    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void dispose() {
        batch.dispose();
        shape.dispose();
        stage.dispose();
        if (bgTex != null) bgTex.dispose();
    }
}
