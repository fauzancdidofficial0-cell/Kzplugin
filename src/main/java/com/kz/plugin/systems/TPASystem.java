// ============================================================
// PATH: src/main/java/com/kz/plugin/systems/TPASystem.java
// ============================================================
package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class TPASystem {

    private final KZPlugin plugin;

    // target UUID -> sender UUID
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();

    // sender UUID -> task ID (untuk cancel countdown kalau player quit/cancel)
    private final Map<UUID, Integer> countdownTasks = new HashMap<>();

    public TPASystem(KZPlugin plugin) {
        this.plugin = plugin;
    }

    // ============================================================
    //  SEND REQUEST
    // ============================================================

    public void sendRequest(Player sender, Player target) {
        UUID senderUUID = sender.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        if (sender.equals(target)) {
            sender.sendMessage("§c§lKZ §8» §7You cannot TPA to yourself.");
            return;
        }

        if (pendingRequests.containsKey(targetUUID)) {
            sender.sendMessage("§c§lKZ §8» §f" + target.getName()
                    + " §7already has a pending TPA request.");
            sender.playSound(sender.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        pendingRequests.put(targetUUID, senderUUID);

        // Notify sender
        sender.sendMessage("");
        sender.sendMessage("§a§lKZ §8» §7TPA request sent to §f" + target.getName() + "§7.");
        sender.sendMessage("  §7Auto-expires in §f60 seconds§7.");
        sender.sendMessage("");

        // Notify target
        target.sendMessage("");
        target.sendMessage("§6§l+------------------------------+");
        target.sendMessage("§f§l        TPA REQUEST");
        target.sendMessage("§6§l+------------------------------+");
        target.sendMessage("  §f" + sender.getName() + " §7wants to teleport to you.");
        target.sendMessage("  §7Type §a/tpaccept §7to accept.");
        target.sendMessage("  §7Type §c/tpadeny §7to decline.");
        target.sendMessage("§6§l+------------------------------+");
        target.sendMessage("");
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);

        // Auto-expire after 60 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingRequests.containsKey(targetUUID)
                    && pendingRequests.get(targetUUID).equals(senderUUID)) {
                pendingRequests.remove(targetUUID);
                if (sender.isOnline())
                    sender.sendMessage("§c§lKZ §8» §7TPA request to §f"
                            + target.getName() + " §7expired.");
                if (target.isOnline())
                    target.sendMessage("§c§lKZ §8» §7TPA request from §f"
                            + sender.getName() + " §7expired.");
            }
        }, 1200L);
    }

    // ============================================================
    //  ACCEPT REQUEST - dengan 5 detik countdown
    // ============================================================

    public void acceptRequest(Player target) {
        UUID targetUUID = target.getUniqueId();

        if (!pendingRequests.containsKey(targetUUID)) {
            target.sendMessage("§c§lKZ §8» §7No pending TPA requests.");
            return;
        }

        UUID   senderUUID = pendingRequests.remove(targetUUID);
        Player sender     = Bukkit.getPlayer(senderUUID);

        if (sender == null || !sender.isOnline()) {
            target.sendMessage("§c§lKZ §8» §7That player is no longer online.");
            return;
        }

        // Notify both players
        sender.sendMessage("");
        sender.sendMessage("§a§lKZ §8» §f" + target.getName()
                + " §7accepted your TPA request!");
        sender.sendMessage("  §7Teleporting in §e§l5 seconds§7...");
        sender.sendMessage("  §7§oDo not move!");
        sender.sendMessage("");

        target.sendMessage("");
        target.sendMessage("§a§lKZ §8» §7TPA accepted. §f" + sender.getName()
                + " §7will teleport to you in §e§l5 seconds§7.");
        target.sendMessage("");

        sender.playSound(sender.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);

        // ── Countdown 5, 4, 3, 2, 1 ──
        for (int sec = 5; sec >= 1; sec--) {
            final int s = sec;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!sender.isOnline()) return;

                String color = s <= 2 ? "§c" : s == 3 ? "§e" : "§a";
                sender.sendMessage("§8[§aTPA§8] " + color + "§lTeleporting in §f§l"
                        + s + color + "§l second" + (s > 1 ? "s" : "") + "...");
                sender.playSound(sender.getLocation(),
                        Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f,
                        0.5f + ((5 - s) * 0.15f));
            }, (long)(5 - sec) * 20L);
        }

        // ── Teleport setelah 5 detik ──
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Cek apakah sender masih online
            if (!sender.isOnline()) {
                target.sendMessage("§c§lKZ §8» §7Teleport cancelled: §f"
                        + sender.getName() + " §7went offline.");
                return;
            }

            // Cek apakah target masih online
            if (!target.isOnline()) {
                sender.sendMessage("§c§lKZ §8» §7Teleport cancelled: §f"
                        + target.getName() + " §7went offline.");
                return;
            }

            // Teleport!
            sender.teleport(target.getLocation());

            sender.sendMessage("");
            sender.sendMessage("§a§lKZ §8» §7Teleported to §f" + target.getName() + "§7.");
            sender.sendMessage("");
            target.sendMessage("§a§lKZ §8» §f" + sender.getName()
                    + " §7has teleported to you.");

            sender.playSound(sender.getLocation(),
                    Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            target.playSound(target.getLocation(),
                    Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

            // Cleanup task ID
            countdownTasks.remove(senderUUID);

        }, 5 * 20L).getTaskId();

        // Simpan task ID untuk keperluan cancel
        countdownTasks.put(senderUUID, taskId);
    }

    // ============================================================
    //  DENY REQUEST
    // ============================================================

    public void denyRequest(Player target) {
        UUID targetUUID = target.getUniqueId();

        if (!pendingRequests.containsKey(targetUUID)) {
            target.sendMessage("§c§lKZ §8» §7No pending TPA requests.");
            return;
        }

        UUID   senderUUID = pendingRequests.remove(targetUUID);
        Player sender     = Bukkit.getPlayer(senderUUID);

        target.sendMessage("§a§lKZ §8» §7TPA request declined.");
        target.playSound(target.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);

        if (sender != null && sender.isOnline()) {
            sender.sendMessage("§c§lKZ §8» §f" + target.getName()
                    + " §7declined your TPA request.");
            sender.playSound(sender.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    // ============================================================
    //  CANCEL REQUEST
    // ============================================================

    public void cancelRequest(Player sender) {
        UUID    senderUUID = sender.getUniqueId();
        boolean found      = false;

        Iterator<Map.Entry<UUID, UUID>> it = pendingRequests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> entry = it.next();
            if (entry.getValue().equals(senderUUID)) {
                it.remove();
                found = true;

                // Cancel countdown task kalau ada
                cancelCountdownTask(senderUUID);

                Player target = Bukkit.getPlayer(entry.getKey());
                if (target != null && target.isOnline()) {
                    target.sendMessage("§c§lKZ §8» §7TPA request from §f"
                            + sender.getName() + " §7was cancelled.");
                }
            }
        }

        if (found) {
            sender.sendMessage("§a§lKZ §8» §7TPA request cancelled.");
            sender.playSound(sender.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        } else {
            sender.sendMessage("§c§lKZ §8» §7You have no active TPA requests.");
        }
    }

    // ============================================================
    //  CLEANUP - Dipanggil saat player quit
    // ============================================================

    public void removeRequests(UUID uuid) {
        // Cancel countdown task kalau ada
        cancelCountdownTask(uuid);

        // Hapus sebagai target
        pendingRequests.remove(uuid);

        // Hapus sebagai sender
        pendingRequests.entrySet().removeIf(entry -> entry.getValue().equals(uuid));
    }

    // ============================================================
    //  HELPER
    // ============================================================

    private void cancelCountdownTask(UUID senderUUID) {
        Integer taskId = countdownTasks.remove(senderUUID);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}
