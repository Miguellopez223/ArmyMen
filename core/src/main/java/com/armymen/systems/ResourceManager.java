package com.armymen.systems;

public class ResourceManager {
    private int plastic;

    public ResourceManager(int initialPlastic) {
        this.plastic = initialPlastic;
    }

    public boolean spend(int amount) {
        if (plastic >= amount) {
            plastic -= amount;
            return true;
        }
        return false;
    }

    public void add(int amount) {
        plastic += amount;
    }

    public int getPlastic() {
        return plastic;
    }
}
