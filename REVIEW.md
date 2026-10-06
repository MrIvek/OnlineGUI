# Update review: OnlineGUI 1.11.0-SNAPSHOT

This document records the reviewed changes and validation for OnlineGUI 1.11.0-SNAPSHOT. Live deployment has not been performed.

## Previous version and Minecraft update

- Repository release: 1.10.1; explicit version whitelist stopped at Minecraft 1.21.3.
- Latest stable Java Edition checked: 26.3, released September 15, 2026.
- Removed whitelist-dependent skull creation and CraftBukkit package-name matching. Player heads use `PLAYER_HEAD` and `SkullMeta.setOwningPlayer`.
- Added an adapter for the InventoryView class-to-interface change, so the default older bytecode can work with the latest Bukkit API.
- API baseline remains 1.13.2, consistent with the existing `api-version: 1.13`. Versions below 1.13 are not claimed as supported.

## Cleanup and performance

- Replaced the nested player loops and repeated inventory clearing with a sorted, visibility-filtered player snapshot. Only the requested page's heads are built (at most 45).
- Fixed pagination across arbitrary player counts and clamped the page after departures.
- Replaced title/item-name-based identity with inventory holders and UUID targets.
- Removed shared moderation target state and retained state only in the active inventory.
- Cancelled clicks on empty slots, bottom inventory transfers, and drags before examining the clicked item.
- Scheduled inventory transitions outside Bukkit's click transaction and coalesced join/quit refreshes within a tick.
- Added per-action permission checks, including a recheck when the action runs.
- Removed duplicate head builders and the static singleton retaining old plugin instances.
- Fixed LuckPerms suffix rendering, used UUID user lookups, and handled unloaded users.
- Honored configured menu size and validated item configuration; old `WOOL` values migrate to `WHITE_WOOL`.
- Removed debug chat output and the event annotation from the command executor.

## Integrations and BungeeCord

EssentialsX balance/AFK/mute/vanish support, GroupManager group lore, and LuckPerms prefix/suffix lore remain. GroupManager is accessed through its existing public methods without requiring its jar on the build classpath.

BungeeCord is opt-in through `bungeecord.enabled`. `/online` shows current-server players and a compass for a server switcher. The switcher lists allowed destinations only, based on the configuration allowlist and `onlinegui.server.<name>` permissions. Remote players are not listed. Clicking sends BungeeCord's built-in `Connect` message; discovery uses `GetServers`, a short-lived cache, and a timeout. No additional proxy plugin is needed.

## Build and validation

Added Maven with provided Bukkit/EssentialsX/LuckPerms dependencies. Historical server jars and old class files are excluded from the produced plugin jar.

Validation completed on October 6, 2026:

- Default package: successful; nine regression tests passed using the 1.13.2 API and Java 8 output bytecode.
- Latest package: successful; nine regression tests passed using the 26.3 API and Java 25.
- Cross-version check: all nine tests passed with the default compiled plugin classes substituted into the 26.3 API test runtime.
- Jar contents inspected: only plugin classes, config, descriptor, and Maven metadata; no server or integration jars.
- Whitespace check passed.

The tests use mocked Bukkit services. Live server/proxy deployment and testing of actual installed integration plugin versions have not been performed. The README includes setup, permission examples, and staging checks.
