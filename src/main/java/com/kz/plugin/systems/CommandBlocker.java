// ============================================================
// PATH: src/main/java/com/kz/plugin/systems/CommandBlocker.java
// ============================================================
package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.*;

/**
 * CommandBlocker - Block specific commands based on server and game mode
 *
 * Priority system:
 * 1. Admin bypass (kzplugin.admin) → always allowed
 * 2. Global blocked commands → blocked on ALL servers
 * 3. Per-server blocked commands → blocked on specific server
 * 4. Per-mode blocked commands → blocked in specific game mode
 *
 * Config structure in config.yml:
 * command-blocker:
 *   enabled: true
 *   global-blocked: [/op, /deop, /stop, ...]
 *   servers:
 *     lobby: [/shop, /sell, /job, ...]
 *     survival: [/createisland, ...]
 *   modes:
 *     vanilla: [/shop, /sell, /ah]
 *     lobby: [/shop, /sell, /job, /createisland, ...]
 */
public class CommandBlocker implements Listener {

    private final KZPlugin plugin;

    // ════════════════════════════════════════════════════════════════
    //  DATA
    // ════════════════════════════════════════════════════════════════

    private boolean enabled = true;

    /** Commands blocked on ALL servers (security) */
    private final Set<String> globalBlocked = new HashSet<>();

    /** Commands blocked per server name */
    private final Map<String, Set<String>> serverBlocked = new HashMap<>();

    /** Commands blocked per game mode */
    private final Map<String, Set<String>> modeBlocked = new HashMap<>();

    /** Custom deny messages per server */
    private final Map<String, String> serverDenyMessages = new HashMap<>();

    /** Custom deny messages per mode */
    private final Map<String, String> modeDenyMessages = new HashMap<>();

    /** Current server name */
    private final String currentServer;

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════

