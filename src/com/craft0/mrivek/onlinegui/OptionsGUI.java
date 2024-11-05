package com.craft0.mrivek.onlinegui;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class OptionsGUI implements Listener {

    public OnlineGUI plugin;
    public Inventory inventory;
    public Player player;
    public String title;
    public FileConfiguration config;
    public InventoryClickEvent inventoryClickedEvent;
    public Player person;

    public OptionsGUI(OnlineGUI plugin) {
        this.setPlugin(plugin);
        this.config = plugin.getConfig();
    }

    public OptionsGUI(OnlineGUI plugin, Player player, Player person) {
        this.setPlugin(plugin);
        this.player = player;
        this.title = plugin.getConfig().getString(plugin.getConfig().getString("gui-options.inventory.title"));
        this.config = plugin.getConfig();
        this.inventory = plugin.getServer().createInventory(player, 36,
                OnlineGUI.colorize(plugin.getConfig().getString("gui-options.inventory.title")));
        this.person = person;

        loadItems();

        player.sendMessage(person.toString());

    }

    public void loadItems() {
        for (String item : config.getConfigurationSection("gui-options.items").getKeys(false)) {
            ItemStack newItem = ItemBuilder.buildNewItem(
                    Material.valueOf(config.getString("gui-options.items." + item + ".material")),
                    config.getInt("gui-options.items." + item + ".amount"),
                    config.getString("gui-options.items." + item + ".display-name"),
                    config.getStringList("gui-options.items." + item + ".lore"), false);
            inventory.setItem(config.getInt("gui-options.items." + item + ".slot"), newItem);
        }
    }

    public void openInventory() {
        player.openInventory(inventory);
    }
    
    public Inventory getInventory() {
        return inventory;
    }

    public Player getViewer() {
        return player;
    }

    public Player getPerson() {
        return (Player) person;
    }

    public String getTitle() {
        return title;
    }

    public OnlineGUI getPlugin() {
        return plugin;
    }

    public void setPlugin(OnlineGUI plugin) {
        this.plugin = plugin;
    }
}
