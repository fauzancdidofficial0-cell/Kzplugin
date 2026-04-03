// ============================================================
// PATH: src/main/java/com/kz/plugin/systems/AntiSpamSystem.java
// ============================================================
package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AntiSpamSystem - Detect and warn spamming players
 *
 * Detection:
 * - Tracks last N messages per player with timestamps
 * - If X identical messages within Y seconds → spam detected
 * - If rapid-fire messages (any content) too fast → spam detected
 *
 * Punishment escalation:
 * - Warning 1: Warning message
 * - Warning 2: Warning message (stricter)
 * - Warning 3: Auto-kick
 * - Warning 4+: Auto-kick (repeated)
 *
 * Warnings decay after 30 minutes of clean chat
 */
public class AntiSpamSystem implements Listener {

    private final KZPlugin plugin;

    // ════════════════════════════════════════════════════════════════
    //  DATA
    // ════════════════════════════════════════════════════════════════

    /** Recent messages per player */
    private final Map<UUID, List<ChatEntry>> recentMessages = new ConcurrentHashMap<>();

    /** Warning count per player */
    private final Map<UUID, Integer> warnings = new ConcurrentHashMap<>();

    /** Last warning time per player (for decay) */
    private final Map<UUID, Long> lastWarningTime = new ConcurrentHashMap<>();

    /** Muted players (temporarily blocked from chatting) */
    private final Map<UUID, Long> mutedUntil = new ConcurrentHashMap<>();

    /** Chat entry record */
    private record ChatEntry(String message, long timestamp) {}

    // ════════════════════════════════════════════════════════════════
    //  CONFIG
    // ════════════════════════════════════════════════════════════════

    private int maxIdenticalMessages = 4;    // X identical messages = spam
    private int identicalTimeWindow = 10;    // within Y seconds
    private int maxRapidMessages = 6;        // X messages (any content)
    private int rapidTimeWindow = 5;         // within Y seconds
    private int maxWarnings = 3;             // warnings before kick
    private int warningDecayMinutes = 30;    // warnings reset after X min
    private int muteSeconds = 10;            // mute duration after spam
    private int maxTrackedMessages = 10;     // max stored messages per player

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════