    public CommandBlocker(KZPlugin plugin) {
        this.plugin = plugin;
        this.currentServer = plugin.getConfig().getString("server-name", "lobby");

        loadConfig();

        // Register listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        plugin.getLogger().info("[CommandBlocker] Initialized. Server: " + currentServer
                + " | Global: " + globalBlocked.size()
                + " | Server: " + serverBlocked.getOrDefault(currentServer, Set.of()).size()
                + " | Modes: " + modeBlocked.size() + " configured");
    }

    // ════════════════════════════════════════════════════════════════
    //  LOAD CONFIG
    // ════════════════════════════════════════════════════════════════

    private void loadConfig() {
        var config = plugin.getConfig();

        enabled = config.getBoolean("command-blocker.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("[CommandBlocker] Disabled in config.");
            return;
        }

        // ── Global blocked commands (security) ──
        List<String> globalList = config.getStringList("command-blocker.global-blocked");
        if (globalList.isEmpty()) {
            // Default security commands
            globalList = List.of(
                    "/op", "/deop", "/stop", "/restart",
                    "/reload", "/plugins", "/pl", "/ver", "/version",
                    "/bukkit:reload", "/bukkit:plugins", "/bukkit:version",
                    "/minecraft:op", "/minecraft:deop",
                    "/timings", "/spark", "/paper"
            );
        }
        for (String cmd : globalList) {
            globalBlocked.add(cmd.toLowerCase().trim());
        }

        // ── Per-server blocked commands ──
        var serversSection = config.getConfigurationSection("command-blocker.servers");
        if (serversSection != null) {
            for (String server : serversSection.getKeys(false)) {
                Set<String> cmds = new HashSet<>();
                for (String cmd : config.getStringList("command-blocker.servers." + server + ".commands")) {
                    cmds.add(cmd.toLowerCase().trim());
                }
                serverBlocked.put(server.toLowerCase(), cmds);

                // Custom deny message
                String msg = config.getString("command-blocker.servers." + server + ".message", null);
                if (msg != null) {
                    serverDenyMessages.put(server.toLowerCase(), msg);
                }
            }
        } else {
            // Default server blocks
            loadDefaultServerBlocks();
        }

        // ── Per-mode blocked commands ──
        var modesSection = config.getConfigurationSection("command-blocker.modes");
        if (modesSection != null) {
            for (String mode : modesSection.getKeys(false)) {
                Set<String> cmds = new HashSet<>();
                for (String cmd : config.getStringList("command-blocker.modes." + mode + ".commands")) {
                    cmds.add(cmd.toLowerCase().trim());
                }
                modeBlocked.put(mode.toLowerCase(), cmds);

                // Custom deny message
                String msg = config.getString("command-blocker.modes." + mode + ".message", null);
                if (msg != null) {
                    modeDenyMessages.put(mode.toLowerCase(), msg);
                }
            }
        } else {
            // Default mode blocks
            loadDefaultModeBlocks();
        }
    }

    /**
     * Default server blocks jika config belum diisi
     */
    private void loadDefaultServerBlocks() {
        // LOBBY - block semua gameplay commands
        Set<String> lobbyBlocked = new HashSet<>(Set.of(
                "/shop", "/sell", "/ah", "/inbox",
                "/job", "/createisland", "/deleteisland",
                "/upisland", "/islandsetting", "/nameisland",
                "/home", "/visit", "/topisland",
                "/invite", "/accept", "/deny",
                "/trust", "/untrust",
                "/landinvite", "/landaccept", "/landdeny",
                "/landrole", "/landkick", "/trustland",
                "/memberrule", "/trustrule", "/setlandname",
                "/deleteland", "/cekcapasitas",
                "/tpa", "/tpaccept", "/tpadeny", "/tpcancel",
                "/cf", "/weekly"
        ));
        serverBlocked.put("lobby", lobbyBlocked);
        serverDenyMessages.put("lobby", "This command is not available in the lobby! Select a game mode first.");

        // SURVIVAL - block island-specific commands
        Set<String> survivalBlocked = new HashSet<>(Set.of(
                "/createisland", "/deleteisland", "/upisland",
                "/islandsetting", "/nameisland"
        ));
        serverBlocked.put("survival", survivalBlocked);
        serverDenyMessages.put("survival", "This command is only available in island game modes!");

        // VOID & CUSTOM - block land commands (island-based, no land claiming)
        Set<String> voidBlocked = new HashSet<>(Set.of(
                "/landinvite", "/landaccept", "/landdeny",
                "/landrole", "/landkick", "/trustland",
                "/memberrule", "/trustrule", "/setlandname",
                "/deleteland", "/cekcapasitas"
        ));
        serverBlocked.put("void", voidBlocked);
        serverBlocked.put("custom", new HashSet<>(voidBlocked));
        serverDenyMessages.put("void", "Land commands are not available in this game mode!");
        serverDenyMessages.put("custom", "Land commands are not available in this game mode!");
    }

    /**
     * Default mode blocks jika config belum diisi
     */
    private void loadDefaultModeBlocks() {
        // VANILLA mode - no economy shop (pure vanilla experience)
        Set<String> vanillaBlocked = new HashSet<>(Set.of(
                "/shop", "/sell", "/ah", "/inbox", "/cf",
                "/job", "/daily", "/weekly", "/baltop"
        ));
        modeBlocked.put("vanilla", vanillaBlocked);
        modeDenyMessages.put("vanilla", "This command is disabled in Vanilla mode for a pure survival experience!");

        // LOBBY mode (when player is on backend but in lobby area)
        Set<String> lobbyModeBlocked = new HashSet<>(Set.of(
                "/shop", "/sell", "/ah", "/inbox",
                "/job", "/createisland", "/deleteisland",
                "/cf"
        ));
        modeBlocked.put("lobby", lobbyModeBlocked);
        modeDenyMessages.put("lobby", "Select a game mode first before using this command!");
    }

    // ════════════════════════════════════════════════════════════════
    //  EVENT HANDLER - Intercept commands before execution
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();

        // Admin bypass
        if (player.hasPermission("kzplugin.admin")) return;

        String fullCommand = event.getMessage().toLowerCase().trim();

        // Extract base command (e.g. "/shop food" → "/shop")
        String baseCommand = fullCommand.split(" ")[0];

        // Also check without leading slash for matching
        String cmdNoSlash = baseCommand.startsWith("/") ? baseCommand.substring(1) : baseCommand;

        // ── Check 1: Global blocked commands ──
        if (isGlobalBlocked(baseCommand)) {
            event.setCancelled(true);
            sendDeny(player, "§cThis command is restricted.",
                    "§7You don't have permission to use this command.");
            return;
        }

        // ── Check 2: Server-specific blocked commands ──
        if (isServerBlocked(baseCommand)) {
            event.setCancelled(true);
            String msg = serverDenyMessages.getOrDefault(
                    currentServer.toLowerCase(),
                    "This command is not available on this server!");
            sendDeny(player, "§cCommand Blocked", "§7" + msg);
            return;
        }

        // ── Check 3: Mode-specific blocked commands ──
        String playerMode = getPlayerMode(player);
        if (playerMode != null && isModeBlocked(baseCommand, playerMode)) {
            event.setCancelled(true);
            String msg = modeDenyMessages.getOrDefault(
                    playerMode.toLowerCase(),
                    "This command is not available in " + capitalize(playerMode) + " mode!");
            sendDeny(player, "§cCommand Blocked", "§7" + msg);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CHECK METHODS
    // ════════════════════════════════════════════════════════════════

    /**
     * Check if command is globally blocked
     */
    private boolean isGlobalBlocked(String command) {
        // Check exact match
        if (globalBlocked.contains(command)) return true;

        // Check with plugin prefix (e.g. /bukkit:op)
        for (String blocked : globalBlocked) {
            String blockedName = blocked.startsWith("/") ? blocked.substring(1) : blocked;
            String cmdName = command.startsWith("/") ? command.substring(1) : command;

            // Match "op" with "bukkit:op", "minecraft:op", etc.
            if (cmdName.contains(":") && cmdName.endsWith(":" + blockedName)) return true;
            if (cmdName.equals(blockedName)) return true;
        }

        return false;
    }

    /**
     * Check if command is blocked on current server
     */
    private boolean isServerBlocked(String command) {
        Set<String> blocked = serverBlocked.get(currentServer.toLowerCase());
        if (blocked == null) return false;

        return matchCommand(command, blocked);
    }

    /**
     * Check if command is blocked in player's current mode
     */
    private boolean isModeBlocked(String command, String mode) {
        Set<String> blocked = modeBlocked.get(mode.toLowerCase());
        if (blocked == null) return false;

        return matchCommand(command, blocked);
    }

    /**
     * Match a command against a set of blocked commands
     * Handles: /shop, shop, /plugin:shop
     */
    private boolean matchCommand(String command, Set<String> blockedSet) {
        String cmd = command.toLowerCase();

        // Direct match: /shop
        if (blockedSet.contains(cmd)) return true;

        // Without slash: shop
        String noSlash = cmd.startsWith("/") ? cmd.substring(1) : cmd;
        if (blockedSet.contains(noSlash)) return true;
        if (blockedSet.contains("/" + noSlash)) return true;

        // Handle plugin prefix: /kzplugin:shop → check "shop"
        if (noSlash.contains(":")) {
            String afterColon = noSlash.substring(noSlash.indexOf(":") + 1);
            if (blockedSet.contains("/" + afterColon)) return true;
            if (blockedSet.contains(afterColon)) return true;
        }

        return false;
    }

    // ════════════════════════════════════════════════════════════════
    //  DENY MESSAGE
    // ════════════════════════════════════════════════════════════════

    /**
     * Send formatted deny message to player
     */
    private void sendDeny(Player player, String title, String reason) {
        player.sendMessage("");
        player.sendMessage("§c§l┌──────────────────────────────────────┐");
        player.sendMessage("§c§l│     §f§l⛔ " + title + "                §c§l│");
        player.sendMessage("§c§l└──────────────────────────────────────┘");
        player.sendMessage("");
        player.sendMessage("  " + reason);
        player.sendMessage("");

        // Extra hint based on server
        if ("lobby".equalsIgnoreCase(currentServer)) {
            player.sendMessage("  §7Click an NPC or type §b/menu §7to select a mode.");
        } else {
            player.sendMessage("  §7Type §b/help §7to see available commands.");
        }
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  UTILITY
    // ════════════════════════════════════════════════════════════════

    /**
     * Get player's current game mode from EconomyManager
     */
    private String getPlayerMode(Player player) {
        try {
            if (plugin.getEconomyManager() != null) {
                return plugin.getEconomyManager().getPlayerMode(player);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Capitalize first letter
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    // ════════════════════════════════════════════════════════════════
    //  PUBLIC API - For admin commands
    // ════════════════════════════════════════════════════════════════

    /**
     * Add a command to server blocklist at runtime
     */
    public void addServerBlock(String server, String command) {
        serverBlocked.computeIfAbsent(server.toLowerCase(), k -> new HashSet<>())
                .add(command.toLowerCase());
    }

    /**
     * Remove a command from server blocklist at runtime
     */
    public void removeServerBlock(String server, String command) {
        Set<String> cmds = serverBlocked.get(server.toLowerCase());
        if (cmds != null) {
            cmds.remove(command.toLowerCase());
        }
    }

    /**
     * Add a command to mode blocklist at runtime
     */
    public void addModeBlock(String mode, String command) {
        modeBlocked.computeIfAbsent(mode.toLowerCase(), k -> new HashSet<>())
                .add(command.toLowerCase());
    }

    /**
     * Remove a command from mode blocklist at runtime
     */
    public void removeModeBlock(String mode, String command) {
        Set<String> cmds = modeBlocked.get(mode.toLowerCase());
        if (cmds != null) {
            cmds.remove(command.toLowerCase());
        }
    }

    /**
     * Check if a command is blocked for a player (external check)
     */
    public boolean isBlocked(Player player, String command) {
        if (player.hasPermission("kzplugin.admin")) return false;

        String cmd = command.toLowerCase();
        if (!cmd.startsWith("/")) cmd = "/" + cmd;

        if (isGlobalBlocked(cmd)) return true;
        if (isServerBlocked(cmd)) return true;

        String mode = getPlayerMode(player);
        if (mode != null && isModeBlocked(cmd, mode)) return true;

        return false;
    }

    /**
     * List all blocked commands for current server
     */
    public void listBlocked(Player player) {
        player.sendMessage("");
        player.sendMessage("§c§l┌──────────────────────────────────────┐");
        player.sendMessage("§c§l│     §f§lBLOCKED COMMANDS                 §c§l│");
        player.sendMessage("§c§l└──────────────────────────────────────┘");
        player.sendMessage("");

        // Global
        player.sendMessage("  §c§lGlobal §8(§7" + globalBlocked.size() + " commands§8)");
        if (!globalBlocked.isEmpty()) {
            player.sendMessage("  §7" + String.join(", ", globalBlocked));
        }
        player.sendMessage("");

        // Server
        Set<String> srvCmds = serverBlocked.getOrDefault(currentServer.toLowerCase(), Set.of());
        player.sendMessage("  §e§lServer: " + capitalize(currentServer) + " §8(§7" + srvCmds.size() + " commands§8)");
        if (!srvCmds.isEmpty()) {
            player.sendMessage("  §7" + String.join(", ", srvCmds));
        }
        player.sendMessage("");

        // Modes
        player.sendMessage("  §d§lMode Blocks:");
        if (modeBlocked.isEmpty()) {
            player.sendMessage("  §7None configured.");
        } else {
            for (Map.Entry<String, Set<String>> entry : modeBlocked.entrySet()) {
                player.sendMessage("  §7" + capitalize(entry.getKey()) + " §8(§7"
                        + entry.getValue().size() + "§8): §f"
                        + String.join(", ", entry.getValue()));
            }
        }

        player.sendMessage("");
    }

    /**
     * Reload config
     */
    public void reload() {
        globalBlocked.clear();
        serverBlocked.clear();
        modeBlocked.clear();
        serverDenyMessages.clear();
        modeDenyMessages.clear();
        loadConfig();
        plugin.getLogger().info("[CommandBlocker] Config reloaded.");
    }

    /**
     * Check if system is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}
