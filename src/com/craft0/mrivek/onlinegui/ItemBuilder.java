package com.craft0.mrivek.onlinegui;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;

public final class ItemBuilder {
    private final OnlineGUI plugin;

    public ItemBuilder(OnlineGUI plugin) { this.plugin = plugin; }

    static ItemStack nextPage() { return buildNewItem(Material.ARROW, 1, "&fNext Page", null, false); }
    static ItemStack previousPage() { return buildNewItem(Material.ARROW, 1, "&fPrevious Page", null, false); }
    static ItemStack close() { return buildNewItem(Material.BARRIER, 1, "&cClose", null, false); }

    public static ItemStack buildNewItem(Material material, int amount, String name,
            List<String> lore, boolean unbreakable) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            throw new IllegalArgumentException("Material has no item metadata: " + material);
        }
        meta.setDisplayName(OnlineGUI.colorize(name));
        if (lore != null) { meta.setLore(OnlineGUI.colorize(lore)); }
        meta.setUnbreakable(unbreakable);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack generatePlayerHead(Player viewer, Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName("§f" + player.getName());
        List<String> lore = new ArrayList<>();
        String group = plugin.getGroup(player);
        if (group != null) { lore.add("§7" + group); }
        Essentials essentials = plugin.getEssentials();
        if (essentials != null) {
            User user = essentials.getUser(player);
            if (user != null) {
                lore.add("§a$" + user.getMoney());
                if (user.isAfk()) {
                    lore.add("§7----------");
                    lore.add("§cAFK");
                }
                if (user.isMuted()) {
                    lore.add("§7----------");
                    lore.add("§4MUTED");
                }
            }
        }
        LuckPerms luckPerms = plugin.getLuckPermsAPI();
        if (luckPerms != null) {
            net.luckperms.api.model.user.User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                CachedMetaData data = user.getCachedData().getMetaData();
                if (data.getPrefix() != null) { lore.add(data.getPrefix()); }
                if (data.getSuffix() != null) { lore.add(data.getSuffix()); }
            }
        }
        if (OnlineGUI.canModerate(viewer) && !viewer.getUniqueId().equals(player.getUniqueId())) {
            lore.add("");
            lore.add("§7Left click to open command gui.");
        }
        meta.setLore(OnlineGUI.colorize(lore));
        head.setItemMeta(meta);
        return head;
    }
}
