// ============================================================
// PATH: src/main/java/com/kz/plugin/listeners/ProxyMessageListener.java
// ============================================================
package com.kz.plugin.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.kz.plugin.KZPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * ProxyMessageListener - Handles BungeeCord plugin messaging channel
 *
 * INCOMING: Receives messages FROM Velocity proxy / other servers
 * OUTGOING: Sends messages TO proxy (connect, playercount, custom forward)
 *
 * Velocity supports BungeeCord channel when:
 * velocity.toml → bungee-plugin-message-channel = true
 *
 * Custom subchannel "KZPlugin" for inter-server communication:
 * - SetMode      : Set player's game mode on arrival
 * - BalanceUpdate: Sync balance across servers
 * - RankUpdate   : Sync rank across servers
 * - ServerMessage: Broadcast message to this server
 * - Announce     : Network-wide announcement
 * - Kick         : Force kick a player
 * - Maintenance  : Toggle maintenance mode
 */
public class ProxyMessageListener implements PluginMessageListener {

    private final KZPlugin plugin;

    // ════════════════════════════════════════════════════════════════
    //  CACHE & CALLBACKS
    // ════════════════════════════════════════════════════════════════

    /** Cached server name received from proxy */
    private String cachedServerName = null;

    /** Cached player counts per server */
    private final Map<String, Integer> playerCounts = new HashMap<>();

    /** Pending callbacks for async responses */
    private final Map<String, Consumer<String>> serverNameCallbacks = new HashMap<>();
    private final Map<String, Consumer<Integer>> playerCountCallbacks = new HashMap<>();

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════

    public ProxyMessageListener(KZPlugin plugin) {
        this.plugin = plugin;
    }

