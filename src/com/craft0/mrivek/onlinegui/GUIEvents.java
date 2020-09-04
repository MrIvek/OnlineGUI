package com.craft0.mrivek.onlinegui;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
	private HashMap<UUID, List<Inventory>> onlineInventories;

	public GUIEvents(OnlineGUI plugin) {
		this.plugin = plugin;
		this.onlineInventories = plugin.onlineInventories;
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
	}
}
