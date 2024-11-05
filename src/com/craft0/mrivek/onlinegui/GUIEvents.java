package com.craft0.mrivek.onlinegui;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.BanList.Type;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.craft0.mrivek.onlinegui.OnlineGUI.ActionType;

import net.md_5.bungee.api.ChatColor;

public class GUIEvents implements Listener {

    private OnlineGUI plugin;
    private HashMap<UUID, List<Inventory>> onlineInventories;
    private FileConfiguration config;
    private OptionsGUI newGUIOptions;

    public GUIEvents(OnlineGUI plugin) {
        this.plugin = plugin;
        this.onlineInventories = plugin.onlineInventories;
        this.config = plugin.getConfig();
    }

    @EventHandler
    public void guiUpdateOnPlayerJoin(PlayerJoinEvent event) {
        Player joinedPlayer = event.getPlayer();
        ItemStack playerHead = ItemBuilder.getInstance(plugin).generatePlayerHead(joinedPlayer);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlineInventories.containsKey(onlinePlayer.getUniqueId())) {
                int numberOfOnlinePlayers = Bukkit.getOnlinePlayers().size();
                int neededInventories = Math.max(1,
                        (int) Math.ceil((double) numberOfOnlinePlayers / OnlineGUI.emptySlots));
                for (int i = 0; i < neededInventories; i++) {
                    int inventoryIndex = i / OnlineGUI.emptySlots;

                    Inventory inventory = onlineInventories.get(onlinePlayer.getUniqueId()).get(inventoryIndex);
                    if (!inventory.contains(playerHead)) {
                        inventory.addItem(playerHead);
                        onlinePlayer.updateInventory();
                    }
                }
            }
        }
    }

    @EventHandler
    public void guiUpdateOnPlayerQuit(PlayerQuitEvent event) {
        Player quitPlayer = event.getPlayer();
        ItemStack playerHead = ItemBuilder.getInstance(plugin).generatePlayerHead(quitPlayer);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlineInventories.containsKey(onlinePlayer.getUniqueId())) {
                int numberOfOnlinePlayers = Bukkit.getOnlinePlayers().size();
                int neededInventories = Math.max(1,
                        (int) Math.ceil((double) numberOfOnlinePlayers / OnlineGUI.emptySlots));
                for (int i = 0; i < neededInventories; i++) {
                    int inventoryIndex = i / OnlineGUI.emptySlots;
                    Inventory inventory = onlineInventories.get(onlinePlayer.getUniqueId()).get(inventoryIndex);

                    for (int slot = 0; slot < inventory.getSize(); slot++) {
                        ItemStack item = inventory.getItem(slot);

                        if (item == null)
                            continue;

                        if (item.getItemMeta().getDisplayName()
                                .equalsIgnoreCase(playerHead.getItemMeta().getDisplayName())) {
                            inventory.clear(slot);
                            onlinePlayer.updateInventory();

                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void guiClicked(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedInventory == null)
            return;

        if (clickedItem == null || clickedItem.equals(new ItemStack(Material.AIR)))
            return;

        if (event.getView().getTitle().startsWith("Online Players")) {
            event.setCancelled(true);

            if (clickedItem.getType().equals(Material.PLAYER_HEAD) && event.getClick().equals(ClickType.LEFT)) {

                if (!player.isOp() || !player.hasPermission("minecraft.command.kick")
                        || !player.hasPermission("minecraft:command.ban")) {
                    return;
                }

                if (ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).equals(player.getName())) {
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1);
                    return;
                }

                newGUIOptions = new OptionsGUI(plugin, player,
                        Bukkit.getPlayer(ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName())));
                newGUIOptions.openInventory();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                return;
            }

            if (clickedItem.equals(ItemBuilder.CLOSE)) {
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                return;
            }

            if (clickedItem.equals(ItemBuilder.NEXT_PAGE)) {
                player.openInventory(onlineInventories.get(player.getUniqueId())
                        .get(onlineInventories.get(player.getUniqueId()).indexOf(clickedInventory) + 1));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                return;
            }

            if (clickedItem.equals(ItemBuilder.PREVIOUS_PAGE)) {
                player.openInventory(onlineInventories.get(player.getUniqueId())
                        .get(onlineInventories.get(player.getUniqueId()).indexOf(clickedInventory) - 1));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                return;
            }
            return;
        }

        if (event.getClickedInventory().equals(newGUIOptions.getInventory())) {
            event.setCancelled(true);

            if (ItemBuilder.configItem(config, clickedItem) != null) {
                ActionType action = ActionType.valueOf(config
                        .getString("gui-options.items." + ItemBuilder.configItem(config, clickedItem) + ".action"));
                switch (action) {
                default:
                    player.closeInventory();
                    break;
                case CLOSE:
                    player.closeInventory();
                    break;
                case KICK:
                    newGUIOptions.getPerson().kickPlayer(null);
                    player.closeInventory();
                    break;
                case BAN:
                    plugin.getServer().getBanList(Type.NAME).addBan(newGUIOptions.getPerson().getName(), null, null,
                            null);
                    newGUIOptions.getPerson().kickPlayer("You have been banned from the server.");
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                    player.closeInventory();
                    break;
                case MUTE:
                    if (plugin.getEssentials() == null || plugin.getEssentials().isEnabled() == false) {
                        player.sendMessage("Essentials is DISABLED!");
                        player.closeInventory();
                        break;
                    }

                    if (plugin.getEssentials().getUser(newGUIOptions.getPerson()).isMuted()) {
                        player.sendMessage(newGUIOptions.getPerson().getName() + " is already muted.");
                        break;
                    }

                    plugin.getEssentials().getUser(newGUIOptions.getPerson()).setMuted(true);
                    player.closeInventory();
                    break;
                }
            }
        }
    }
}
