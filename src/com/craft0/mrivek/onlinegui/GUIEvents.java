package com.craft0.mrivek.onlinegui;

import org.bukkit.Bukkit;
import org.bukkit.BanList;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import com.craft0.mrivek.onlinegui.OnlineGUI.ActionType;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

public final class GUIEvents implements Listener {
    private final OnlineGUI plugin;

    public GUIEvents(OnlineGUI plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) { plugin.scheduleRefresh(); }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getBungeeSupport() != null) {
            plugin.getBungeeSupport().forget(event.getPlayer().getUniqueId());
        }
        plugin.scheduleRefresh();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (InventoryViews.top(event.getView()).getHolder() instanceof GuiInventory) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = InventoryViews.top(event.getView());
        if (!(top.getHolder() instanceof GuiInventory)) { return; }
        // Include empty slots and the player's inventory to prevent item transfers.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player) || event.getClick() != ClickType.LEFT
                || event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) { return; }
        Player viewer = (Player) event.getWhoClicked();
        GuiInventory menu = (GuiInventory) top.getHolder();
        if (!viewer.getUniqueId().equals(menu.getViewer())) { return; }
        int slot = event.getRawSlot();
        if (menu.isServerList()) {
            if (plugin.getBungeeSupport() != null) {
                later(viewer, top, () -> plugin.getBungeeSupport().click(viewer, menu, slot));
            }
            return;
        }
        if (menu.isOnlineList()) {
            if (slot == 49) {
                later(viewer, top, viewer::closeInventory);
            } else if (slot == 48 && plugin.getBungeeSupport() != null
                    && viewer.hasPermission("onlinegui.servers")) {
                later(viewer, top, () -> plugin.getBungeeSupport().open(viewer));
            } else if ((slot == 45 || slot == 53) && top.getItem(slot) != null) {
                later(viewer, top, () -> plugin.openOnlineList(viewer,
                        menu.getPage() + (slot == 53 ? 1 : -1)));
            } else if (menu.getTarget(slot) != null && OnlineGUI.canModerate(viewer)) {
                later(viewer, top, () -> {
                    Player target = Bukkit.getPlayer(menu.getTarget(slot));
                    if (target != null && plugin.canSee(viewer, target)
                            && !viewer.getUniqueId().equals(target.getUniqueId())) {
                        new OptionsGUI(plugin, viewer, target).openInventory();
                    }
                });
            }
            return;
        }
        ActionType action = menu.getAction(slot);
        if (action != null) { later(viewer, top, () -> perform(viewer, menu, action)); }
    }

    // Open/close outside the inventory click transaction, as required by Bukkit.
    private void later(Player viewer, Inventory expected, Runnable action) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (viewer.isOnline() && InventoryViews.top(viewer.getOpenInventory()).equals(expected)) {
                action.run();
                viewer.playSound(viewer.getLocation(), "minecraft:ui.button.click", 1, 1);
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void perform(Player viewer, GuiInventory menu, ActionType action) {
        if (!OnlineGUI.canPerform(viewer, action)) {
            viewer.sendMessage("§cYou do not have permission to perform this action.");
            return;
        }
        if (action == ActionType.CLOSE) { viewer.closeInventory(); return; }
        Player target = Bukkit.getPlayer(menu.getTarget());
        if (target == null || !target.isOnline() || !plugin.canSee(viewer, target)
                || viewer.getUniqueId().equals(target.getUniqueId())) {
            viewer.sendMessage("§cThat player is no longer available.");
            viewer.closeInventory();
            return;
        }
        switch (action) {
            case KICK:
                target.kickPlayer("You have been kicked from the server.");
                break;
            case BAN:
                // Name bans retain the 1.13 API baseline.
                plugin.getServer().getBanList(BanList.Type.NAME)
                        .addBan(target.getName(), null, (java.util.Date) null, viewer.getName());
                target.kickPlayer("You have been banned from the server.");
                break;
            case MUTE:
                Essentials essentials = plugin.getEssentials();
                if (essentials == null) {
                    viewer.sendMessage("§cMuting requires EssentialsX.");
                    break;
                }
                User user = essentials.getUser(target);
                if (user != null) {
                    if (user.isMuted()) { viewer.sendMessage(target.getName() + " is already muted."); }
                    else { user.setMuted(true); }
                }
                break;
            default:
                break;
        }
        viewer.closeInventory();
    }
}
