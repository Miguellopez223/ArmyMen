package com.armymen.model;

public enum Faction {
    TEAM_A,  // Jugador (verde)
    TEAM_B;  // Enemigo (azul/vino/amarillo)

    public boolean isEnemyOf(Faction other) {
        if (other == null) return false;
        return this != other;
    }
}
