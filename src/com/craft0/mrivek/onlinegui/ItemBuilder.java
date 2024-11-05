package com.craft0.mrivek.onlinegui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.earth2me.essentials.Essentials;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;

public class ItemBuilder {

    public OnlineGUI plugin;
    private GroupManager groupManager;
    private Essentials essentialsX;
    private LuckPerms luckPerms;
    private static ItemBuilder itemBuilder;

    public ItemBuilder(OnlineGUI plugin) {
        this.plugin = plugin;
        this.groupManager = plugin.getGroupManager();
        this.essentialsX = plugin.getEssentials();
        this.luckPerms = plugin.getLuckPermsAPI();
    }

    public static final ItemStack NEXT_PAGE = buildNewItem(Material.ARROW, 1, "§fNext Page", null, false);
    public static final ItemStack PREVIOUS_PAGE = buildNewItem(Material.ARROW, 1, "§fPrevious Page", null, false);
    public static final ItemStack CLOSE = buildNewItem(Material.BARRIER, 1, "§cClose", null, false);
    public static final ItemStack BAN = buildNewItem(Material.RED_WOOL, 1, "§6Ban", null, false);
    public static final ItemStack KICK = buildNewItem(Material.FEATHER, 1, "§6Kick", null, false);

    public static ItemStack buildNewItem(Material material, int amount, String displayName, List<String> lore,
            boolean unbreakable) {
        ItemStack itemStack = new ItemStack(material, amount);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(OnlineGUI.colorize(displayName));

        if (lore != null) {
            itemMeta.setLore(OnlineGUI.colorize(lore));
        }
        for (int i = 0; i < OnlineGUI.gameVersions.size(); i++) {
            if (OnlineGUI.packageName.matches(OnlineGUI.gameVersions.get(i))) {
                itemMeta.setUnbreakable(unbreakable);
            } else {
                unbreakable = false;
            }
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

    @SuppressWarnings("deprecation")
    public ItemStack generatePlayerHead(Player player) {
        ItemStack playerSkull;
        if (OnlineGUI.gameVersions.contains(Bukkit.getBukkitVersion().replaceAll("-R0.1-SNAPSHOT", ""))) {
            playerSkull = new ItemStack(Material.PLAYER_HEAD, 1);
        } else {
            playerSkull = new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 3);
        }

        SkullMeta playerSkullMeta = (SkullMeta) playerSkull.getItemMeta();
        if (OnlineGUI.packageName.matches(OnlineGUI.gameVersions.iterator().next())) {
            playerSkullMeta.setOwningPlayer(player);

        } else {
            playerSkullMeta.setOwner(player.getName());
        }

        playerSkullMeta.setDisplayName("§f" + player.getName());
        List<String> lore = new ArrayList<String>();
        if (groupManager != null) {
            lore.add("§7" + plugin.getGroup(player));
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

        if (luckPerms != null) {
            User user = luckPerms.getUserManager().getUser(player.getName());
            if (user.getCachedData().getMetaData().getPrefix() != null) {
                lore.add(user.getCachedData().getMetaData().getPrefix());
            }

            if (user.getCachedData().getMetaData().getSuffix() != null) {
                lore.add(user.getCachedData().getMetaData().getPrefix());
            }
        }

        playerSkullMeta.setLore(OnlineGUI.colorize(lore));
        playerSkull.setItemMeta(playerSkullMeta);
        return playerSkull;
    }

    @SuppressWarnings("deprecation")
    public ItemStack generatePlayerHead(Player invViewer, Player player) {
        ItemStack playerSkull;
        if (OnlineGUI.gameVersions.contains(Bukkit.getBukkitVersion().replaceAll("-R0.1-SNAPSHOT", ""))) {
            playerSkull = new ItemStack(Material.PLAYER_HEAD, 1);
        } else {
            playerSkull = new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 3);
        }

        SkullMeta playerSkullMeta = (SkullMeta) playerSkull.getItemMeta();
        for (int i = 0; i < OnlineGUI.gameVersions.size(); i++) {
            if (OnlineGUI.packageName.matches(OnlineGUI.gameVersions.get(i))) {
                playerSkullMeta.setOwningPlayer(player);
            } else {
                playerSkullMeta.setOwner(player.getName());
            }
        }

        playerSkullMeta.setDisplayName("§f" + player.getName());
        List<String> lore = new ArrayList<String>();
        if (groupManager != null) {
            lore.add("§7" + plugin.getGroup(player));
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

        if (luckPerms != null) {
            User user = luckPerms.getUserManager().getUser(player.getName());
            if (user.getCachedData().getMetaData().getPrefix() != null) {
                lore.add(user.getCachedData().getMetaData().getPrefix());
            }

            if (user.getCachedData().getMetaData().getSuffix() != null) {
                lore.add(user.getCachedData().getMetaData().getPrefix());
            }
        }

        if (invViewer.isOp() && player.getName() != invViewer.getName()) {
            lore.add("");
            lore.add("§7Left click to open command gui.");
        }

        playerSkullMeta.setLore(OnlineGUI.colorize(lore));
        playerSkull.setItemMeta(playerSkullMeta);
        return playerSkull;

    }

    public static String configItem(FileConfiguration config, ItemStack itemCompared) {

        for (String item : config.getConfigurationSection("gui-options.items").getKeys(false)) {
            ItemStack newConfigItem = ItemBuilder.buildNewItem(
                    Material.valueOf(config.getString("gui-options.items." + item + ".material")),
                    config.getInt("gui-options.items." + item + ".amount"),
                    config.getString("gui-options.items." + item + ".display-name"),
                    config.getStringList("gui-options.items." + item + ".lore"), false);
            if (newConfigItem.isSimilar(itemCompared)) {
                return item;
            }
        }

        return null;
    }

    public static ItemBuilder getInstance(OnlineGUI plugin) {
        if (itemBuilder == null) {
            itemBuilder = new ItemBuilder(plugin);
            return itemBuilder;
        }

        return itemBuilder;
    }

}