package com.craft0.mrivek.onlinegui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.earth2me.essentials.Essentials;

public class OnlineGUI extends JavaPlugin implements Listener {

	public OnlineGUI plugin = this;

	public HashMap<UUID, List<Inventory>> onlineInventories = new HashMap<>();

	public static final int inventorySize = 54;
	public static final int emptySlots = 45;
	public static String packageName = Bukkit.getServer().getClass().getPackage().getName();
	public static final List<String> gameVersions = Arrays.asList("1.13", "1.13.1", "1.13.2", "1.14", "1.14.1",
			"1.14.2", "1.14.3", "1.14.4", "1.15", "1.15.1", "1.15.2", "1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5",
			"1.17", "1.17.1", "1.18", "1.18.1", "1.18.2", "1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4", "1.20",
			"1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2", "1.21.3");

	private GroupManager groupManager;
	private Essentials essentialsX;

	@Override
	public void onEnable() {
		final Plugin pluginGroupManager = getServer().getPluginManager().getPlugin("GroupManager");
		final Plugin pluginEssentialsX = getServer().getPluginManager().getPlugin("Essentials");

		if (pluginGroupManager != null && pluginGroupManager.isEnabled()) {
			groupManager = (GroupManager) pluginGroupManager;
		}

		if (pluginEssentialsX != null && pluginEssentialsX.isEnabled()) {
			essentialsX = (Essentials) pluginEssentialsX;
		}

		closeAllInventories();
		getServer().getPluginManager().registerEvents(new GUIEvents(this), this);
		getServer().getPluginManager().registerEvents(new OptionsGUI(this), this);

		getCommand("online").setExecutor(new OnlineCommand(this));
		super.onEnable();
	}

	@Override
	public void onDisable() {
		super.onDisable();
	}

	public void openOnlineList(Player player) {
		int numberOfOnlinePlayers = getServer().getOnlinePlayers().size();
		int neededInventories = Math.max(1, (int) Math.ceil((double) numberOfOnlinePlayers / emptySlots));

		List<Inventory> inventories = new ArrayList<>();
		for (int i = 0; i < neededInventories; i++) {
			Inventory newInv = Bukkit.createInventory(null, inventorySize, "Online Players P" + i);
			inventories.add(newInv);
		}
		onlineInventories.put(player.getUniqueId(), inventories);

		for (int i = 0; i < numberOfOnlinePlayers; i++) {
			int inventoryIndex = i / emptySlots;

			Inventory inventory = onlineInventories.get(player.getUniqueId()).get(0);
			inventory.clear();
			if (i == emptySlots) {
				inventory = onlineInventories.get(player.getUniqueId()).get(inventoryIndex++);
			}

			for (Player onlinePlayer : getServer().getOnlinePlayers()) {
				ItemStack playerHead = ItemBuilder.getInstance(this).generatePlayerHead(player, onlinePlayer);
				if (essentialsX != null) {
					if (essentialsX.getUser(onlinePlayer).isVanished()) {
						if (!player.hasPermission("essentials.vanish.see")) {
							continue;
						}
					}
				}

				if (!inventory.contains(playerHead)) {
					inventory.addItem(playerHead);
				}
			}

			if (inventoryIndex != neededInventories - 1) {
				inventory.setItem(53, ItemBuilder.NEXT_PAGE);
			}

			if (inventoryIndex >= 1) {
				inventory.setItem(45, ItemBuilder.PREVIOUS_PAGE);
			}

			inventory.setItem(49, ItemBuilder.CLOSE);
		}

		player.openInventory(onlineInventories.get(player.getUniqueId()).get(0));
		return;
	}

	public void closeAllInventories() {
		for (Player player : getServer().getOnlinePlayers()) {
			player.closeInventory();
		}
	}

	public String getGroup(final Player base) {
		final AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldPermissions(base);
		if (handler == null) {
			return null;
		}
		return handler.getGroup(base.getName());
	}

	public String getPrefix(final Player base) {
		final AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldPermissions(base);
		if (handler == null) {
			return null;
		}
		return handler.getUserPrefix(base.getName());
	}

	public String getSuffix(final Player base) {
		final AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldPermissions(base);
		if (handler == null) {
			return null;
		}
		return handler.getUserSuffix(base.getName());
	}

	public GroupManager getGroupManager() {
		return groupManager;
	}

	public Essentials getEssentials() {
		return essentialsX;
	}
}
