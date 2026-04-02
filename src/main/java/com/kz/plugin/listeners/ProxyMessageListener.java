// ============================================================
// Path: src/main/java/com/kz/plugin/listeners/ProxyMessageListener.java
// ============================================================
package com.kz.plugin.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.kz.plugin.KZPlugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class ProxyMessageListener implements PluginMessageListener {

    private final KZPlugin plugin;

    public ProxyMessageListener(KZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals("BungeeCord")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);

        try {
            String subChannel = in.readUTF();

            switch (subChannel) {
                case "KZPlugin" -> handleKZMessage(in, player);
                case "GetServer" -> handleGetServer(in);
                case "PlayerCount" -> handlePlayerCount(in);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Proxy] Error reading plugin message: " + e.getMessage());
        }
    }

    private void handleKZMessage(ByteArrayDataInput in, Player player) {
        try {
            String action = in.readUTF();

            switch (action) {
                case "BalanceUpdate" -> {
                    String uuid = in.readUTF();
                    String mode = in.readUTF();
                    double amount = in.readDouble();
                    plugin.getLogger().info("[Proxy] Balance update received: " + uuid + " mode=" + mode + " amount=" + amount);
                }

                case "RankUpdate" -> {
                    String uuid = in.readUTF();
                    String rank = in.readUTF();
                    plugin.getLogger().info("[Proxy] Rank update received: " + uuid + " rank=" + rank);
                }

                case "ServerMessage" -> {
                    String msg = in.readUTF();
                    plugin.getServer().broadcastMessage(msg);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Proxy] Error handling KZ message: " + e.getMessage());
        }
    }

    private void handleGetServer(ByteArrayDataInput in) {
        try {
            String serverName = in.readUTF();
            plugin.getLogger().info("[Proxy] Connected to server: " + serverName);
        } catch (Exception e) {
            plugin.getLogger().warning("[Proxy] Error reading server name: " + e.getMessage());
        }
    }

    private void handlePlayerCount(ByteArrayDataInput in) {
        try {
            String server = in.readUTF();
            int count = in.readInt();
            plugin.getLogger().info("[Proxy] Player count on " + server + ": " + count);
        } catch (Exception e) {
            plugin.getLogger().warning("[Proxy] Error reading player count: " + e.getMessage());
        }
    }
}
