package com.armymen.lwjgl3;

import com.armymen.MainGame;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import java.io.File;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        // Captura cualquier excepción no atrapada
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("=== Uncaught exception in thread: " + t.getName() + " ===");
            e.printStackTrace();
        });

        try {
            if (StartupHelper.startNewJvmIfRequired()) return;
        } catch (Throwable ignore) {}

        // Log de entorno + sanity-check de assets (recuerda: workingDir = ./assets)
        System.out.println("=== BOOT ===");
        System.out.println("cwd = " + new File(".").getAbsolutePath());
        check("uiskin.json");
        check("uiskin.atlas");
        check("soldier.png");
        check("bulldozer.png");
        check("building_storage.png");
        check("building_hq.png");
        check("building_garage.png");
        check("mine.png");
        check("sweeper.png");

        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("ArmyMen");
        configuration.useVsync(true);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        configuration.setWindowedMode(1280, 720);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.ANGLE_GLES20, 0, 0);

        try {
            new Lwjgl3Application(new MainGame(), configuration);
        } catch (Throwable ex) {
            System.err.println("=== Application crashed on startup ===");
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static void check(String relPath) {
        File f = new File(relPath);
        System.out.println("[asset?] " + relPath + " -> " + (f.exists() ? "OK" : "MISSING"));
    }
}
