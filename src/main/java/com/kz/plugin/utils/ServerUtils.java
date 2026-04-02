// ============================================================
// Path: src/main/java/com/kz/plugin/utils/ServerUtils.java
// ============================================================
package com.kz.plugin.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ServerUtils {

    public static void sendToServer(Plugin plugin, Player player, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public static void sendToLobby(Plugin plugin, Player player) {
        sendToServer(plugin, player, "lobby");
    }

    public static void sendToSurvival(Plugin plugin, Player player) {
        sendToServer(plugin, player, "survival");
    }

    public static void sendToVoid(Plugin plugin, Player player) {
        sendToServer(plugin, player, "void");
    }

    public static void sendToCustom(Plugin plugin, Player player) {
        sendToServer(plugin, player, "custom");
    }
}
