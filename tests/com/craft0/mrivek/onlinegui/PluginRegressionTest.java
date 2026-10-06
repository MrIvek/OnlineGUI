package com.craft0.mrivek.onlinegui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PluginRegressionTest {
    private static Server server;
    private OnlineGUI plugin;
    private Player viewer;
    private YamlConfiguration config;
    private List<Inventory> created;

    @BeforeAll
    static void initializeBukkit() {
        server = mock(Server.class);
        when(server.getLogger()).thenReturn(Logger.getLogger("OnlineGUITests"));
        when(server.getName()).thenReturn("TestServer");
        when(server.getVersion()).thenReturn("test");
        when(server.getBukkitVersion()).thenReturn("test");
        Bukkit.setServer(server);
    }

    @BeforeEach
    void setup() throws Exception {
        reset(server);
        when(server.getMessenger()).thenReturn(mock(org.bukkit.plugin.messaging.Messenger.class));
        created = new ArrayList<>();
        config = new YamlConfiguration();
        ItemFactory factory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(factory);
        when(factory.getItemMeta(any(Material.class))).thenAnswer(call -> {
            ItemMeta meta = call.getArgument(0) == Material.PLAYER_HEAD
                    ? mock(SkullMeta.class) : mock(ItemMeta.class);
            when(meta.clone()).thenReturn(meta);
            return meta;
        });
        when(factory.isApplicable(any(ItemMeta.class), any(Material.class))).thenReturn(true);
        when(factory.asMetaFor(any(ItemMeta.class), any(Material.class))).thenAnswer(call -> call.getArgument(0));
        when(server.createInventory(any(InventoryHolder.class), anyInt(), anyString()))
                .thenAnswer(call -> {
                    Inventory inventory = mock(Inventory.class);
                    InventoryHolder holder = call.getArgument(0);
                    int size = call.getArgument(1);
                    Map<Integer, ItemStack> contents = new HashMap<>();
                    when(inventory.getHolder()).thenReturn(holder);
                    when(inventory.getSize()).thenReturn(size);
                    doAnswer(set -> { contents.put(set.getArgument(0), set.getArgument(1)); return null; })
                            .when(inventory).setItem(anyInt(), any(ItemStack.class));
                    when(inventory.getItem(anyInt())).thenAnswer(get -> contents.get(get.getArgument(0)));
                    created.add(inventory);
                    return inventory;
                });
        plugin = mock(OnlineGUI.class, CALLS_REAL_METHODS);
        doReturn(server).when(plugin).getServer();
        doReturn(config).when(plugin).getConfig();
        doReturn(Logger.getLogger("OnlineGUITests")).when(plugin).getLogger();
        Field builder = OnlineGUI.class.getDeclaredField("itemBuilder");
        builder.setAccessible(true);
        builder.set(plugin, new ItemBuilder(plugin));
        viewer = player("Viewer");
        when(viewer.canSee(any(Player.class))).thenReturn(true);
        when(viewer.isOnline()).thenReturn(true);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTask(any(), any(Runnable.class))).thenReturn(mock(BukkitTask.class));
        when(scheduler.runTaskLater(any(), any(Runnable.class), anyLong())).thenReturn(mock(BukkitTask.class));
    }

    private Player player(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return player;
    }

    private InventoryView view(Inventory inventory) {
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(inventory);
        when(viewer.getOpenInventory()).thenReturn(view);
        return view;
    }

    @Test
    void paginationHandlesMoreThanTwoPagesAndClampsAfterPlayersLeave() {
        List<Player> players = new ArrayList<>();
        for (int index = 0; index < 100; index++) { players.add(player(String.format("Player%03d", index))); }
        doReturn(players).when(server).getOnlinePlayers();
        plugin.openOnlineList(viewer, 0);
        Inventory first = created.get(0);
        assertEquals(players.get(44).getUniqueId(), ((GuiInventory) first.getHolder()).getTarget(44));
        assertNotNull(first.getItem(53));
        assertNull(first.getItem(45));
        plugin.openOnlineList(viewer, 1);
        assertEquals(players.get(45).getUniqueId(), ((GuiInventory) created.get(1).getHolder()).getTarget(0));
        plugin.openOnlineList(viewer, 2);
        Inventory last = created.get(2);
        assertEquals(players.get(99).getUniqueId(), ((GuiInventory) last.getHolder()).getTarget(9));
        assertNull(last.getItem(10));
        assertNull(last.getItem(53));
        doReturn(players.subList(0, 1)).when(server).getOnlinePlayers();
        plugin.openOnlineList(viewer, 2);
        assertEquals(0, ((GuiInventory) created.get(3).getHolder()).getPage());
    }

    @Test
    void hiddenPlayersDoNotConsumePaginationSlots() {
        Player hidden = player("Hidden");
        Player visible = player("Visible");
        when(viewer.canSee(hidden)).thenReturn(false);
        doReturn(Arrays.asList(hidden, visible)).when(server).getOnlinePlayers();
        plugin.openOnlineList(viewer);
        GuiInventory menu = (GuiInventory) created.get(0).getHolder();
        assertEquals(visible.getUniqueId(), menu.getTarget(0));
        assertNull(menu.getTarget(1));
    }

    @Test
    void emptySlotsBottomInventoryAndDraggingAreCancelled() {
        GuiInventory menu = GuiInventory.online(viewer.getUniqueId(), 0);
        Inventory inventory = mock(Inventory.class);
        when(inventory.getHolder()).thenReturn(menu);
        when(inventory.getSize()).thenReturn(54);
        InventoryView view = view(inventory);
        GUIEvents events = new GUIEvents(plugin);
        for (int slot : new int[] { 0, 60, -999 }) {
            InventoryClickEvent click = mock(InventoryClickEvent.class);
            when(click.getView()).thenReturn(view);
            when(click.getWhoClicked()).thenReturn(viewer);
            when(click.getClick()).thenReturn(ClickType.LEFT);
            when(click.getRawSlot()).thenReturn(slot);
            events.onClick(click);
            verify(click).setCancelled(true);
        }
        InventoryDragEvent drag = mock(InventoryDragEvent.class);
        when(drag.getView()).thenReturn(view);
        events.onDrag(drag);
        verify(drag).setCancelled(true);
        verify(server.getScheduler(), never()).runTask(any(), any(Runnable.class));
    }

    @Test
    void moderationChecksPermissionAgainWhenActionExecutes() {
        GuiInventory menu = GuiInventory.options(viewer.getUniqueId(), UUID.randomUUID());
        menu.setAction(11, OnlineGUI.ActionType.KICK);
        Inventory inventory = mock(Inventory.class);
        when(inventory.getHolder()).thenReturn(menu);
        when(inventory.getSize()).thenReturn(36);
        InventoryClickEvent click = mock(InventoryClickEvent.class);
        InventoryView clickView = view(inventory);
        when(click.getView()).thenReturn(clickView);
        when(click.getWhoClicked()).thenReturn(viewer);
        when(click.getClick()).thenReturn(ClickType.LEFT);
        when(click.getRawSlot()).thenReturn(11);
        new GUIEvents(plugin).onClick(click);
        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);
        verify(server.getScheduler()).runTask(eq(plugin), action.capture());
        action.getValue().run();
        verify(viewer).sendMessage(contains("permission"));
        verify(viewer, never()).closeInventory();
    }

    @Test
    void serverDiscoveryFiltersPermissionsAndRechecksBeforeConnecting() throws Exception {
        config.set("bungeecord.servers", Arrays.asList("lobby", "survival"));
        when(viewer.hasPermission("onlinegui.servers")).thenReturn(true);
        when(viewer.hasPermission("onlinegui.server.survival")).thenReturn(true);
        when(server.getPlayer(viewer.getUniqueId())).thenReturn(viewer);
        BungeeSupport bungee = new BungeeSupport(plugin);
        bungee.open(viewer);
        verify(viewer).sendPluginMessage(eq(plugin), eq("BungeeCord"), any(byte[].class));
        view(created.get(0));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeUTF("GetServers");
        output.writeUTF("survival, admin, lobby");
        bungee.onPluginMessageReceived("BungeeCord", viewer, bytes.toByteArray());
        GuiInventory menu = (GuiInventory) created.get(1).getHolder();
        assertEquals("survival", menu.getServer(0));
        assertNull(menu.getServer(1));
        when(viewer.hasPermission("onlinegui.server.survival")).thenReturn(false);
        bungee.click(viewer, menu, 0);
        verify(viewer, times(1)).sendPluginMessage(eq(plugin), eq("BungeeCord"), any(byte[].class));
        when(viewer.hasPermission("onlinegui.server.survival")).thenReturn(true);
        bungee.click(viewer, menu, 0);
        ArgumentCaptor<byte[]> messages = ArgumentCaptor.forClass(byte[].class);
        verify(viewer, times(2)).sendPluginMessage(eq(plugin), eq("BungeeCord"), messages.capture());
        java.io.DataInputStream connect = new java.io.DataInputStream(
                new java.io.ByteArrayInputStream(messages.getAllValues().get(1)));
        assertEquals("Connect", connect.readUTF());
        assertEquals("survival", connect.readUTF());
        bungee.close();
    }

    @Test
    void parsesEmptyAndDuplicateServerLists() {
        assertTrue(BungeeSupport.parseServers("").isEmpty());
        assertEquals(Arrays.asList("lobby", "survival"),
                BungeeSupport.parseServers(" survival, lobby, survival, "));
    }

    @Test
    void independentModeratorsActOnTheirOwnSelectedTargets() {
        Player secondViewer = player("SecondViewer");
        Player firstTarget = player("FirstTarget");
        Player secondTarget = player("SecondTarget");
        Player[] viewers = { viewer, secondViewer };
        Player[] targets = { firstTarget, secondTarget };
        for (int index = 0; index < 2; index++) {
            Player moderator = viewers[index];
            Player target = targets[index];
            when(moderator.isOnline()).thenReturn(true);
            when(moderator.hasPermission("onlinegui.kick")).thenReturn(true);
            when(moderator.canSee(target)).thenReturn(true);
            when(target.isOnline()).thenReturn(true);
            when(server.getPlayer(target.getUniqueId())).thenReturn(target);
            GuiInventory menu = GuiInventory.options(moderator.getUniqueId(), target.getUniqueId());
            menu.setAction(11, OnlineGUI.ActionType.KICK);
            Inventory inventory = mock(Inventory.class);
            when(inventory.getHolder()).thenReturn(menu);
            when(inventory.getSize()).thenReturn(36);
            InventoryView view = mock(InventoryView.class);
            when(view.getTopInventory()).thenReturn(inventory);
            when(moderator.getOpenInventory()).thenReturn(view);
            InventoryClickEvent click = mock(InventoryClickEvent.class);
            when(click.getView()).thenReturn(view);
            when(click.getWhoClicked()).thenReturn(moderator);
            when(click.getClick()).thenReturn(ClickType.LEFT);
            when(click.getRawSlot()).thenReturn(11);
            new GUIEvents(plugin).onClick(click);
        }
        ArgumentCaptor<Runnable> actions = ArgumentCaptor.forClass(Runnable.class);
        verify(server.getScheduler(), times(2)).runTask(eq(plugin), actions.capture());
        actions.getAllValues().forEach(Runnable::run);
        verify(firstTarget).kickPlayer(anyString());
        verify(secondTarget).kickPlayer(anyString());
        verify(viewer).closeInventory();
        verify(secondViewer).closeInventory();
    }

    @Test
    void essentialsAndLuckPermsLoreRetainsStatusesAndDistinctSuffix() {
        Player target = player("Target");
        com.earth2me.essentials.Essentials essentials = mock(com.earth2me.essentials.Essentials.class);
        com.earth2me.essentials.User essentialsUser = mock(com.earth2me.essentials.User.class);
        when(essentials.getUser(target)).thenReturn(essentialsUser);
        when(essentialsUser.getMoney()).thenReturn(new java.math.BigDecimal("12.50"));
        when(essentialsUser.isAfk()).thenReturn(true);
        when(essentialsUser.isMuted()).thenReturn(true);
        doReturn(essentials).when(plugin).getEssentials();
        net.luckperms.api.LuckPerms luckPerms = mock(net.luckperms.api.LuckPerms.class, RETURNS_DEEP_STUBS);
        net.luckperms.api.model.user.User user = luckPerms.getUserManager().getUser(target.getUniqueId());
        when(user.getCachedData().getMetaData().getPrefix()).thenReturn("&aPrefix");
        when(user.getCachedData().getMetaData().getSuffix()).thenReturn("&bSuffix");
        doReturn(luckPerms).when(plugin).getLuckPermsAPI();
        SkullMeta meta = mock(SkullMeta.class);
        when(meta.clone()).thenReturn(meta);
        when(server.getItemFactory().getItemMeta(Material.PLAYER_HEAD)).thenReturn(meta);
        new ItemBuilder(plugin).generatePlayerHead(viewer, target);
        ArgumentCaptor<List<String>> lore = ArgumentCaptor.forClass(List.class);
        verify(meta).setLore(lore.capture());
        assertTrue(lore.getValue().containsAll(Arrays.asList("§a$12.50", "§cAFK", "§4MUTED", "§aPrefix", "§bSuffix")));
    }

    @Test
    void proxyTimeoutRestoresLocalMenuWithoutShowingRemotePlayers() {
        when(viewer.hasPermission("onlinegui.servers")).thenReturn(true);
        doReturn(Arrays.asList(viewer)).when(server).getOnlinePlayers();
        BungeeSupport bungee = new BungeeSupport(plugin);
        bungee.open(viewer);
        view(created.get(0));
        ArgumentCaptor<Runnable> timeout = ArgumentCaptor.forClass(Runnable.class);
        verify(server.getScheduler()).runTaskLater(eq(plugin), timeout.capture(), eq(100L));
        timeout.getValue().run();
        verify(viewer).sendMessage(contains("did not respond"));
        assertTrue(((GuiInventory) created.get(1).getHolder()).isOnlineList());
    }
}
