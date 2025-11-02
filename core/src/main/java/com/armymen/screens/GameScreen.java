package com.armymen.screens;

import com.armymen.MainGame;
import com.armymen.world.Unit;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.Input;

public class GameScreen implements Screen {

    private final MainGame game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shape;

    private Array<Unit> playerUnits;
    private Array<Unit> selectedUnits;

    // para la selección por arrastre
    private boolean selecting = false;
    private Vector2 selectStart = new Vector2();
    private Vector2 selectEnd = new Vector2();

    public GameScreen(MainGame game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.camera.setToOrtho(false, 1280, 720);
        this.batch = new SpriteBatch();
        this.shape = new ShapeRenderer();

        this.playerUnits = new Array<>();
        this.selectedUnits = new Array<>();

        // creamos varios soldados
        playerUnits.add(new Unit(new Vector2(400, 300)));
        playerUnits.add(new Unit(new Vector2(600, 350)));
        playerUnits.add(new Unit(new Vector2(800, 300)));
        playerUnits.add(new Unit(new Vector2(1000, 500)));
        playerUnits.add(new Unit(new Vector2(1200, 250)));
    }

    @Override
    public void render(float delta) {
        update(delta);

        // fondo blanco
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (Unit u : playerUnits)
            u.render(batch);
        batch.end();

        // dibujar el rectángulo de selección
        if (selecting) {
            shape.setProjectionMatrix(camera.combined);
            shape.begin(ShapeRenderer.ShapeType.Line);
            shape.setColor(Color.BLUE);
            Rectangle rect = getSelectionRectangle();
            shape.rect(rect.x, rect.y, rect.width, rect.height);
            shape.end();
        }

        // dibujar bordes sobre soldados seleccionados
        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.RED);
        for (Unit u : selectedUnits)
            shape.circle(u.getPosition().x, u.getPosition().y, 25);
        shape.end();
    }

    private void update(float delta) {
        handleCamera(delta);
        handleInput();

        for (Unit u : playerUnits)
            u.update(delta);
    }

    private void handleCamera(float delta) {
        float speed = 400 * delta;

        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))
            camera.position.y += speed;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))
            camera.position.y -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))
            camera.position.x -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT))
            camera.position.x += speed;

        // límites del mapa (tamaño 2000x2000 por ejemplo)
        float MAP_WIDTH = 2000;
        float MAP_HEIGHT = 2000;

        float halfWidth = camera.viewportWidth / 2;
        float halfHeight = camera.viewportHeight / 2;

        // Evitar que la cámara salga de los límites
        if (camera.position.x < halfWidth) camera.position.x = halfWidth;
        if (camera.position.y < halfHeight) camera.position.y = halfHeight;
        if (camera.position.x > MAP_WIDTH - halfWidth) camera.position.x = MAP_WIDTH - halfWidth;
        if (camera.position.y > MAP_HEIGHT - halfHeight) camera.position.y = MAP_HEIGHT - halfHeight;

        camera.update();
    }


    private void handleInput() {
        // clic izquierdo PRESIONADO: empezar selección
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            selectStart = screenToWorld(Gdx.input.getX(), Gdx.input.getY());
            selecting = true;
        }

        // mientras se arrastra
        if (selecting) {
            selectEnd = screenToWorld(Gdx.input.getX(), Gdx.input.getY());
        }

        // clic izquierdo SOLTADO: finalizar selección
        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT) && selecting) {
            selecting = false;
            selectedUnits.clear();

            Rectangle selection = getSelectionRectangle();
            for (Unit u : playerUnits) {
                if (selection.contains(u.getPosition()))
                    selectedUnits.add(u);
            }
        }

        // clic derecho: mover todos los seleccionados
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            if (selectedUnits.size > 0) {
                Vector2 dest = screenToWorld(Gdx.input.getX(), Gdx.input.getY());

                // separación entre unidades
                float spacing = 50f;

                // disposición en cuadrícula alrededor del punto destino
                int cols = (int) Math.ceil(Math.sqrt(selectedUnits.size));
                int rows = cols;

                int i = 0;
                for (Unit u : selectedUnits) {
                    int row = i / cols;
                    int col = i % cols;

                    float offsetX = (col - cols / 2f) * spacing;
                    float offsetY = (row - rows / 2f) * spacing;

                    Vector2 unitDest = new Vector2(dest.x + offsetX, dest.y + offsetY);
                    u.setTarget(unitDest);
                    i++;
                }
            }
        }
    }

    private Rectangle getSelectionRectangle() {
        float x = Math.min(selectStart.x, selectEnd.x);
        float y = Math.min(selectStart.y, selectEnd.y);
        float width = Math.abs(selectStart.x - selectEnd.x);
        float height = Math.abs(selectStart.y - selectEnd.y);
        return new Rectangle(x, y, width, height);
    }

    private Vector2 screenToWorld(int x, int y) {
        Vector3 tmp = new Vector3(x, y, 0);
        camera.unproject(tmp);
        return new Vector2(tmp.x, tmp.y);
    }

    @Override public void resize(int w, int h) { camera.setToOrtho(false, w, h); }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() {
        batch.dispose();
        shape.dispose();
    }
}
