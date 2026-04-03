// ============================================================
// PATH: src/main/java/com/kz/plugin/systems/BedrockFormManager.java
// ============================================================
package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.utils.ServerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * BedrockFormManager - Handles Floodgate Cumulus Forms for Bedrock clients
 *
 * Uses reflection to call FloodgateApi + Cumulus so the plugin
 * compiles WITHOUT Floodgate as a hard dependency.
 *
 * If Floodgate is not present, all methods gracefully fallback
 * to chat messages.
 */
public class BedrockFormManager {

    private final KZPlugin plugin;
    private boolean floodgateAvailable = false;

    // Cached reflection objects
    private Object floodgateApi;
    private java.lang.reflect.Method sendFormMethod;
    private Class<?> simpleFormClass;
    private Class<?> modalFormClass;
    private Class<?> formBuilderClass;

    // ════════════════════════════════════════════════════════════════
    //  SERVER/MODE DEFINITIONS
    // ════════════════════════════════════════════════════════════════

    /**
     * Represents a game mode option shown in forms
     */
    public record ModeOption(
            String displayName,
            String mode,
            String targetServer,
            String icon,        // Bedrock form button image path (optional)
            String description
    ) {}

    // All available modes in the network
    private final List<ModeOption> allModes = List.of(
            new ModeOption("⚔ Survival", "survival", "survival",
                    "", "Classic survival experience"),
            new ModeOption("🌿 Vanilla", "vanilla", "survival",
                    "", "Pure vanilla gameplay"),
            new ModeOption("📦 OneBlock", "oneblock", "void",
                    "", "Start with one block, expand!"),
            new ModeOption("🏝 Skyblock", "skyblock", "void",
                    "", "Build your island in the sky"),
            new ModeOption("🌴 Island", "island", "custom",
                    "", "Tropical island survival"),
            new ModeOption("☠ Acid Island", "acid", "custom",
                    "", "Survive on acid waters!")
    );

    // Modes available per server (for mode selector when already on a backend server)
    private final Map<String, List<ModeOption>> serverModes = Map.of(
            "survival", List.of(
                    new ModeOption("⚔ Survival", "survival", "survival", "", "Classic survival"),
                    new ModeOption("🌿 Vanilla", "vanilla", "survival", "", "Pure vanilla")
            ),
            "void", List.of(
                    new ModeOption("📦 OneBlock", "oneblock", "void", "", "One block challenge"),
                    new ModeOption("🏝 Skyblock", "skyblock", "void", "", "Sky island")
            ),
            "custom", List.of(
                    new ModeOption("🌴 Island", "island", "custom", "", "Tropical island"),
                    new ModeOption("☠ Acid Island", "acid", "custom", "", "Acid survival")
            )
    );

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════

    public BedrockFormManager(KZPlugin plugin) {
        this.plugin = plugin;
        initFloodgate();
    }

    /**
     * Try to load FloodgateApi via reflection
     * This way the plugin compiles even without Floodgate jar
     */
    private void initFloodgate() {
        try {
            // Load FloodgateApi
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            floodgateApi = apiClass.getMethod("getInstance").invoke(null);
            sendFormMethod = apiClass.getMethod("sendForm", UUID.class,
                    Class.forName("org.geysermc.cumulus.form.Form"));

            // Load form classes
            simpleFormClass = Class.forName("org.geysermc.cumulus.form.SimpleForm");
            modalFormClass = Class.forName("org.geysermc.cumulus.form.ModalForm");

            floodgateAvailable = true;
            plugin.getLogger().info("[Bedrock] FloodgateApi detected. Bedrock forms enabled.");
        } catch (Exception e) {
            floodgateAvailable = false;
            plugin.getLogger().info("[Bedrock] FloodgateApi not found. Bedrock forms disabled (chat fallback).");
        }
    }

    /**
     * Check if FloodgateApi is available
     */
    public boolean isFloodgateAvailable() {
        return floodgateAvailable;
    }

