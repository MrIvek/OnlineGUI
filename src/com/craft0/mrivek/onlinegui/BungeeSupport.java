package com.craft0.mrivek.onlinegui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;

/** Uses BungeeCord's built-in messaging channel; installed on backend servers. */
final class BungeeSupport implements PluginMessageListener {
    private static final String CHANNEL = "BungeeCord";
    private final OnlineGUI plugin;
    private final Map<UUID, Inventory> waiting = new HashMap<>();
    private final Map<UUID, BukkitTask> timeouts = new HashMap<>();
    private List<String> servers = new ArrayList<>();
    private long fetchedAt;

    BungeeSupport(OnlineGUI plugin) { this.plugin = plugin; }

    void register() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    void close() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        timeouts.values().forEach(BukkitTask::cancel);
        timeouts.clear();
        waiting.clear();
    }

    void forget(UUID viewer) {
        waiting.remove(viewer);
        BukkitTask task = timeouts.remove(viewer);
        if (task != null) { task.cancel(); }
    }

    void open(Player viewer) {
        if (!viewer.hasPermission("onlinegui.servers")) { return; }
        forget(viewer.getUniqueId());
        if (fetchedAt != 0 && System.currentTimeMillis() - fetchedAt < 30000) {
            show(viewer, 0);
            return;
        }
        GuiInventory menu = GuiInventory.servers(viewer.getUniqueId(), 0);
        Inventory loading = Bukkit.createInventory(menu, 54, "Servers - Loading");
        menu.setInventory(loading);
        loading.setItem(22, ItemBuilder.buildNewItem(Material.CLOCK, 1, "&eLoading servers...", null, false));
        loading.setItem(49, ItemBuilder.close());
        boolean requestNeeded = waiting.isEmpty();
        waiting.put(viewer.getUniqueId(), loading);
        viewer.openInventory(loading);
        long seconds = Math.max(1, Math.min(30, plugin.getConfig().getInt("bungeecord.request-timeout-seconds", 5)));
        timeouts.put(viewer.getUniqueId(), plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Inventory expected = waiting.get(viewer.getUniqueId());
            forget(viewer.getUniqueId());
            if (viewer.isOnline() && InventoryViews.top(viewer.getOpenInventory()).equals(expected)) {
                viewer.sendMessage("§cBungeeCord did not respond. Check the proxy connection.");
                plugin.openOnlineList(viewer);
            }
        }, seconds * 20));
        if (requestNeeded) { send(viewer, "GetServers"); }
    }

    private boolean allowed(Player viewer, String server) {
        List<String> allowlist = plugin.getConfig().getStringList("bungeecord.servers");
        return viewer.hasPermission("onlinegui.servers")
                && (allowlist.isEmpty() || allowlist.contains(server))
                && (viewer.hasPermission("onlinegui.server.*")
                    || viewer.hasPermission("onlinegui.server." + server.toLowerCase(Locale.ROOT)));
    }

    private void show(Player viewer, int requestedPage) {
        List<String> visible = servers.stream().filter(server -> allowed(viewer, server)).collect(Collectors.toList());
        int pages = Math.max(1, (visible.size() + OnlineGUI.PLAYER_SLOTS - 1) / OnlineGUI.PLAYER_SLOTS);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        GuiInventory menu = GuiInventory.servers(viewer.getUniqueId(), page);
        Inventory inventory = Bukkit.createInventory(menu, 54, "Servers P" + (page + 1));
        menu.setInventory(inventory);
        int start = page * OnlineGUI.PLAYER_SLOTS;
        for (int index = start; index < Math.min(start + OnlineGUI.PLAYER_SLOTS, visible.size()); index++) {
            String server = visible.get(index);
            int slot = index - start;
            inventory.setItem(slot, ItemBuilder.buildNewItem(Material.ENDER_PEARL, 1, "&a" + server,
                    Arrays.asList("&7Click to join this server"), false));
            menu.setServer(slot, server);
        }
        if (visible.isEmpty()) {
            inventory.setItem(22, ItemBuilder.buildNewItem(Material.PAPER, 1,
                    "&7No servers available", null, false));
        }
        if (page > 0) { inventory.setItem(45, ItemBuilder.previousPage()); }
        inventory.setItem(48, ItemBuilder.buildNewItem(Material.PLAYER_HEAD, 1, "&fLocal players", null, false));
        inventory.setItem(49, ItemBuilder.close());
        if (page + 1 < pages) { inventory.setItem(53, ItemBuilder.nextPage()); }
        viewer.openInventory(inventory);
    }

    void click(Player viewer, GuiInventory menu, int slot) {
        if (!viewer.hasPermission("onlinegui.servers")) { return; }
        if (slot == 49) {
            forget(viewer.getUniqueId());
            viewer.closeInventory();
        } else if (slot == 48 && menu.getInventory().getItem(slot) != null) {
            plugin.openOnlineList(viewer);
        } else if ((slot == 45 || slot == 53) && menu.getInventory().getItem(slot) != null) {
            show(viewer, menu.getPage() + (slot == 53 ? 1 : -1));
        } else {
            String server = menu.getServer(slot);
            if (server != null && servers.contains(server) && allowed(viewer, server)) {
                viewer.closeInventory();
                viewer.sendMessage("§7Connecting to " + server + "...");
                send(viewer, "Connect", server);
            }
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player carrier, byte[] message) {
        if (!CHANNEL.equals(channel) || waiting.isEmpty()) { return; }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(message))) {
            if (!"GetServers".equals(input.readUTF())) { return; }
            servers = parseServers(input.readUTF());
            fetchedAt = System.currentTimeMillis();
            for (UUID id : new ArrayList<>(waiting.keySet())) {
                Player viewer = Bukkit.getPlayer(id);
                Inventory expected = waiting.get(id);
                forget(id);
                if (viewer != null && InventoryViews.top(viewer.getOpenInventory()).equals(expected)) {
                    show(viewer, 0);
                }
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Ignoring malformed BungeeCord server response.");
        }
    }

    static List<String> parseServers(String response) {
        return Arrays.stream(response.split(","))
                .map(String::trim).filter(name -> !name.isEmpty()).distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
    }

    private void send(Player carrier, String... values) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                for (String value : values) { output.writeUTF(value); }
            }
            carrier.sendPluginMessage(plugin, CHANNEL, bytes.toByteArray());
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not send BungeeCord message: " + exception.getMessage());
        }
    }
}
