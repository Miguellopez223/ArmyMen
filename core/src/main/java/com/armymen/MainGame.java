package com.armymen;

import com.armymen.screens.GameScreen;
import com.badlogic.gdx.Game;

public class MainGame extends Game {

    @Override
    public void create() {
        setScreen(new GameScreen(this));
    }
}
