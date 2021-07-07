package com.craft0.mrivek.onlinegui;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;

public class OptionsGUI implements Listener {
	
	private OnlineGUI plugin;
	private Inventory inventory;
	private Player player;
	private String title;

	public OptionsGUI(OnlineGUI plugin) {
		this.setPlugin(plugin);
	}
	
	public OptionsGUI(OnlineGUI plugin, Player player, String title) {
		this.setPlugin(plugin);
		this.player = player;
		this.title = title;
		this.inventory = plugin.getServer().createInventory(player, 36, title + "'s Options");
		
		loadItems();
	}
	
	public void loadItems() {
		inventory.setItem(11, ItemBuilder.KICK);
		inventory.setItem(15, ItemBuilder.BAN);
		inventory.setItem(35, ItemBuilder.CLOSE);
	}
	
	public void openInventory() {
		player.openInventory(inventory);
	}
	
	@EventHandler
	public void onClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		Inventory clickedInventory = event.getClickedInventory();
		ItemStack clickedItem = event.getCurrentItem();

		if (clickedInventory == null)
			return;

		if (clickedItem == null || clickedItem.equals(new ItemStack(Material.AIR)))
			return;
		
		if(event.getView().getTitle().endsWith("Options")) {
			event.setCancelled(true);
			
			if(clickedItem.equals(ItemBuilder.KICK)) {
				player.performCommand("kick " + ChatColor.stripColor(event.getView().getTitle().replaceAll("'s Options", "")));
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
				player.closeInventory();
				return;
			}
			
			if(clickedItem.equals(ItemBuilder.BAN)) {
				player.performCommand("ban " + ChatColor.stripColor(event.getView().getTitle().replaceAll("'s Options", "")));
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
				player.closeInventory();
				return;
			}
			
			if (clickedItem.equals(ItemBuilder.CLOSE)) {
				player.closeInventory();
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
				return;
			}
		}
	}
	
	public Player getViewer() {
		return player;
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
