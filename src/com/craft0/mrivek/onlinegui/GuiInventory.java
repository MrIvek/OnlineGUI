package com.craft0.mrivek.onlinegui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import com.craft0.mrivek.onlinegui.OnlineGUI.ActionType;

/** Each menu owns its identity, viewer, and targets; item names are presentation only. */
final class GuiInventory implements InventoryHolder {
    private final UUID viewer;
    private final UUID target;
    private final int page;
    private final Map<Integer, UUID> targets = new HashMap<>();
    private final Map<Integer, ActionType> actions = new HashMap<>();
    private Inventory inventory;
    private boolean serverList;
    private final Map<Integer, String> servers = new HashMap<>();

    private GuiInventory(UUID viewer, UUID target, int page) {
        this.viewer = viewer;
        this.target = target;
        this.page = page;
    }

    static GuiInventory online(UUID viewer, int page) { return new GuiInventory(viewer, null, page); }
    static GuiInventory options(UUID viewer, UUID target) { return new GuiInventory(viewer, target, 0); }
    static GuiInventory servers(UUID viewer, int page) {
        GuiInventory menu = new GuiInventory(viewer, null, page);
        menu.serverList = true;
        return menu;
    }
    boolean isOnlineList() { return target == null && !serverList; }
    boolean isServerList() { return serverList; }
    void setServer(int slot, String server) { servers.put(slot, server); }
    String getServer(int slot) { return servers.get(slot); }
    UUID getViewer() { return viewer; }
    UUID getTarget() { return target; }
    UUID getTarget(int slot) { return targets.get(slot); }
    int getPage() { return page; }
    void setTarget(int slot, UUID id) { targets.put(slot, id); }
    void setAction(int slot, ActionType action) { actions.put(slot, action); }
    ActionType getAction(int slot) { return actions.get(slot); }
    void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