    /**
     * Check if player is Bedrock
     */
    public boolean isBedrockPlayer(Player player) {
        if (!floodgateAvailable) return false;
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            return (boolean) apiClass.getMethod("isFloodgatePlayer", UUID.class)
                    .invoke(api, player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  FORM 1: SERVER SELECTOR (Lobby only)
    //  Tampilkan semua mode → kirim ke server target
    // ════════════════════════════════════════════════════════════════

    /**
     * Open server selector form for Bedrock player (used in lobby)
     * Shows all available game modes across all servers
     */
    public void openServerSelector(Player player) {
        if (!floodgateAvailable) {
            sendChatFallbackServerSelector(player);
            return;
        }

        try {
            // Build SimpleForm via reflection
            Class<?> builderClass = Class.forName("org.geysermc.cumulus.form.SimpleForm");
            Object builder = builderClass.getMethod("builder").invoke(null);

            // Title
            builder = builder.getClass().getMethod("title", String.class)
                    .invoke(builder, "§b§lKZ SERVER");

            // Content
            builder = builder.getClass().getMethod("content", String.class)
                    .invoke(builder, "§7Welcome to KZ Network!\n§7Select a game mode to play:\n");

            // Add mode buttons
            for (ModeOption mode : allModes) {
                builder = builder.getClass().getMethod("button", String.class)
                        .invoke(builder, mode.displayName() + "\n§7" + mode.description());
            }

            // Add extra buttons
            builder = builder.getClass().getMethod("button", String.class)
                    .invoke(builder, "🎰 Crate Preview\n§7View available crates");

            builder = builder.getClass().getMethod("button", String.class)
                    .invoke(builder, "📊 My Stats\n§7View your statistics");

            builder = builder.getClass().getMethod("button", String.class)
                    .invoke(builder, "❓ Help\n§7Commands & info");

            // Response handler
            final Object finalBuilder = builder;
            Class<?> responseClass = Class.forName("org.geysermc.cumulus.response.SimpleFormResponse");

            builder = finalBuilder.getClass().getMethod("validResultHandler",
                    Class.forName("java.util.function.BiConsumer")).invoke(finalBuilder,
                    (java.util.function.BiConsumer<Object, Object>) (form, response) -> {
                        try {
                            int clickedButton = (int) response.getClass()
                                    .getMethod("clickedButtonId").invoke(response);

                            // Schedule on main thread
                            Bukkit.getScheduler().runTask(plugin, () ->
                                    handleServerSelectorResponse(player, clickedButton));
                        } catch (Exception ex) {
                            plugin.getLogger().warning("[Bedrock] Form response error: " + ex.getMessage());
                        }
                    });

            // Build and send
            Object form = builder.getClass().getMethod("build").invoke(builder);
            sendFormMethod.invoke(floodgateApi, player.getUniqueId(), form);

        } catch (Exception e) {
            plugin.getLogger().warning("[Bedrock] Failed to open server selector: " + e.getMessage());
            sendChatFallbackServerSelector(player);
        }
    }

    /**
     * Handle response from server selector form
     */
    private void handleServerSelectorResponse(Player player, int buttonId) {
        if (buttonId < allModes.size()) {
            // Mode button clicked
            ModeOption selected = allModes.get(buttonId);
            String currentServer = plugin.getConfig().getString("server-name", "lobby");

            player.sendMessage("");
            player.sendMessage("§a§lKZ §8» §7Connecting to §b" + selected.displayName() + "§7...");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

            if (!currentServer.equalsIgnoreCase(selected.targetServer())) {
                // Cross-server transfer
                ServerUtils.sendToServer(plugin, player, selected.targetServer());
            } else {
                // Same server - teleport to mode spawn
                if (plugin.getLobbySystem() != null) {
                    var spawn = plugin.getLobbySystem().getModeSpawn(selected.mode());
                    if (spawn != null) {
                        player.teleport(spawn);
                        player.sendMessage("§a§lKZ §8» §7Teleported to §f"
                                + selected.displayName() + "§7.");
                    }
                }
                if (plugin.getEconomyManager() != null) {
                    plugin.getEconomyManager().setPlayerMode(player, selected.mode());
                }
            }

        } else if (buttonId == allModes.size()) {
            // Crate Preview button
            openCrateListForm(player);

        } else if (buttonId == allModes.size() + 1) {
            // Stats button
            openStatsForm(player);

        } else if (buttonId == allModes.size() + 2) {
            // Help button
            openHelpForm(player);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  FORM 2: MODE SELECTOR (Backend server)
    //  Tampilkan mode yang tersedia di server ini
    // ════════════════════════════════════════════════════════════════

    /**
     * Open mode selector for current backend server
     * Only shows modes available on the server the player is currently on
     */
    public void openModeSelector(Player player) {
        String currentServer = plugin.getConfig().getString("server-name", "lobby");

        // If on lobby, use server selector instead
        if ("lobby".equalsIgnoreCase(currentServer)) {
            openServerSelector(player);
            return;
        }

        List<ModeOption> modes = serverModes.getOrDefault(currentServer, List.of());

        if (!floodgateAvailable || modes.isEmpty()) {
            sendChatFallbackModeSelector(player, currentServer, modes);
            return;
        }

        try {
            Class<?> builderClass = Class.forName("org.geysermc.cumulus.form.SimpleForm");
            Object builder = builderClass.getMethod("builder").invoke(null);

            builder = builder.getClass().getMethod("title", String.class)
                    .invoke(builder, "§e§lSelect Mode");

            builder = builder.getClass().getMethod("content", String.class)
                    .invoke(builder, "§7Server: §f" + capitalize(currentServer)
                            + "\n§7Choose a mode:\n");

            // Add mode buttons for this server
            for (ModeOption mode : modes) {
                builder = builder.getClass().getMethod("button", String.class)
                        .invoke(builder, mode.displayName() + "\n§7" + mode.description());
            }

            // Back to lobby button
            builder = builder.getClass().getMethod("button", String.class)
                    .invoke(builder, "🏠 Back to Lobby\n§7Return to lobby");

            final Object finalBuilder = builder;
            final List<ModeOption> finalModes = modes;

            builder = finalBuilder.getClass().getMethod("validResultHandler",
                    Class.forName("java.util.function.BiConsumer")).invoke(finalBuilder,
                    (java.util.function.BiConsumer<Object, Object>) (form, response) -> {
                        try {
                            int clickedButton = (int) response.getClass()
                                    .getMethod("clickedButtonId").invoke(response);

                            Bukkit.getScheduler().runTask(plugin, () ->
                                    handleModeSelectorResponse(player, clickedButton, finalModes));
                        } catch (Exception ex) {
                            plugin.getLogger().warning("[Bedrock] Mode selector error: " + ex.getMessage());
                        }
                    });

            Object form = builder.getClass().getMethod("build").invoke(builder);
            sendFormMethod.invoke(floodgateApi, player.getUniqueId(), form);

        } catch (Exception e) {
            plugin.getLogger().warning("[Bedrock] Failed to open mode selector: " + e.getMessage());
            sendChatFallbackModeSelector(player, currentServer, modes);
        }
    }

    private void handleModeSelectorResponse(Player player, int buttonId, List<ModeOption> modes) {
        if (buttonId < modes.size()) {
            ModeOption selected = modes.get(buttonId);

            player.sendMessage("§a§lKZ §8» §7Switching to §b" + selected.displayName() + "§7...");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

            // Teleport to mode spawn on same server
            if (plugin.getLobbySystem() != null) {
                var spawn = plugin.getLobbySystem().getModeSpawn(selected.mode());
                if (spawn != null) {
                    player.teleport(spawn);
                }
            }

            // Update economy mode
            if (plugin.getEconomyManager() != null) {
                plugin.getEconomyManager().setPlayerMode(player, selected.mode());
            }

        } else {
            // Back to lobby
            player.sendMessage("§a§lKZ §8» §7Returning to lobby...");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            ServerUtils.sendToServer(plugin, player, "lobby");
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  FORM 3: CRATE LIST
    // ════════════════════════════════════════════════════════════════

    /**
     * Show list of all crates in a Bedrock form
     */
    public void openCrateListForm(Player player) {
        CrateSystem crateSystem = plugin.getCrateSystem();
        if (crateSystem == null) {
            player.sendMessage("§c§lKZ §8» §cCrate system is not available.");
            return;
        }

        var allCrates = crateSystem.getAllCrates();
        if (allCrates.isEmpty()) {
            player.sendMessage("§c§lKZ §8» §cNo crates available right now.");
            return;
        }

        if (!floodgateAvailable) {
            sendChatFallbackCrateList(player, allCrates);
            return;
        }

        try {
            Class<?> builderClass = Class.forName("org.geysermc.cumulus.form.SimpleForm");
            Object builder = builderClass.getMethod("builder").invoke(null);

            builder = builder.getClass().getMethod("title", String.class)
                    .invoke(builder, "§6§lGacha Crates");

            builder = builder.getClass().getMethod("content", String.class)
                    .invoke(builder, "§7Select a crate to view rewards:\n");

            List<String> crateIds = new ArrayList<>(allCrates.keySet());

            for (String crateId : crateIds) {
                CrateSystem.CrateData crate = allCrates.get(crateId);
                String keyName = getItemDisplayName(crate.keyItem);
                builder = builder.getClass().getMethod("button", String.class)
                        .invoke(builder, "🎰 " + crate.title
                                + "\n§7Key: " + keyName
                                + " | Rewards: " + crate.getTotalRewards());
            }

            final Object finalBuilder = builder;
            final List<String> finalCrateIds = crateIds;

            builder = finalBuilder.getClass().getMethod("validResultHandler",
                    Class.forName("java.util.function.BiConsumer")).invoke(finalBuilder,
                    (java.util.function.BiConsumer<Object, Object>) (form, response) -> {
                        try {
                            int clicked = (int) response.getClass()
                                    .getMethod("clickedButtonId").invoke(response);

                            if (clicked < finalCrateIds.size()) {
                                String selectedId = finalCrateIds.get(clicked);
                                Bukkit.getScheduler().runTask(plugin, () ->
                                        openCrateDetailForm(player, selectedId));
                            }
                        } catch (Exception ex) {
                            plugin.getLogger().warning("[Bedrock] Crate list error: " + ex.getMessage());
                        }
                    });

            Object form = builder.getClass().getMethod("build").invoke(builder);
            sendFormMethod.invoke(floodgateApi, player.getUniqueId(), form);

        } catch (Exception e) {
            plugin.getLogger().warning("[Bedrock] Failed to open crate list: " + e.getMessage());
            sendChatFallbackCrateList(player, allCrates);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  FORM 4: CRATE DETAIL (Modal - shows rewards)
    // ════════════════════════════════════════════════════════════════

    /**
     * Show detailed crate info with all rewards per rarity
     */
    public void openCrateDetailForm(Player player, String crateId) {
        CrateSystem crateSystem = plugin.getCrateSystem();
        if (crateSystem == null) return;

        CrateSystem.CrateData crate = crateSystem.getCrate(crateId);
        if (crate == null) {
            player.sendMessage("§c§lKZ §8» §cCrate not found.");
            return;
        }

        // Build reward text
        StringBuilder content = new StringBuilder();
        content.append("§b").append(crate.title).append("\n");
        content.append("§7").append(crate.description1).append("\n");
        content.append("§7").append(crate.description2).append("\n\n");
        content.append("§eKey Required: §f").append(getItemDisplayName(crate.keyItem)).append("\n\n");
        content.append("§6═══ REWARDS ═══\n\n");

        for (CrateSystem.Rarity rarity : CrateSystem.Rarity.values()) {
            List<ItemStack> items = crate.rewards.get(rarity);
            if (items == null || items.isEmpty()) continue;

            content.append(rarity.displayName).append(" §7(").append(rarity.weight).append("%)\n");
            for (ItemStack item : items) {
                content.append("  §f• ").append(item.getAmount()).append("x ")
                        .append(getItemDisplayName(item)).append("\n");
            }
            content.append("\n");
        }

        if (crate.getTotalRewards() == 0) {
            content.append("§cNo rewards configured yet.\n");
        }

        if (!floodgateAvailable) {
            // Chat fallback
            player.sendMessage("");
            for (String line : content.toString().split("\n")) {
                player.sendMessage("  " + line);
            }
            player.sendMessage("");
            return;
        }

        try {
            Class<?> builderClass = Class.forName("org.geysermc.cumulus.form.ModalForm");
            Object builder = builderClass.getMethod("builder").invoke(null);

            builder = builder.getClass().getMethod("title", String.class)
                    .invoke(builder, "§6 " + crate.title);

            builder = builder.getClass().getMethod("content", String.class)
                    .invoke(builder, content.toString());

            builder = builder.getClass().getMethod("button1", String.class)
                    .invoke(builder, "§a Back to Crate List");

            builder = builder.getClass().getMethod("button2", String.class)
                    .invoke(builder, "§c Close");

            final Object finalBuilder = builder;

            builder = finalBuilder.getClass().getMethod("validResultHandler",
                    Class.forName("java.util.function.BiConsumer")).invoke(finalBuilder,
                    (java.util.function.BiConsumer<Object, Object>) (form, response) -> {
                        try {
                            int clickedId = (int) response.getClass()
                                    .getMethod("clickedButtonId").invoke(response);
                            if (clickedId == 0) {
                                Bukkit.getScheduler().runTask(plugin, () ->
                                        openCrateListForm(player));
                            }
                        } catch (Exception ignored) {}
                    });

            Object form = builder.getClass().getMethod("build").invoke(builder);
            sendFormMethod.invoke(floodgateApi, player.getUniqueId(), form);

        } catch (Exception e) {
            plugin.getLogger().warning("[Bedrock] Failed to open crate detail: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  FORM 5: PLAYER STATS
    // ════════════════════════════════════════════════════════════════

    public void openStatsForm(Player player) {
        LobbySystem lobby = plugin.getLobbySystem();
        if (lobby == null) return;

        UUID uuid = player.getUniqueId();
        String rank = lobby.getRank(uuid);
        LobbySystem.RankData rd = lobby.getRankData(rank);
        String platform = lobby.getPlatform(uuid);

        double bal = 0;
        String modeName = "Lobby";
        if (plugin.getEconomyManager() != null) {
            bal = plugin.getEconomyManager().getBalance(player);
            String mode = plugin.getEconomyManager().getPlayerMode(player);
            modeName = plugin.getEconomyManager().getModeName(mode);
        }

        String jobDisplay = "None";
        try {
            if (plugin.getJobSystem() != null) {
                String job = plugin.getJobSystem().getJob(uuid);
                if (job != null) jobDisplay = capitalize(job);
            }
        } catch (Exception ignored) {}

        StringBuilder content = new StringBuilder();
        content.append("§f👤 Player: §b").append(player.getName()).append("\n");
        content.append("§f👑 Rank: ").append(rd.chatTag()).append("\n");
        content.append("§f🎮 Platform: §f").append(platform).append("\n\n");
        content.append("§f💰 Balance: §a$").append(String.format("%,.0f", bal)).append("\n");
        content.append("§f🌍 Mode: §f").append(modeName).append("\n");
        content.append("§f💼 Job: §f").append(jobDisplay).append("\n\n");
        content.append("§f🏠 Max Land: §f").append(rd.maxLandSize())
                .append("x").append(rd.maxLandSize()).append("\n");
        content.append("§f📍 Max Claims: §f").append(rd.maxClaims()).append("\n");
        content.append("§f🏡 Max Homes: §f").append(rd.maxHomes()).append("\n");

        if (!floodgateAvailable) {
            player.sendMessage("");
            for (String line : content.toString().split("\n")) {
                player.sendMessage("  " + line);
            }
            player.sendMessage("");
            return;
        }

        try {
            Class<?> builderClass = Class.forName("org.geysermc.cumulus.form.ModalForm");
            Object builder = builderClass.getMethod("builder").invoke(null);

            builder = builder.getClass().getMethod("title", String.class)
                    .invoke(builder, "§b§l Player Stats");
            builder = builder.getClass().getMethod("content", String.class)
                    .invoke(builder, content.toString());
            builder = builder.getClass().getMethod("button1", String.class)
                    .invoke(builder, "§a OK");
            builder = builder.getClass().getMethod("button2", String.class)
                    .invoke(builder, "§7 Close");

            Object form = builder.getClass().getMethod("build").invoke(builder);
            sendFormMethod.invoke(floodgateApi, player.getUniqueId(), form);

        } catch (Exception e) {
            plugin.getLogger().warning("[Bedrock] Failed to open stats form: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  FORM 6: HELP
    // ════════════════════════════════════════════════════════════════

    public void openHelpForm(Player player) {
        String content = """
                §b§lKZ SERVER COMMANDS
                
                §e/menu §7- Open this menu
                §e/lobby §7- Return to lobby
                §e/spawn §7- Go to mode spawn
                
                §6§lECONOMY
                §e/bal §7- Check balance
                §e/pay <player> <amount> §7- Pay someone
                §e/baltop §7- Richest players
                §e/shop §7- Open shop
                §e/sell §7- Sell items
                §e/daily §7- Daily reward
                §e/weekly §7- Weekly reward
                
                §a§lISLAND
                §e/createisland §7- Create island
                §e/home §7- Go to island
                §e/visit <player> §7- Visit island
                §e/invite <player> §7- Invite to island
                
                §d§lSOCIAL
                §e/tpa <player> §7- Teleport request
                §e/stats §7- View stats
                §e/discord §7- Discord link
                """;

        if (!floodgateAvailable) {
            player.sendMessage("");
            for (String line : content.split("\n")) {
                player.sendMessage("  " + line);
            }
            player.sendMessage("");
            return;
        }

        try {
            Class<?> builderClass = Class.forName("org.geysermc.cumulus.form.ModalForm");
            Object builder = builderClass.getMethod("builder").invoke(null);

            builder = builder.getClass().getMethod("title", String.class)
                    .invoke(builder, "§e§l Help & Commands");
            builder = builder.getClass().getMethod("content", String.class)
                    .invoke(builder, content);
            builder = builder.getClass().getMethod("button1", String.class)
                    .invoke(builder, "§a OK");
            builder = builder.getClass().getMethod("button2", String.class)
                    .invoke(builder, "§7 Close");

            Object form = builder.getClass().getMethod("build").invoke(builder);
            sendFormMethod.invoke(floodgateApi, player.getUniqueId(), form);

        } catch (Exception e) {
            plugin.getLogger().warning("[Bedrock] Failed to open help form: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  FORM 7: GENERIC CONFIRM (Yes/No)
    // ════════════════════════════════════════════════════════════════

    /**
     * Show a confirmation form for Bedrock player
     *
     * @param player   Target player
     * @param title    Form title
     * @param message  Question/message
     * @param onAccept Runnable to execute on "Yes"
     * @param onDeny   Runnable to execute on "No" (nullable)
     */
    public void openConfirmForm(Player player, String title, String message,
                                Runnable onAccept, Runnable onDeny) {
        if (!floodgateAvailable) {
            // Chat fallback - just run onAccept (assume yes)
            player.sendMessage("§e§lKZ §8» §7" + message);
            player.sendMessage("§a§lKZ §8» §7Auto-confirmed (Bedrock forms unavailable).");
            if (onAccept != null) onAccept.run();
            return;
        }

        try {
            Class<?> builderClass = Class.forName("org.geysermc.cumulus.form.ModalForm");
            Object builder = builderClass.getMethod("builder").invoke(null);

            builder = builder.getClass().getMethod("title", String.class)
                    .invoke(builder, title);
            builder = builder.getClass().getMethod("content", String.class)
                    .invoke(builder, message);
            builder = builder.getClass().getMethod("button1", String.class)
                    .invoke(builder, "§a Yes");
            builder = builder.getClass().getMethod("button2", String.class)
                    .invoke(builder, "§c No");

            final Object finalBuilder = builder;

            builder = finalBuilder.getClass().getMethod("validResultHandler",
                    Class.forName("java.util.function.BiConsumer")).invoke(finalBuilder,
                    (java.util.function.BiConsumer<Object, Object>) (form, response) -> {
                        try {
                            int clickedId = (int) response.getClass()
                                    .getMethod("clickedButtonId").invoke(response);

                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (clickedId == 0 && onAccept != null) {
                                    onAccept.run();
                                } else if (clickedId == 1 && onDeny != null) {
                                    onDeny.run();
                                }
                            });
                        } catch (Exception ignored) {}
                    });

            Object form = builder.getClass().getMethod("build").invoke(builder);
            sendFormMethod.invoke(floodgateApi, player.getUniqueId(), form);

        } catch (Exception e) {
            plugin.getLogger().warning("[Bedrock] Failed to open confirm form: " + e.getMessage());
            if (onAccept != null) onAccept.run();
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CHAT FALLBACKS - When Floodgate is not available
    // ════════════════════════════════════════════════════════════════

    private void sendChatFallbackServerSelector(Player player) {
        player.sendMessage("");
        player.sendMessage("§b§l┌─────────────────────────────────┐");
        player.sendMessage("§b§l│      §f§lSERVER SELECTOR             §b§l│");
        player.sendMessage("§b§l└─────────────────────────────────┘");
        player.sendMessage("");
        int i = 1;
        for (ModeOption mode : allModes) {
            player.sendMessage("  §7" + i + ". " + mode.displayName()
                    + " §8- §7" + mode.description()
                    + " §8[§e" + mode.targetServer() + "§8]");
            i++;
        }
        player.sendMessage("");
        player.sendMessage("  §7Use §e/lobby <mode> §7to join.");
        player.sendMessage("  §7Example: §f/lobby survival");
        player.sendMessage("");
    }

    private void sendChatFallbackModeSelector(Player player, String currentServer,
                                               List<ModeOption> modes) {
        player.sendMessage("");
        player.sendMessage("§e§l┌─────────────────────────────────┐");
        player.sendMessage("§e§l│      §f§lMODE SELECTOR               §e§l│");
        player.sendMessage("§e§l└─────────────────────────────────┘");
        player.sendMessage("  §7Server: §f" + capitalize(currentServer));
        player.sendMessage("");
        if (modes.isEmpty()) {
            player.sendMessage("  §7No modes available on this server.");
        } else {
            for (ModeOption mode : modes) {
                player.sendMessage("  §7• " + mode.displayName() + " §8- §7" + mode.description());
            }
        }
        player.sendMessage("");
        player.sendMessage("  §7Use §e/lobby §7to return to lobby.");
        player.sendMessage("");
    }

    private void sendChatFallbackCrateList(Player player,
                                            Map<String, CrateSystem.CrateData> allCrates) {
        player.sendMessage("");
        player.sendMessage("§6§l┌─────────────────────────────────┐");
        player.sendMessage("§6§l│      §f§lAVAILABLE CRATES            §6§l│");
        player.sendMessage("§6§l└─────────────────────────────────┘");
        player.sendMessage("");
        for (CrateSystem.CrateData crate : allCrates.values()) {
            player.sendMessage("  §7• §b" + crate.title
                    + " §8| §7Rewards: §f" + crate.getTotalRewards()
                    + " §8| §7Key: §e" + getItemDisplayName(crate.keyItem));
        }
        player.sendMessage("");
        player.sendMessage("  §7Use §e/gachapreview §7to view rewards.");
        player.sendMessage("");
    }

    // ════════════════════════════════════════════════════════════════
    //  UTILITY
    // ════════════════════════════════════════════════════════════════

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private String getItemDisplayName(ItemStack item) {
        if (item == null) return "Unknown";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        String name = item.getType().name().replace("_", " ");
        StringBuilder result = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        return result.toString().trim();
    }
}
