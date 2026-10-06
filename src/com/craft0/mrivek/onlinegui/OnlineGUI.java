package com.craft0.mrivek.onlinegui;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import net.luckperms.api.LuckPerms;

public final class OnlineGUI extends JavaPlugin {
    static final int INVENTORY_SIZE = 54;
    static final int PLAYER_SLOTS = 45;
    private Plugin groupManager;
    private Essentials essentials;
    private LuckPerms luckPerms;
    private ItemBuilder itemBuilder;
    private boolean refreshPending;
    private BungeeSupport bungeeSupport;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        groupManager = enabledPlugin("GroupManager");
        Plugin essentialsPlugin = enabledPlugin("Essentials");
        if (essentialsPlugin != null && essentialsPlugin instanceof Essentials) {
            essentials = (Essentials) essentialsPlugin;
        }
        // Resolve optional API classes only when their plugin is installed.
        if (enabledPlugin("LuckPerms") != null) {
            RegisteredServiceProvider<LuckPerms> provider =
                    getServer().getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                luckPerms = provider.getProvider();
            }
        }
        itemBuilder = new ItemBuilder(this);
        if (getConfig().getBoolean("bungeecord.enabled", false)) {
            bungeeSupport = new BungeeSupport(this);
            bungeeSupport.register();
        }
        getServer().getPluginManager().registerEvents(new GUIEvents(this), this);
        Objects.requireNonNull(getCommand("online"), "Missing online command in plugin.yml")
                .setExecutor(new OnlineCommand(this));
    }

    private Plugin enabledPlugin(String name) {
        Plugin plugin = getServer().getPluginManager().getPlugin(name);
        return plugin != null && plugin.isEnabled() ? plugin : null;
    }

    @Override
    public void onDisable() {
        if (bungeeSupport != null) { bungeeSupport.close(); }
        for (Player player : getServer().getOnlinePlayers()) {
            if (InventoryViews.top(player.getOpenInventory()).getHolder() instanceof GuiInventory) {
                player.closeInventory();
            }
        }
    }

    public void openOnlineList(Player viewer) {
        openOnlineList(viewer, 0);
    }

    void openOnlineList(Player viewer, int requestedPage) {
        List<Player> players = getServer().getOnlinePlayers().stream()
                .filter(target -> canSee(viewer, target))
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        int pageCount = Math.max(1, (players.size() + PLAYER_SLOTS - 1) / PLAYER_SLOTS);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        GuiInventory menu = GuiInventory.online(viewer.getUniqueId(), page);
        Inventory inventory = Bukkit.createInventory(menu, INVENTORY_SIZE,
                "Online Players P" + (page + 1));
        menu.setInventory(inventory);
        int start = page * PLAYER_SLOTS;
        for (int index = start; index < Math.min(start + PLAYER_SLOTS, players.size()); index++) {
            Player target = players.get(index);
            int slot = index - start;
            inventory.setItem(slot, itemBuilder.generatePlayerHead(viewer, target));
            menu.setTarget(slot, target.getUniqueId());
        }
        if (page > 0) {
            inventory.setItem(45, ItemBuilder.previousPage());
        }
        inventory.setItem(49, ItemBuilder.close());
        if (bungeeSupport != null && viewer.hasPermission("onlinegui.servers")) {
            inventory.setItem(48, ItemBuilder.buildNewItem(org.bukkit.Material.COMPASS, 1,
                    "&aSwitch server", java.util.Arrays.asList("&7View servers you can join"), false));
        }
        if (page + 1 < pageCount) {
            inventory.setItem(53, ItemBuilder.nextPage());
        }
        viewer.openInventory(inventory);
    }

    boolean canSee(Player viewer, Player target) {
        if (!viewer.canSee(target)) {
            return false;
        }
        Essentials active = getEssentials();
        if (active != null) {
            User user = active.getUser(target);
            return user == null || !user.isVanished() || viewer.hasPermission("essentials.vanish.see");
        }
        return true;
    }

    // Coalesce joins/quits in the same tick and refresh only menus being viewed.
    void scheduleRefresh() {
        if (refreshPending) {
            return;
        }
        refreshPending = true;
        getServer().getScheduler().runTask(this, () -> {
            refreshPending = false;
            for (Player viewer : getServer().getOnlinePlayers()) {
                Inventory top = InventoryViews.top(viewer.getOpenInventory());
                if (top.getHolder() instanceof GuiInventory) {
                    GuiInventory menu = (GuiInventory) top.getHolder();
                    if (menu.isOnlineList()) {
                        openOnlineList(viewer, menu.getPage());
                    } else if (!menu.isServerList()) {
                        Player target = Bukkit.getPlayer(menu.getTarget());
                        if (target == null || !canSee(viewer, target)) {
                            viewer.closeInventory();
                        }
                    }
                }
            }
        });
    }

    public String getGroup(Player player) {
        if (groupManager == null || !groupManager.isEnabled()) {
            return null;
        }
        // GroupManager has no stable Maven API; isolate its optional classes.
        try {
            Object worlds = groupManager.getClass().getMethod("getWorldsHolder").invoke(groupManager);
            Object handler = worlds.getClass().getMethod("getWorldPermissions", Player.class)
                    .invoke(worlds, player);
            return handler == null ? null : (String) handler.getClass()
                    .getMethod("getGroup", String.class).invoke(handler, player.getName());
        } catch (ReflectiveOperationException exception) {
            getLogger().warning("Disabling GroupManager integration: " + exception.getMessage());
            groupManager = null;
            return null;
        }
    }

    public Essentials getEssentials() {
        return essentials != null && essentials.isEnabled() ? essentials : null;
    }

    public LuckPerms getLuckPermsAPI() { return luckPerms; }
    BungeeSupport getBungeeSupport() { return bungeeSupport; }

    static boolean canModerate(Player player) {
        return canPerform(player, ActionType.KICK) || canPerform(player, ActionType.BAN)
                || canPerform(player, ActionType.MUTE);
    }

    static boolean canPerform(Player player, ActionType action) {
        switch (action) {
            case KICK:
                return player.hasPermission("onlinegui.kick") || player.hasPermission("minecraft.command.kick");
            case BAN:
                return player.hasPermission("onlinegui.ban") || player.hasPermission("minecraft.command.ban");
            case MUTE:
                return player.hasPermission("onlinegui.mute") || player.hasPermission("essentials.mute");
            default:
                return true;
        }
    }

    public static List<String> colorize(List<String> lines) {
        return lines.stream().map(OnlineGUI::colorize).collect(Collectors.toList());
    }

    public static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    enum ActionType { KICK, BAN, MUTE, CLOSE }
}
