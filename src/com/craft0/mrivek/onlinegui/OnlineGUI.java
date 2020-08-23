package com.craft0.mrivek.onlinegui;

import java.util.ArrayList;
import java.util.List;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.earth2me.essentials.Essentials;

public class OnlineGUI extends JavaPlugin implements Listener {

	public OnlineGUI plugin = this;

	public List<Inventory> onlineInventories = new ArrayList<Inventory>();
	public static final int inventorySize = 54;
	public static final int emptySlots = 45;
	public Inventory inventory = Bukkit.createInventory(null, 54, "Online Players");
	public static String packageName = Bukkit.getServer().getClass().getPackage().getName();

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

		getCommand("online").setExecutor(new OnlineCommand(this));
		super.onEnable();
	}

	@Override
	public void onDisable() {
		super.onDisable();
	}

	public void open(Player player) {
		int numberOfOnlinePlayers = getServer().getOnlinePlayers().size();
		int neededInventories = Math.max(1, (int) Math.ceil((double) numberOfOnlinePlayers / emptySlots));
		for (int i = 0; i < neededInventories; i++) {
			onlineInventories.add(Bukkit.createInventory(null, inventorySize, "Online Players P" + i));
		}

		for (int i = 0; i < numberOfOnlinePlayers; i++) {
			int inventoryIndex = i / emptySlots;

			Inventory inventory = onlineInventories.get(inventoryIndex);
			inventory.clear();
			if (i == emptySlots) {
				inventory = onlineInventories.get(inventoryIndex++);
			}

			for (Player onlinePlayer : getServer().getOnlinePlayers()) {
				ItemStack playerHead = generatePlayerHead(onlinePlayer);
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

		player.openInventory(onlineInventories.get(0));
		return;
	}

	@SuppressWarnings("deprecation")
	public ItemStack generatePlayerHead(Player player) {
		ItemStack playerSkull;
		if (Bukkit.getBukkitVersion().contains("1.16") || Bukkit.getBukkitVersion().contains("1.15")
				|| Bukkit.getBukkitVersion().contains("1.14") || Bukkit.getBukkitVersion().contains("1.13")) {
			playerSkull = new ItemStack(Material.PLAYER_HEAD, 1);
		} else {
			playerSkull = new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 3);
		}

		SkullMeta playerSkullMeta = (SkullMeta) playerSkull.getItemMeta();

		if (packageName.contains("1.13") || packageName.contains("1.14") || packageName.contains("1.15")
				|| packageName.contains("1.16")) {
			playerSkullMeta.setOwningPlayer(player);

		} else {
			playerSkullMeta.setOwner(player.getName());
		}

		playerSkullMeta.setDisplayName("§f" + player.getName());
		List<String> lore = new ArrayList<String>();
		if (player.isOp()) {
			lore.add("§eOperator");
		}

		if (groupManager != null) {
			lore.add("§7" + getGroup(player));
		}

		if (essentialsX != null) {
			lore.add("§a$" + essentialsX.getUser(player).getMoney());

			if (essentialsX.getUser(player) != null && essentialsX.getUser(player).isAfk()) {
				lore.add("§7----------");
				lore.add("§cAFK");
			}

			if (essentialsX.getUser(player).isMuted()) {
				lore.add("§7----------");
				lore.add("§4MUTED");
			}
		}

		playerSkullMeta.setLore(lore);
		playerSkull.setItemMeta(playerSkullMeta);
		return playerSkull;
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
}