    // ════════════════════════════════════════════════════════════════
    //  INCOMING MESSAGE HANDLER
    // ════════════════════════════════════════════════════════════════

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player,
                                         byte @NotNull [] message) {
        if (!channel.equals("BungeeCord")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);

        try {
            String subChannel = in.readUTF();

            switch (subChannel) {
                case "GetServer" -> handleGetServer(in);
                case "PlayerCount" -> handlePlayerCount(in);
                case "ServerIP" -> handleServerIP(in);
                case "KZPlugin" -> handleKZMessage(in, player);
                default -> plugin.getLogger().fine("[Proxy] Unknown subchannel: " + subChannel);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Proxy] Error reading plugin message: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  HANDLE: GetServer response
    // ════════════════════════════════════════════════════════════════

    private void handleGetServer(ByteArrayDataInput in) {
        try {
            String serverName = in.readUTF();
            cachedServerName = serverName;

            plugin.getLogger().info("[Proxy] Server identity confirmed: " + serverName);

            // Execute pending callbacks
            for (Consumer<String> callback : serverNameCallbacks.values()) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(serverName));
            }
            serverNameCallbacks.clear();

        } catch (Exception e) {
            plugin.getLogger().warning("[Proxy] Error reading GetServer: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  HANDLE: PlayerCount response
    // ════════════════════════════════════════════════════════════════

    private void handlePlayerCount(ByteArrayDataInput in) {
        try {
            String server = in.readUTF();
            int count = in.readInt();

            playerCounts.put(server, count);

            // Execute pending callback
            String key = "count_" + server;
            Consumer<Integer> callback = playerCountCallbacks.remove(key);
            if (callback != null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(count));
            }

            plugin.getLogger().fine("[Proxy] PlayerCount " + server + ": " + count);

        } catch (Exception e) {
            plugin.getLogger().warning("[Proxy] Error reading PlayerCount: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  HANDLE: ServerIP response
    // ════════════════════════════════════════════════════════════════

    private void handleServerIP(ByteArrayDataInput in) {
        try {
            String server = in.readUTF();
            String ip = in.readUTF();
            int port = in.readUnsignedShort();
            plugin.getLogger().fine("[Proxy] ServerIP: " + server + " = " + ip + ":" + port);
        } catch (Exception e) {
            plugin.getLogger().warning("[Proxy] Error reading ServerIP: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  HANDLE: Custom KZPlugin messages
    // ════════════════════════════════════════════════════════════════

    private void handleKZMessage(ByteArrayDataInput in, Player player) {
        try {
            String action = in.readUTF();

            switch (action) {

                // ── SetMode: Set player's game mode on this server ──
                // Sent when player transfers from another server
                case "SetMode" -> {
                    String playerName = in.readUTF();
                    String mode = in.readUTF();

                    Player target = Bukkit.getPlayer(playerName);
                    if (target != null && target.isOnline()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            // Set economy mode
                            if (plugin.getEconomyManager() != null) {
                                plugin.getEconomyManager().setPlayerMode(target, mode);
                            }

                            // Teleport to mode spawn
                            if (plugin.getLobbySystem() != null) {
                                var spawn = plugin.getLobbySystem().getModeSpawn(mode);
                                if (spawn != null) {
                                    target.teleport(spawn);
                                }
                            }

                            target.sendMessage("§a§lKZ §8» §7Welcome to §b"
                                    + capitalize(mode) + "§7 mode!");
                            target.playSound(target.getLocation(),
                                    Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                        });

                        plugin.getLogger().info("[Proxy] SetMode: " + playerName + " → " + mode);
                    }
                }

                // ── BalanceUpdate: Sync balance from another server ──
                case "BalanceUpdate" -> {
                    String uuidStr = in.readUTF();
                    String mode = in.readUTF();
                    double amount = in.readDouble();

                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        Player target = Bukkit.getPlayer(uuid);

                        if (target != null && target.isOnline() && plugin.getEconomyManager() != null) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                // Force reload balance from database
                                plugin.getEconomyManager().loadPlayerData(target);
                            });
                        }

                        plugin.getLogger().info("[Proxy] BalanceUpdate: " + uuidStr
                                + " mode=" + mode + " amount=" + amount);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("[Proxy] Invalid UUID in BalanceUpdate: " + uuidStr);
                    }
                }

                // ── RankUpdate: Sync rank from another server ──
                case "RankUpdate" -> {
                    String uuidStr = in.readUTF();
                    String rank = in.readUTF();

                    try {
                        UUID uuid = UUID.fromString(uuidStr);

                        if (plugin.getLobbySystem() != null) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                plugin.getLobbySystem().setRank(uuid, rank);

                                Player target = Bukkit.getPlayer(uuid);
                                if (target != null && target.isOnline()) {
                                    target.sendMessage("§a§lKZ §8» §7Your rank has been updated to §f"
                                            + plugin.getLobbySystem().getRankDisplay(rank) + "§7!");
                                    target.playSound(target.getLocation(),
                                            Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                                }
                            });
                        }

                        plugin.getLogger().info("[Proxy] RankUpdate: " + uuidStr + " → " + rank);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("[Proxy] Invalid UUID in RankUpdate: " + uuidStr);
                    }
                }

                // ── ServerMessage: Broadcast a message on this server only ──
                case "ServerMessage" -> {
                    String msg = in.readUTF();

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            online.sendMessage(msg);
                        }
                    });

                    plugin.getLogger().info("[Proxy] ServerMessage: " + msg);
                }

                // ── Announce: Network-wide announcement with formatting ──
                case "Announce" -> {
                    String message = in.readUTF();

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            online.sendMessage("");
                            online.sendMessage("§6§l┌─────────────────────────────────┐");
                            online.sendMessage("§6§l│  §e§lNETWORK ANNOUNCEMENT          §6§l│");
                            online.sendMessage("§6§l└─────────────────────────────────┘");
                            online.sendMessage("");
                            online.sendMessage("  §f" + message);
                            online.sendMessage("");
                            online.playSound(online.getLocation(),
                                    Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
                        }
                    });

                    plugin.getLogger().info("[Proxy] Announce: " + message);
                }

                // ── Kick: Force kick a player from this server ──
                case "Kick" -> {
                    String playerName = in.readUTF();
                    String reason = in.readUTF();

                    Player target = Bukkit.getPlayer(playerName);
                    if (target != null && target.isOnline()) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                target.kickPlayer("§c§lKZ SERVER\n\n§7" + reason));
                    }

                    plugin.getLogger().info("[Proxy] Kick: " + playerName + " (" + reason + ")");
                }

                // ── Maintenance: Toggle maintenance mode ──
                case "Maintenance" -> {
                    String enabledStr = in.readUTF();
                    boolean enabled = Boolean.parseBoolean(enabledStr);

                    if (plugin.getLobbySystem() != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getLobbySystem().setMaintenance(enabled);

                            String status = enabled ? "§c§lENABLED" : "§a§lDISABLED";
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                online.sendMessage("");
                                online.sendMessage("§e§lKZ §8» §7Maintenance mode: " + status);
                                online.sendMessage("");

                                // Kick non-admin if maintenance enabled
                                if (enabled && !online.hasPermission("kzplugin.admin")) {
                                    online.kickPlayer("§c§lKZ SERVER\n\n§7Server is under maintenance.\n§7Please try again later.");
                                }
                            }
                        });
                    }

                    plugin.getLogger().info("[Proxy] Maintenance: " + (enabled ? "ON" : "OFF"));
                }

                default -> plugin.getLogger().fine("[Proxy] Unknown KZPlugin action: " + action);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("[Proxy] Error handling KZPlugin message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  OUTGOING: Request server name from proxy
    // ════════════════════════════════════════════════════════════════

    /**
     * Request this server's name from the proxy
     * Response comes back async via handleGetServer()
     *
     * @param player   Any online player (needed as message carrier)
     * @param callback Called when response is received
     */
    public void requestServerName(Player player, Consumer<String> callback) {
        // Return cached if available
        if (cachedServerName != null) {
            callback.accept(cachedServerName);
            return;
        }

        serverNameCallbacks.put(player.getName(), callback);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServer");
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    // ════════════════════════════════════════════════════════════════
    //  OUTGOING: Request player count
    // ════════════════════════════════════════════════════════════════

    /**
     * Request player count on a specific server
     *
     * @param requester Any online player
     * @param server    Server name (or "ALL" for total)
     * @param callback  Called with the count
     */
    public void requestPlayerCount(Player requester, String server, Consumer<Integer> callback) {
        String key = "count_" + server;
        playerCountCallbacks.put(key, callback);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerCount");
        out.writeUTF(server);
        requester.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    /**
     * Request total network player count
     */
    public void requestTotalPlayerCount(Player requester, Consumer<Integer> callback) {
        requestPlayerCount(requester, "ALL", callback);
    }

    // ════════════════════════════════════════════════════════════════
    //  OUTGOING: Forward custom message to another server
    // ════════════════════════════════════════════════════════════════

    /**
     * Send a custom KZPlugin message to another server via proxy
     *
     * Uses BungeeCord "Forward" subchannel to route messages
     *
     * @param sender       Any online player (message carrier)
     * @param targetServer Target server name (or "ALL" for all servers)
     * @param action       Action name (SetMode, Announce, etc)
     * @param data         Data strings to send
     */
    public void sendCustomMessage(Player sender, String targetServer,
                                   String action, String... data) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Forward");
            out.writeUTF(targetServer);
            out.writeUTF("KZPlugin"); // Our custom subchannel

            // Build the payload
            ByteArrayDataOutput payload = ByteStreams.newDataOutput();
            payload.writeUTF(action);
            for (String d : data) {
                payload.writeUTF(d);
            }

            byte[] payloadBytes = payload.toByteArray();
            out.writeShort(payloadBytes.length);
            out.write(payloadBytes);

            sender.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());

            plugin.getLogger().fine("[Proxy] Sent: " + action + " → " + targetServer);

        } catch (Exception e) {
            plugin.getLogger().warning("[Proxy] Failed to send message: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  OUTGOING HELPERS - Convenience methods
    // ════════════════════════════════════════════════════════════════

    /**
     * Tell target server to set player's game mode
     * Used when player transfers from lobby to backend
     */
    public void sendSetMode(Player sender, String targetServer,
                             String playerName, String mode) {
        sendCustomMessage(sender, targetServer, "SetMode", playerName, mode);
    }

    /**
     * Send network-wide announcement to ALL servers
     */
    public void sendNetworkAnnounce(Player sender, String message) {
        sendCustomMessage(sender, "ALL", "Announce", message);
    }

    /**
     * Send balance update notification to other servers
     * So they can reload from database
     */
    public void sendBalanceUpdate(Player sender, String targetServer,
                                   UUID playerUUID, String mode, double amount) {
        sendCustomMessage(sender, targetServer, "BalanceUpdate",
                playerUUID.toString(), mode, String.valueOf(amount));
    }

    /**
     * Send rank update to all servers
     */
    public void sendRankUpdate(Player sender, UUID playerUUID, String rank) {
        sendCustomMessage(sender, "ALL", "RankUpdate",
                playerUUID.toString(), rank);
    }

    /**
     * Send kick request to a specific server
     */
    public void sendKick(Player sender, String targetServer,
                          String playerName, String reason) {
        sendCustomMessage(sender, targetServer, "Kick", playerName, reason);
    }

    /**
     * Toggle maintenance on all servers
     */
    public void sendMaintenanceToggle(Player sender, boolean enabled) {
        sendCustomMessage(sender, "ALL", "Maintenance", String.valueOf(enabled));
    }

    /**
     * Broadcast a message on a specific server
     */
    public void sendServerMessage(Player sender, String targetServer, String message) {
        sendCustomMessage(sender, targetServer, "ServerMessage", message);
    }

    // ════════════════════════════════════════════════════════════════
    //  GETTERS - Cache access
    // ════════════════════════════════════════════════════════════════

    /** Get cached server name (null if not yet received) */
    public String getCachedServerName() {
        return cachedServerName;
    }

    /** Get cached player count for a server (-1 if not cached) */
    public int getCachedPlayerCount(String server) {
        return playerCounts.getOrDefault(server, -1);
    }

    /** Get all cached player counts */
    public Map<String, Integer> getAllPlayerCounts() {
        return new HashMap<>(playerCounts);
    }

    // ════════════════════════════════════════════════════════════════
    //  UTILITY
    // ════════════════════════════════════════════════════════════════

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
