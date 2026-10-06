# OnlineGUI

OnlineGUI displays online players in an inventory using `/online` (aliases: `/players`, `/list`). Player heads show optional GroupManager groups, EssentialsX balances/AFK/mute status, and LuckPerms prefixes and suffixes.

This working version is **1.11.0-SNAPSHOT**. The previous source release was **1.10.1**, with an explicit Minecraft version list ending at **1.21.3**.

## Minecraft compatibility

The implementation uses the Bukkit API instead of an exact Minecraft-version whitelist. The default build targets the **1.13.2 API and Java 8 bytecode**, while the `latest` profile checks **26.3**, the current stable Java Edition release as of October 6, 2026. This covers the API transition introduced in 1.21 with an inventory-view adapter. Releases after 1.21.3 (including later 1.21 releases and 26.1–26.3) no longer fall back to the removed `SKULL_ITEM` material.

API compilation and mocked regression tests are not live server certification. Check the plugin on a staging server before deployment, including your installed versions of the optional integrations. Minecraft versions below 1.13 are outside this build's API baseline. The original README's 1.8–1.12 claims were inconsistent with the existing `api-version: 1.13` and modern material references.

Latest-release references: [Mojang 26.3 release notes](https://www.minecraft.net/ko-kr/article/minecraft-java-edition-26-3), [Spigot API](https://hub.spigotmc.org/javadocs/bukkit/), [Paper Java requirements](https://docs.papermc.io/paper/getting-started/).

## Build

Use Maven 3.9+ and JDK 17 or newer for the default build and test tooling:

```text
mvn clean package
```

The plugin is `target/onlinegui-1.11.0-SNAPSHOT.jar`. Server APIs and optional plugin dependencies are provided dependencies; they are not bundled. Existing server jars under `src/.../resources` and compiled files under `bin` are retained as historical files and are excluded from the build.

To compile and test against Minecraft 26.3, use JDK 25:

```text
mvn -Platest package
```

That validation build is written to `target/latest/` and requires Java 25. The default jar retains Java 8 bytecode for older supported servers. Always use the Java version required by your Minecraft server.

## Moderation and optional integrations

Install EssentialsX, GroupManager, and/or LuckPerms on the backend server to enable their respective lore. EssentialsX also provides mute actions. Install versions compatible with your server; no integration is required for the basic player list.

Click another player's head to open the moderation menu if you have at least one action permission. Permissions are checked separately for each action and again when executing it:

| Action | OnlineGUI permission | Existing permission also accepted |
| --- | --- | --- |
| Kick | `onlinegui.kick` | `minecraft.command.kick` |
| Ban | `onlinegui.ban` | `minecraft.command.ban` |
| Mute | `onlinegui.mute` | `essentials.mute` |

The OnlineGUI moderation nodes default to operators. GUI actions have the same direct kick/ban/mute behavior as before; they do not execute command aliases. Hidden players are filtered using Bukkit visibility and EssentialsX vanish status. Player identity and moderation state are stored per inventory using UUIDs.

Options menu size, materials, amounts, and slots come from `config.yml`. Invalid entries are skipped with a warning. Existing `WOOL` configuration values are accepted as `WHITE_WOOL`.

## BungeeCord server switching

Install OnlineGUI on each Bukkit/Spigot/Paper backend server, not in the proxy's plugin folder. Enable this in the backend's `plugins/OnlineGUI/config.yml`:

```yaml
bungeecord:
  enabled: true
  servers: [lobby, survival, minigames]
  request-timeout-seconds: 5
```

Server names must match the BungeeCord configuration exactly. Leave `servers: []` to discover all proxy servers, then filter them by permission. Restart the backend after changing this configuration.

The compass in `/online` opens a paginated server switcher. A player needs `onlinegui.servers` (true by default) and a destination permission such as `onlinegui.server.survival`. Destination permission names use lowercase. `onlinegui.server.*` grants every allowed destination and defaults to operators. Give non-operators only the destination permissions they should use, through LuckPerms or GroupManager. The proxy's own restricted-server permissions still apply.

Only the current server's player list is displayed. Other servers appear as destinations, without their players' names. Selecting a destination sends BungeeCord's `Connect` message for the clicking player. Permissions and the configured allowlist are checked again at click time. Discovery results are cached for 30 seconds; a missing proxy response times out rather than keeping the GUI loading indefinitely.

This uses BungeeCord's [built-in plugin messaging channel](https://www.spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel/); no separate proxy plugin is required. Configure normal BungeeCord forwarding and backend access correctly. Cross-server moderation, remote player lists, and Folia are not implemented.

## Review checks

Regression tests cover pagination across 100 players, page clamping after departures, visibility filtering, blocked inventory transfers/dragging, permission rechecks, server allowlists, and server-list parsing.

Before deployment, try two moderators targeting different players, disconnect a selected target, open the GUI with more than 45 players, and test the server switcher as an operator and a player with one destination permission. Test with each optional integration enabled and with all integrations absent.

[Published release on Spigot](https://www.spigotmc.org/resources/onlinegui.82988/)
