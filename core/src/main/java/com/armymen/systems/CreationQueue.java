package com.armymen.systems;

import com.badlogic.gdx.utils.Array;

public class CreationQueue {
    private Array<String> items = new Array<>();

    public void add(String type) { items.add(type); }
    public boolean hasOrders() { return items.size > 0; }
    public String peek() { return items.size > 0 ? items.get(0) : null; }
    public String poll() { return items.size > 0 ? items.removeIndex(0) : null; }
    public int size() { return items.size; }
}
