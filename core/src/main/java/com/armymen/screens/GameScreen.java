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


    // === Selección con arrastre ===
    private boolean selecting = false;
    private Vector2 selectStart = new Vector2();
    private Vector2 selectEnd = new Vector2();

    // === HUD / UI ===
    private Stage stage;
    private Skin skin;
    private Label plasticLabel;
    private Label modeLabel;

    // Modo simple de acción (evitamos tocar muchas cosas)
//    private enum ActionMode { NONE, BUILD_STORAGE }
//    private ActionMode actionMode = ActionMode.NONE;
    //private enum ActionMode { NONE, BUILD_HQ, BUILD_DEPOT, BUILD_GARAGE, ENQUEUE_HQ_SOLDIER, ENQUEUE_HQ_SWEEPER, ENQUEUE_GAR_TRUCK, ENQUEUE_GAR_TANK }
    private ActionMode actionMode = ActionMode.NONE;

    // Debe existir en GameScreen:
    private enum ActionMode {
        NONE,
        BUILD_STORAGE,           // tu modo original (Depósito sencillo)
        BUILD_HQ, BUILD_DEPOT, BUILD_GARAGE,
        ENQUEUE_HQ_SOLDIER, ENQUEUE_HQ_SWEEPER,
        ENQUEUE_GAR_TRUCK, ENQUEUE_GAR_TANK
    }

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

        // Estado del juego
        this.buildings = new Array<>();
        this.playerUnits = new Array<>();
        this.selectedUnits = new Array<>();
        this.resourceManager = new ResourceManager(200); // plástico inicial

        // ======= Minas iniciales (mínimo indispensable) =======
        mines = new Array<>();
        // Colocamos 4-5 minas fijas para probar (puedes mover posiciones a gusto)
        mines.add(new Mine(new Vector2(650, 300)));
        mines.add(new Mine(new Vector2(900, 420)));
        mines.add(new Mine(new Vector2(1100, 360)));
        mines.add(new Mine(new Vector2(750, 550)));

        // Entidades base
        bulldozer = new Bulldozer(new Vector2(500, 500));
        playerUnits.add(bulldozer);

        // Algunos soldados
        playerUnits.add(new Unit(new Vector2(400, 300)));
        playerUnits.add(new Unit(new Vector2(600, 350)));
        playerUnits.add(new Unit(new Vector2(800, 300)));
        playerUnits.add(new Unit(new Vector2(1000, 500)));
        playerUnits.add(new Unit(new Vector2(1200, 250)));

        // Un edificio de ejemplo
        buildings.add(new Building(new Vector2(700, 400), "building_storage.png"));

        // ====== ToyPiles (fuentes de plástico para volquetas) ======
        toyPiles = new Array<>();
        toyPiles.add(new ToyPile(new Vector2(500, 250), 120));
        toyPiles.add(new ToyPile(new Vector2(900, 520), 200));
        toyPiles.add(new ToyPile(new Vector2(1300, 380), 160));

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

    // -------------------------------------------------------------------------------------
    // UI: crea botonera izquierda + indicadores
    // -------------------------------------------------------------------------------------
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
        final TextButton btnBuildStorage = new TextButton("Construir Almacen (-50)", skin);
        btnBuildStorage.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                actionMode = ActionMode.BUILD_STORAGE;
                modeLabel.setText("Modo: Construir Almacén (clic IZQ. en el mapa)");
            }
        });
        left.add(btnBuildStorage).row();

        // Nuevos: HQ / Depósito / Garaje (usan costos de Building)
        final TextButton btnBuildHQ = new TextButton("HQ (-" + com.armymen.entities.Building.COST_HQ + ")", skin);
        btnBuildHQ.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                actionMode = ActionMode.BUILD_HQ;
                modeLabel.setText("Modo: Construir HQ (clic mapa)");
            }
        });
        left.add(btnBuildHQ).row();

        final TextButton btnBuildDepot = new TextButton("Depósito (-" + com.armymen.entities.Building.COST_DEPOT + ")", skin);
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
        final TextButton btnCancel = new TextButton("Cancelar modo", skin);
        btnCancel.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                actionMode = ActionMode.NONE;
                modeLabel.setText("Modo: Ninguno");
            }
        });
        left.add(btnCancel).row();

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
    }

    @Override
    public void render(float delta) {
        update(delta);

        // Fondo
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // === Dibujo del mundo (SpriteBatch) ===
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // 1) Edificios
        for (Building b : buildings) b.render(batch);

        // 2) ToyPiles
        for (ToyPile p : toyPiles) p.render(batch);

        // 3) Minas
        for (Mine m : mines) m.render(batch);

        // 4) Unidades
        for (Unit u : playerUnits) u.render(batch);

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

        // HUD
        plasticLabel.setText("Plástico: " + resourceManager.getPlastic());

        // Edificios (incluye ingreso pasivo del DEPOT y colas de HQ/GARAGE)
        for (Building b : buildings) {
            b.update(delta, playerUnits, resourceManager);
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
            if (actionMode == ActionMode.BUILD_HQ || actionMode == ActionMode.BUILD_DEPOT || actionMode == ActionMode.BUILD_GARAGE) {
                // verificar espacio libre
                boolean spaceFree = true;
                for (Building b : buildings) if (b.getBounds().contains(world)) { spaceFree = false; break; }
                if (!spaceFree) { modeLabel.setText("Espacio ocupado."); return; }

                BuildingKind kind;
                int cost;
                if (actionMode == ActionMode.BUILD_HQ) { kind = BuildingKind.HQ; cost = Building.COST_HQ; }
                else if (actionMode == ActionMode.BUILD_GARAGE) { kind = BuildingKind.GARAGE; cost = Building.COST_GARAGE; }
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
        float speed = 400 * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) camera.position.y += speed;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) camera.position.y -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) camera.position.x -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) camera.position.x += speed;

        // Límites del mapa
        float MAP_WIDTH = 2000, MAP_HEIGHT = 2000;
        float halfW = camera.viewportWidth / 2f, halfH = camera.viewportHeight / 2f;

        if (camera.position.x < halfW) camera.position.x = halfW;
        if (camera.position.y < halfH) camera.position.y = halfH;
        if (camera.position.x > MAP_WIDTH - halfW) camera.position.x = MAP_WIDTH - halfW;
        if (camera.position.y > MAP_HEIGHT - halfH) camera.position.y = MAP_HEIGHT - halfH;

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
    }
}