    public AntiSpamSystem(KZPlugin plugin) {
        this.plugin = plugin;

        // Load config
        loadConfig();

        // Register listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Warning decay task - check every 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                decayWarnings();
            }
        }.runTaskTimerAsynchronously(plugin, 6000L, 6000L);

        plugin.getLogger().info("[AntiSpam] System initialized. Max identical: "
                + maxIdenticalMessages + " in " + identicalTimeWindow + "s");
    }

    private void loadConfig() {
        var config = plugin.getConfig();
        maxIdenticalMessages = config.getInt("antispam.max-identical", 4);
        identicalTimeWindow = config.getInt("antispam.identical-window-seconds", 10);
        maxRapidMessages = config.getInt("antispam.max-rapid", 6);
        rapidTimeWindow = config.getInt("antispam.rapid-window-seconds", 5);
        maxWarnings = config.getInt("antispam.max-warnings", 3);
        warningDecayMinutes = config.getInt("antispam.warning-decay-minutes", 30);
        muteSeconds = config.getInt("antispam.mute-seconds", 10);
    }

    // ════════════════════════════════════════════════════════════════
    //  CHAT LISTENER - Priority LOWEST to run before everything
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Skip admins
        if (player.hasPermission("kzplugin.admin")) return;

        // Check if player is muted
        Long muteEnd = mutedUntil.get(uuid);
        if (muteEnd != null && System.currentTimeMillis() < muteEnd) {
            event.setCancelled(true);
            long remaining = (muteEnd - System.currentTimeMillis()) / 1000;
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage("§c§lKZ §8» §cYou are muted for §f" + remaining + "s §cdue to spamming."));
            return;
        }

        String message = event.getMessage();
        long now = System.currentTimeMillis();

        // Track message
        recentMessages.computeIfAbsent(uuid, k -> new ArrayList<>())
                .add(new ChatEntry(message, now));

        // Trim old entries
        trimMessages(uuid);

        // Check for spam
        SpamResult result = checkSpam(uuid, message, now);

        if (result != SpamResult.CLEAN) {
            event.setCancelled(true);

            // Increment warning
            int currentWarnings = warnings.merge(uuid, 1, Integer::sum);
            lastWarningTime.put(uuid, now);

            // Mute player temporarily
            mutedUntil.put(uuid, now + (muteSeconds * 1000L));

            // Handle punishment on main thread
            Bukkit.getScheduler().runTask(plugin, () ->
                    handlePunishment(player, currentWarnings, result));
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  SPAM DETECTION
    // ════════════════════════════════════════════════════════════════

    private enum SpamResult {
        CLEAN,
        IDENTICAL_SPAM,
        RAPID_SPAM
    }

    private SpamResult checkSpam(UUID uuid, String currentMessage, long now) {
        List<ChatEntry> messages = recentMessages.get(uuid);
        if (messages == null || messages.size() < 2) return SpamResult.CLEAN;

        // ── Check 1: Identical messages ──
        long identicalWindowMs = identicalTimeWindow * 1000L;
        int identicalCount = 0;

        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatEntry entry = messages.get(i);
            if (now - entry.timestamp() > identicalWindowMs) break;

            if (entry.message().equalsIgnoreCase(currentMessage)) {
                identicalCount++;
            }
        }

        if (identicalCount >= maxIdenticalMessages) {
            return SpamResult.IDENTICAL_SPAM;
        }

        // ── Check 2: Rapid-fire messages (any content) ──
        long rapidWindowMs = rapidTimeWindow * 1000L;
        int rapidCount = 0;

        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatEntry entry = messages.get(i);
            if (now - entry.timestamp() > rapidWindowMs) break;
            rapidCount++;
        }

        if (rapidCount >= maxRapidMessages) {
            return SpamResult.RAPID_SPAM;
        }

        return SpamResult.CLEAN;
    }

    // ════════════════════════════════════════════════════════════════
    //  PUNISHMENT - Warning → Kick escalation
    // ════════════════════════════════════════════════════════════════

    private void handlePunishment(Player player, int warningCount, SpamResult type) {
        String typeName = type == SpamResult.IDENTICAL_SPAM
                ? "repeated messages" : "rapid-fire messages";

        if (warningCount >= maxWarnings) {
            // ── KICK ──
            player.sendMessage("");
            player.sendMessage("§c§l┌──────────────────────────────────────┐");
            player.sendMessage("§c§l│     §f§l⚠ AUTO-KICK: SPAM VIOLATION      §c§l│");
            player.sendMessage("§c§l└──────────────────────────────────────┘");
            player.sendMessage("");
            player.sendMessage("  §7You have been kicked for excessive spamming.");
            player.sendMessage("  §7Please respect the chat rules.");
            player.sendMessage("");

            player.kickPlayer(
                    "§c§lKZ SERVER\n\n"
                            + "§7You have been kicked for §cspamming§7.\n"
                            + "§7Warnings: §c" + warningCount + "/" + maxWarnings + "\n\n"
                            + "§7Please follow the chat rules."
            );

            // Broadcast kick to other players
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) {
                    online.sendMessage("§c§lKZ §8» §f" + player.getName()
                            + " §7was kicked for spamming. §c(" + warningCount + " warnings)");
                }
            }

            // Reset warnings after kick (give them another chance)
            warnings.put(player.getUniqueId(), 0);

            plugin.getLogger().info("[AntiSpam] Kicked " + player.getName()
                    + " for spamming (" + typeName + ")");

        } else {
            // ── WARNING ──
            player.sendMessage("");
            player.sendMessage("§c§l┌──────────────────────────────────────┐");
            player.sendMessage("§c§l│     §f§l⚠ SPAM DETECTED                  §c§l│");
            player.sendMessage("§c§l└──────────────────────────────────────┘");
            player.sendMessage("");
            player.sendMessage("  §7Hey §c§l" + player.getName() + "§7, you have been");
            player.sendMessage("  §7detected sending §c" + typeName + "§7.");
            player.sendMessage("");
            player.sendMessage("  §7This is warning §c§l" + warningCount + "§7/§c§l" + maxWarnings + "§7.");
            player.sendMessage("  §7You are muted for §f" + muteSeconds + " seconds§7.");
            player.sendMessage("");
            player.sendMessage("  §7Continued violation will result in a");
            player.sendMessage("  §ckick §7or §cban §7from the server.");
            player.sendMessage("");
            player.sendMessage("  §7Please follow the server rules.");
            player.sendMessage("");
            player.sendMessage("§c§l────────────────────────────────────── ");
            player.sendMessage("");

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);

            plugin.getLogger().info("[AntiSpam] Warned " + player.getName()
                    + " (" + warningCount + "/" + maxWarnings + ") for " + typeName);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CLEANUP
    // ════════════════════════════════════════════════════════════════

    /**
     * Trim old messages beyond tracking window
     */
    private void trimMessages(UUID uuid) {
        List<ChatEntry> messages = recentMessages.get(uuid);
        if (messages == null) return;

        // Keep only last N messages
        while (messages.size() > maxTrackedMessages) {
            messages.remove(0);
        }

        // Remove entries older than the larger time window
        long cutoff = System.currentTimeMillis()
                - (Math.max(identicalTimeWindow, rapidTimeWindow) * 1000L);
        messages.removeIf(entry -> entry.timestamp() < cutoff);
    }

    /**
     * Decay warnings for players who haven't spammed recently
     */
    private void decayWarnings() {
        long now = System.currentTimeMillis();
        long decayMs = warningDecayMinutes * 60L * 1000L;

        Iterator<Map.Entry<UUID, Long>> iter = lastWarningTime.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, Long> entry = iter.next();
            if (now - entry.getValue() > decayMs) {
                warnings.remove(entry.getKey());
                recentMessages.remove(entry.getKey());
                mutedUntil.remove(entry.getKey());
                iter.remove();
            }
        }
    }

    /**
     * Clean up player data on quit
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        recentMessages.remove(uuid);
        // Keep warnings and lastWarningTime so they persist across rejoin
    }

    // ════════════════════════════════════════════════════════════════
    //  PUBLIC METHODS
    // ════════════════════════════════════════════════════════════════

    /**
     * Get warning count for a player
     */
    public int getWarnings(UUID uuid) {
        return warnings.getOrDefault(uuid, 0);
    }

    /**
     * Reset warnings for a player (admin command)
     */
    public void resetWarnings(UUID uuid) {
        warnings.remove(uuid);
        lastWarningTime.remove(uuid);
        recentMessages.remove(uuid);
        mutedUntil.remove(uuid);
    }

    /**
     * Check if player is currently muted
     */
    public boolean isMuted(UUID uuid) {
        Long end = mutedUntil.get(uuid);
        return end != null && System.currentTimeMillis() < end;
    }

    /**
     * Shutdown cleanup
     */
    public void shutdown() {
        recentMessages.clear();
        warnings.clear();
        lastWarningTime.clear();
        mutedUntil.clear();
        plugin.getLogger().info("[AntiSpam] System shutdown.");
    }
}
