package com.craft0.mrivek.onlinegui;

import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import com.craft0.mrivek.onlinegui.OnlineGUI.ActionType;

public final class OptionsGUI {
    private final Player viewer;
    private final Inventory inventory;

    public OptionsGUI(OnlineGUI plugin, Player viewer, Player target) {
        this.viewer = viewer;
        int size = plugin.getConfig().getInt("gui-options.inventory.slots", 36);
        if (size < 9 || size > 54 || size % 9 != 0) {
            plugin.getLogger().warning("Invalid options inventory size; using 36 slots.");
            size = 36;
        }
        GuiInventory menu = GuiInventory.options(viewer.getUniqueId(), target.getUniqueId());
        inventory = plugin.getServer().createInventory(menu, size,
                OnlineGUI.colorize(plugin.getConfig().getString("gui-options.inventory.title", "&0&lOptions")));
        menu.setInventory(inventory);
        ConfigurationSection items = plugin.getConfig().getConfigurationSection("gui-options.items");
        if (items == null) { return; }
        for (String key : items.getKeys(false)) {
            ConfigurationSection item = items.getConfigurationSection(key);
            try {
                if (item == null) { throw new IllegalArgumentException("Expected an item section"); }
                ActionType action = ActionType.valueOf(item.getString("action", "CLOSE").toUpperCase(Locale.ROOT));
                int slot = item.getInt("slot", -1);
                if (slot < 0 || slot >= size || menu.getAction(slot) != null) {
                    throw new IllegalArgumentException("Invalid or duplicate slot: " + slot);
                }
                String materialName = item.getString("material", "BARRIER");
                // Support old configurations using the pre-flattening name.
                Material material = Material.matchMaterial("WOOL".equalsIgnoreCase(materialName)
                        ? "WHITE_WOOL" : materialName);
                if (material == null || material == Material.AIR || !material.isItem()) {
                    throw new IllegalArgumentException("Invalid item material: " + materialName);
                }
                int amount = item.getInt("amount", 1);
                if (amount < 1 || amount > material.getMaxStackSize()) {
                    throw new IllegalArgumentException("Invalid amount: " + amount);
                }
                if (!OnlineGUI.canPerform(viewer, action)) { continue; }
                inventory.setItem(slot, ItemBuilder.buildNewItem(material, amount,
                        item.getString("display-name", key), item.getStringList("lore"), false));
                menu.setAction(slot, action);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Skipping gui-options.items." + key + ": " + exception.getMessage());
            }
        }
    }

    public void openInventory() { viewer.openInventory(inventory); }
}
