package com.craft0.mrivek.onlinegui;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

/** InventoryView became an interface in 1.21; reflection avoids incompatible invoke bytecode. */
final class InventoryViews {
    private static final Method TOP = topMethod();

    private InventoryViews() { }

    private static Method topMethod() {
        try {
            return InventoryView.class.getMethod("getTopInventory");
        } catch (NoSuchMethodException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    static Inventory top(Object view) {
        try {
            return (Inventory) TOP.invoke(view);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not access the top inventory", exception);
        }
    }
}
