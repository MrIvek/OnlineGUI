package com.craft0.mrivek.onlinegui;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GUIEvents implements Listener {

	private OnlineGUI plugin;

	public GUIEvents(OnlineGUI plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void guiUpdateOnPlayerJoin(PlayerJoinEvent event) {
		Inventory inventory = plugin.onlineInventories.get(0);

		ItemStack playerHead = plugin.generatePlayerHead(event.getPlayer());
		if (!inventory.contains(playerHead)) {
			inventory.addItem(playerHead);
		}

		if (inventory.getViewers().size() >= 1) {
			for (HumanEntity viewer : inventory.getViewers()) {
				((Player) viewer).updateInventory();
			}
		}
	}

	@EventHandler
	public void guiUpdateOnPlayerQuit(PlayerQuitEvent event) {
		ItemStack playerHead = plugin.generatePlayerHead(event.getPlayer());

		for (int i = 0; i < OnlineGUI.emptySlots; i++) {

			int inventoryIndex = i / OnlineGUI.emptySlots;
			Inventory inventory = plugin.onlineInventories.get(inventoryIndex);

			ItemStack item = inventory.getItem(i);
			if (item != null) {
				if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
					if (item.isSimilar(playerHead)) {
						inventory.remove(playerHead);
					}
				}
			}

			for (HumanEntity viewer : inventory.getViewers()) {
				((Player) viewer).updateInventory();
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

			if (clickedItem.equals(ItemBuilder.CLOSE)) {
				player.closeInventory();
				return;
			}

			if (clickedItem.equals(ItemBuilder.NEXT_PAGE)) {
				player.openInventory(
						plugin.onlineInventories.get(plugin.onlineInventories.indexOf(clickedInventory) + 1));
				return;
			}

			if (clickedItem.equals(ItemBuilder.PREVIOUS_PAGE)) {
				player.openInventory(
						plugin.onlineInventories.get(plugin.onlineInventories.indexOf(clickedInventory) - 1));
				return;
			}
			return;
		}

	}

}
