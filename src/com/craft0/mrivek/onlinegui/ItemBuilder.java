package com.craft0.mrivek.onlinegui;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemBuilder {

	public OnlineGUI plugin;

	public ItemBuilder(OnlineGUI plugin) {
		this.plugin = plugin;
	}

	public static final ItemStack NEXT_PAGE = buildNewItem(Material.ARROW, 1, "�fNext Page", null, false);
	public static final ItemStack PREVIOUS_PAGE = buildNewItem(Material.ARROW, 1, "�fPrevious Page", null, false);
	public static final ItemStack CLOSE = buildNewItem(Material.BARRIER, 1, "�cClose", null, false);

	public static ItemStack buildNewItem(Material material, int amount, String displayName, List<String> lore,
			boolean unbreakable) {
		ItemStack itemStack = new ItemStack(material, amount);
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setDisplayName(displayName);

		if (lore != null) {
			itemMeta.setLore(lore);
		}
		if (OnlineGUI.packageName.contains("1.13") || OnlineGUI.packageName.contains("1.14")
				|| OnlineGUI.packageName.contains("1.15") || OnlineGUI.packageName.contains("1.16")) {
			itemMeta.setUnbreakable(unbreakable);
		} else {
			unbreakable = false;
		}

		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public static ItemStack build(Material material, int amount, String displayName, List<String> lore,
			boolean unbreakable, ItemFlag flags, Map<Enchantment, Integer> enchantments) {
		ItemStack itemStack = new ItemStack(material, amount);
		ItemMeta itemMeta = itemStack.getItemMeta();

		itemMeta.setDisplayName(displayName);

		if (lore != null) {
			itemMeta.setLore(lore);
		}

		if (flags != null) {
			itemMeta.addItemFlags(flags);
		}

		if (enchantments != null) {
			for (Map.Entry<Enchantment, Integer> enchant : enchantments.entrySet()) {
				itemMeta.addEnchant(enchant.getKey(), enchant.getValue(), false);
			}
		}

		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

}