package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class TPASystem {

    private final KZPlugin plugin;
    private final Map<UUID, UUID> pendingRequests = new HashMap<>(); // target -> sender

    public TPASystem(KZPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendRequest(Player sender, Player target) {
        UUID senderUUID = sender.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        if (pendingRequests.containsKey(targetUUID)) {
            sender.sendMessage("§c§lKZ §8» §7" + target.getName() + " already has a pending request.");
            sender.playSound(sender.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        pendingRequests.put(targetUUID, senderUUID);

        sender.sendMessage("§a§lKZ §8» §7TPA request sent to §f" + target.getName() + "§7.");
        sender.sendMessage("§7  Auto-expires in §f60 seconds§7.");

        target.sendMessage("");
        target.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        target.sendMessage("§f§l  TPA REQUEST");
        target.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        target.sendMessage("§7  §f" + sender.getName() + " §7wants to teleport to you.");
        target.sendMessage("§7  Type §a/tpaccept §7to accept.");
        target.sendMessage("§7  Type §c/tpadeny §7to decline.");
        target.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        target.sendMessage("");

        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        // Auto-expire
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingRequests.containsKey(targetUUID) &&
                pendingRequests.get(targetUUID).equals(senderUUID)) {
                pendingRequests.remove(targetUUID);
                if (sender.isOnline())
                    sender.sendMessage("§c§lKZ §8» §7TPA request to §f" + target.getName() + " §7expired.");
                if (target.isOnline())
                    target.sendMessage("§c§lKZ §8» §7TPA request from §f" + sender.getName() + " §7expired.");
            }
        }, 1200L);
    }

    public void acceptRequest(Player target) {
        UUID targetUUID = target.getUniqueId();

        if (!pendingRequests.containsKey(targetUUID)) {
            target.sendMessage("§c§lKZ §8» §7No pending TPA requests.");
            return;
        }

        UUID senderUUID = pendingRequests.remove(targetUUID);
        Player sender = Bukkit.getPlayer(senderUUID);

        if (sender == null || !sender.isOnline()) {
            target.sendMessage("§c§lKZ §8» §7Player is no longer online.");
            return;
        }

        sender.teleport(target.getLocation());
        sender.sendMessage("§a§lKZ §8» §7Teleported to §f" + target.getName() + "§7.");
        target.sendMessage("§a§lKZ §8» §7TPA accepted. §f" + sender.getName() + " §7teleported to you.");

        sender.playSound(sender.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }

    public void denyRequest(Player target) {
        UUID targetUUID = target.getUniqueId();

        if (!pendingRequests.containsKey(targetUUID)) {
            target.sendMessage("§c§lKZ §8» §7No pending TPA requests.");
            return;
        }

        UUID senderUUID = pendingRequests.remove(targetUUID);
        target.sendMessage("§a§lKZ §8» §7TPA request declined.");

        Player sender = Bukkit.getPlayer(senderUUID);
        if (sender != null && sender.isOnline()) {
            sender.sendMessage("§c§lKZ §8» §f" + target.getName() + " §7declined your TPA request.");
        }
    }

    public void cancelRequest(Player sender) {
        UUID senderUUID = sender.getUniqueId();
        boolean found = false;

        Iterator<Map.Entry<UUID, UUID>> it = pendingRequests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> entry = it.next();
            if (entry.getValue().equals(senderUUID)) {
                it.remove();
                found = true;
                Player target = Bukkit.getPlayer(entry.getKey());
                if (target != null && target.isOnline()) {
                    target.sendMessage("§c§lKZ §8» §7TPA from §f" + sender.getName() + " §7was cancelled.");
                }
            }
        }

        if (found) {
            sender.sendMessage("§a§lKZ §8» §7TPA request cancelled.");
        } else {
            sender.sendMessage("§c§lKZ §8» §7No active TPA requests.");
        }
    }

    // Method baru untuk clear requests saat player quit
    public void removeRequests(UUID uuid) {
        pendingRequests.remove(uuid);
        pendingRequests.entrySet().removeIf(entry -> entry.getValue().equals(uuid));
    }
}
