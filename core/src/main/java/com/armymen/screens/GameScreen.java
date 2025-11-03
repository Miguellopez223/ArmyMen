package com.armymen.screens;

import com.armymen.MainGame;
import com.armymen.entities.Building;
import com.armymen.entities.Bulldozer;
import com.armymen.entities.Unit;
import com.armymen.systems.ResourceManager;

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
    private enum ActionMode { NONE, BUILD_STORAGE }
    private ActionMode actionMode = ActionMode.NONE;

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

        // Asegúrate de tener este skin en assets: uiskin.json + uiskin.png
        // Puedes usar el skin por defecto de LibGDX: https://github.com/czyzby/gdx-skins/tree/master/uiskin
        // donde creas el Skin:
        skin = new Skin(
            Gdx.files.internal("uiskin.json"),
            new TextureAtlas(Gdx.files.internal("uiskin.atlas"))
        );


        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Panel izquierdo (acciones)
        Table left = new Table(skin);
        left.defaults().pad(4).fillX();

        // Título
        Label title = new Label("Acciones", skin, "default");
        left.add(title).row();

        // Botón: Construir Almacén (usa tu bulldozer.orderBuild)
        final TextButton btnBuildStorage = new TextButton("Construir Almacen (-50)", skin);
        btnBuildStorage.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                actionMode = ActionMode.BUILD_STORAGE;
                modeLabel.setText("Modo: Construir Almacén (clic IZQ. en el mapa)");
            }
        });
        left.add(btnBuildStorage).row();

        // Separador
        left.add(new Label(" ", skin)).row();

        // Botón: Salir de modo
        final TextButton btnCancel = new TextButton("Cancelar modo", skin);
        btnCancel.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                actionMode = ActionMode.NONE;
                modeLabel.setText("Modo: Ninguno");
            }
        });
        left.add(btnCancel).row();

        // Panel superior derecho: indicadores
        Table top = new Table(skin);
        top.defaults().pad(6);

        plasticLabel = new Label("Plástico: 0", skin);
        modeLabel = new Label("Modo: Ninguno", skin);

        // Layout general: left a la izquierda, top arriba-derecha
        root.left().top();
        root.add(left).left().top().width(220f);

        root.add().expand(); // espacio central

        // Columna derecha (arriba): indicadores
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

        // Dibujo del mundo
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (Building b : buildings) b.render(batch);
        for (Unit u : playerUnits) u.render(batch);
        batch.end();

        // Rectángulo de selección (mientras arrastras)
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

        // UI
        stage.act(delta);
        stage.draw();
    }

    private void update(float delta) {
        handleCamera(delta);
        handleInput();

        for (Unit u : playerUnits) u.update(delta);

        // Evitar atravesar edificios (empujón suave)
        for (Unit u : playerUnits) {
            for (Building b : buildings) {
                if (b.getBounds().contains(u.getPosition())) {
                    Vector2 dir = new Vector2(u.getPosition()).sub(b.getPosition()).nor();
                    u.getPosition().mulAdd(dir, 3f); // retrocede un poco
                }
            }
        }

        // Actualizar HUD
        plasticLabel.setText("Plástico: " + resourceManager.getPlastic());
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

            if (actionMode == ActionMode.BUILD_STORAGE) {
                // Construcción directa: no requiere tener seleccionado el bulldozer
                boolean spaceFree = true;
                for (Building b : buildings) {
                    if (b.getBounds().contains(world)) { spaceFree = false; break; }
                }

                if (!spaceFree) {
                    modeLabel.setText("Espacio ocupado. Elige otro lugar.");
                    return;
                }

                // Coste fijo: 50 (como ya usas en tu right-click)
                if (resourceManager.spend(50)) {
                    // Ordenar al bulldozer que vaya a construir
                    bulldozer.orderBuild(world);
                    modeLabel.setText("Construyendo... (Bulldozer en camino)");
                } else {
                    modeLabel.setText("Plástico insuficiente (50).");
                }

                // Tras click, salimos del modo construcción
                actionMode = ActionMode.NONE;
                return;
            }

            // Si NO estamos en modo de construcción → iniciar selección por arrastre
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
