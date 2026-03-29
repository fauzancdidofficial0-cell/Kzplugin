package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class OneBlockSystem {

    private final KZPlugin plugin;

    // Phase data
    private final Map<Integer, Phase> phases = new LinkedHashMap<>();

    public static class Phase {
        public int id;
        public String name;
        public String color;
        public int requiredBreaks;
        public List<Material> blocks;
        public List<EntityType> mobs;
        public int mobSpawnChance; // 1 in X chance per break

        public Phase(int id, String name, String color, int requiredBreaks,
                     List<Material> blocks, List<EntityType> mobs, int mobSpawnChance) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.requiredBreaks = requiredBreaks;
            this.blocks = blocks;
            this.mobs = mobs;
            this.mobSpawnChance = mobSpawnChance;
        }
    }

    public OneBlockSystem(KZPlugin plugin) {
        this.plugin = plugin;
        loadPhases();
    }

    private void loadPhases() {

        // ══════════════════════════════════════
        //  PHASE 1: PLAINS (0 - 300)
        // ══════════════════════════════════════
        phases.put(1, new Phase(1, "Plains", "§a", 0,
            Arrays.asList(
                Material.GRASS_BLOCK, Material.DIRT, Material.OAK_LOG,
                Material.OAK_PLANKS, Material.COBBLESTONE, Material.GRASS_BLOCK,
                Material.DIRT, Material.OAK_LOG, Material.BIRCH_LOG,
                Material.OAK_LEAVES, Material.POPPY, Material.DANDELION,
                Material.TALL_GRASS, Material.FERN, Material.CLAY,
                Material.GRAVEL, Material.SUGAR_CANE, Material.PUMPKIN,
                Material.MELON, Material.WHEAT, Material.BROWN_MUSHROOM,
                Material.RED_MUSHROOM
            ),
            Arrays.asList(
                EntityType.COW, EntityType.PIG, EntityType.CHICKEN,
                EntityType.SHEEP, EntityType.RABBIT, EntityType.BEE
            ),
            25 // 1 in 25 chance
        ));

        // ══════════════════════════════════════
        //  PHASE 2: DESERT (300 - 650)
        // ══════════════════════════════════════
        phases.put(2, new Phase(2, "Desert", "§e", 300,
            Arrays.asList(
                Material.SAND, Material.SANDSTONE, Material.RED_SAND,
                Material.RED_SANDSTONE, Material.SMOOTH_SANDSTONE,
                Material.CHISELED_SANDSTONE, Material.CUT_SANDSTONE,
                Material.TERRACOTTA, Material.WHITE_TERRACOTTA,
                Material.ORANGE_TERRACOTTA, Material.YELLOW_TERRACOTTA,
                Material.RED_TERRACOTTA, Material.CACTUS, Material.DEAD_BUSH,
                Material.BONE_BLOCK, Material.COAL_ORE
            ),
            Arrays.asList(
                EntityType.HUSK, EntityType.RABBIT, EntityType.SPIDER
            ),
            20
        ));

        // ══════════════════════════════════════
        //  PHASE 3: FOREST (650 - 1050)
        // ══════════════════════════════════════
        phases.put(3, new Phase(3, "Forest", "§2", 650,
            Arrays.asList(
                Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
                Material.DARK_OAK_LOG, Material.OAK_LEAVES, Material.SPRUCE_LEAVES,
                Material.BIRCH_LEAVES, Material.DARK_OAK_LEAVES,
                Material.MOSS_BLOCK, Material.MOSSY_COBBLESTONE,
                Material.VINE, Material.GRASS_BLOCK, Material.PODZOL,
                Material.MYCELIUM, Material.BROWN_MUSHROOM_BLOCK,
                Material.RED_MUSHROOM_BLOCK, Material.BEE_NEST,
                Material.IRON_ORE, Material.COAL_ORE
            ),
            Arrays.asList(
                EntityType.ZOMBIE, EntityType.SPIDER, EntityType.CREEPER,
                EntityType.WOLF, EntityType.FOX, EntityType.PARROT
            ),
            18
        ));

        // ══════════════════════════════════════
        //  PHASE 4: SNOW (1050 - 1500)
        // ══════════════════════════════════════
        phases.put(4, new Phase(4, "Snow", "§f", 1050,
            Arrays.asList(
                Material.SNOW_BLOCK, Material.ICE, Material.PACKED_ICE,
                Material.BLUE_ICE, Material.POWDER_SNOW, Material.SPRUCE_LOG,
                Material.SPRUCE_PLANKS, Material.STONE, Material.IRON_ORE,
                Material.COAL_ORE, Material.COBBLESTONE, Material.GRAVEL,
                Material.CLAY, Material.FROSTED_ICE
            ),
            Arrays.asList(
                EntityType.STRAY, EntityType.POLAR_BEAR, EntityType.SNOW_GOLEM,
                EntityType.RABBIT, EntityType.WOLF
            ),
            18
        ));

        // ══════════════════════════════════════
        //  PHASE 5: UNDERGROUND (1500 - 2000)
        // ══════════════════════════════════════
        phases.put(5, new Phase(5, "Underground", "§8", 1500,
            Arrays.asList(
                Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE,
                Material.COBBLED_DEEPSLATE, Material.TUFF, Material.GRANITE,
                Material.DIORITE, Material.ANDESITE, Material.GRAVEL,
                Material.COAL_ORE, Material.IRON_ORE, Material.COPPER_ORE,
                Material.GOLD_ORE, Material.LAPIS_ORE, Material.REDSTONE_ORE,
                Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_IRON_ORE,
                Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_COPPER_ORE,
                Material.AMETHYST_BLOCK, Material.CALCITE,
                Material.DRIPSTONE_BLOCK, Material.POINTED_DRIPSTONE,
                Material.MOSS_BLOCK, Material.CLAY, Material.OBSIDIAN
            ),
            Arrays.asList(
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER,
                EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.SILVERFISH,
                EntityType.BAT, EntityType.SLIME
            ),
            15
        ));

        // ══════════════════════════════════════
        //  PHASE 6: OCEAN (2000 - 2500)
        // ══════════════════════════════════════
        phases.put(6, new Phase(6, "Ocean", "§9", 2000,
            Arrays.asList(
                Material.SAND, Material.GRAVEL, Material.CLAY,
                Material.PRISMARINE, Material.PRISMARINE_BRICKS,
                Material.DARK_PRISMARINE, Material.SEA_LANTERN,
                Material.SPONGE, Material.WET_SPONGE,
                Material.KELP, Material.SEAGRASS,
                Material.TUBE_CORAL_BLOCK, Material.BRAIN_CORAL_BLOCK,
                Material.BUBBLE_CORAL_BLOCK, Material.FIRE_CORAL_BLOCK,
                Material.HORN_CORAL_BLOCK, Material.MAGMA_BLOCK,
                Material.OBSIDIAN, Material.DIAMOND_ORE
            ),
            Arrays.asList(
                EntityType.DROWNED, EntityType.GUARDIAN, EntityType.SQUID,
                EntityType.GLOW_SQUID, EntityType.COD, EntityType.SALMON,
                EntityType.TROPICAL_FISH, EntityType.PUFFERFISH,
                EntityType.TURTLE, EntityType.DOLPHIN, EntityType.AXOLOTL
            ),
            15
        ));

        // ══════════════════════════════════════
        //  PHASE 7: JUNGLE (2500 - 3000)
        // ══════════════════════════════════════
        phases.put(7, new Phase(7, "Jungle", "§2", 2500,
            Arrays.asList(
                Material.JUNGLE_LOG, Material.JUNGLE_PLANKS,
                Material.JUNGLE_LEAVES, Material.BAMBOO,
                Material.VINE, Material.MELON, Material.COCOA,
                Material.MOSS_BLOCK, Material.MOSSY_COBBLESTONE,
                Material.MOSSY_STONE_BRICKS, Material.GRASS_BLOCK,
                Material.PODZOL, Material.MUD, Material.MANGROVE_LOG,
                Material.MANGROVE_ROOTS, Material.IRON_ORE,
                Material.GOLD_ORE, Material.DIAMOND_ORE,
                Material.EMERALD_ORE
            ),
            Arrays.asList(
                EntityType.OCELOT, EntityType.PARROT, EntityType.PANDA,
                EntityType.CREEPER, EntityType.SPIDER, EntityType.SKELETON,
                EntityType.WITCH, EntityType.FROG
            ),
            15
        ));

        // ══════════════════════════════════════
        //  PHASE 8: NETHER (3000 - 3600)
        // ══════════════════════════════════════
        phases.put(8, new Phase(8, "Nether", "§4", 3000,
            Arrays.asList(
                Material.NETHERRACK, Material.NETHER_BRICKS,
                Material.RED_NETHER_BRICKS, Material.SOUL_SAND,
                Material.SOUL_SOIL, Material.BASALT,
                Material.SMOOTH_BASALT, Material.BLACKSTONE,
                Material.POLISHED_BLACKSTONE, Material.POLISHED_BLACKSTONE_BRICKS,
                Material.GILDED_BLACKSTONE, Material.MAGMA_BLOCK,
                Material.GLOWSTONE, Material.SHROOMLIGHT,
                Material.CRIMSON_STEM, Material.WARPED_STEM,
                Material.CRIMSON_NYLIUM, Material.WARPED_NYLIUM,
                Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
                Material.ANCIENT_DEBRIS, Material.CRYING_OBSIDIAN,
                Material.OBSIDIAN
            ),
            Arrays.asList(
                EntityType.ZOMBIFIED_PIGLIN, EntityType.PIGLIN,
                EntityType.HOGLIN, EntityType.BLAZE,
                EntityType.WITHER_SKELETON, EntityType.GHAST,
                EntityType.MAGMA_CUBE, EntityType.STRIDER,
                EntityType.PIGLIN_BRUTE, EntityType.ENDERMAN
            ),
            12
        ));

        // ══════════════════════════════════════
        //  PHASE 9: SKY (3600 - 4200)
        // ══════════════════════════════════════
        phases.put(9, new Phase(9, "Sky", "§b", 3600,
            Arrays.asList(
                Material.WHITE_CONCRETE, Material.LIGHT_BLUE_CONCRETE,
                Material.CYAN_CONCRETE, Material.QUARTZ_BLOCK,
                Material.SMOOTH_QUARTZ, Material.CHISELED_QUARTZ_BLOCK,
                Material.AMETHYST_BLOCK, Material.AMETHYST_CLUSTER,
                Material.CALCITE, Material.IRON_BLOCK,
                Material.GOLD_BLOCK, Material.DIAMOND_BLOCK,
                Material.SCULK, Material.SCULK_CATALYST,
                Material.SCULK_SENSOR, Material.COPPER_BLOCK,
                Material.LIGHTNING_ROD, Material.END_ROD,
                Material.DIAMOND_ORE, Material.EMERALD_ORE
            ),
            Arrays.asList(
                EntityType.PHANTOM, EntityType.ALLAY, EntityType.VEX,
                EntityType.EVOKER, EntityType.ENDERMAN
            ),
            12
        ));

        // ══════════════════════════════════════
        //  PHASE 10: END (4200 - 5000)
        // ══════════════════════════════════════
        phases.put(10, new Phase(10, "End", "§5", 4200,
            Arrays.asList(
                Material.END_STONE, Material.END_STONE_BRICKS,
                Material.PURPUR_BLOCK, Material.PURPUR_PILLAR,
                Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
                Material.CHORUS_PLANT, Material.CHORUS_FLOWER,
                Material.SHULKER_BOX, Material.DRAGON_EGG,
                Material.EMERALD_BLOCK, Material.DIAMOND_BLOCK,
                Material.NETHERITE_BLOCK, Material.ANCIENT_DEBRIS,
                Material.BEACON
            ),
            Arrays.asList(
                EntityType.ENDERMAN, EntityType.SHULKER,
                EntityType.ENDERMITE, EntityType.PHANTOM
            ),
            10
        ));

        // ══════════════════════════════════════
        //  PHASE 11: VOID / INFINITY (5000+)
        //  Random dari SEMUA 10 fase!
        // ══════════════════════════════════════
        List<Material> allBlocks = new ArrayList<>();
        List<EntityType> allMobs = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Phase p = phases.get(i);
            allBlocks.addAll(p.blocks);
            allMobs.addAll(p.mobs);
        }
        // Remove duplicates
        allBlocks = new ArrayList<>(new LinkedHashSet<>(allBlocks));
        allMobs = new ArrayList<>(new LinkedHashSet<>(allMobs));

        phases.put(11, new Phase(11, "Infinity", "§d§l", 5000,
            allBlocks, allMobs, 8
        ));
    }

    // ══════════════════════════════════════
    //  GET CURRENT PHASE
    // ══════════════════════════════════════

    public Phase getCurrentPhase(int blocksBroken) {
        Phase current = phases.get(1);
        for (Phase phase : phases.values()) {
            if (blocksBroken >= phase.requiredBreaks) {
                current = phase;
            }
        }
        return current;
    }

    public Phase getNextPhase(int blocksBroken) {
        for (Phase phase : phases.values()) {
            if (blocksBroken < phase.requiredBreaks) {
                return phase;
            }
        }
        return null; // Already at max
    }

    // ══════════════════════════════════════
    //  HANDLE BLOCK BREAK
    // ══════════════════════════════════════

    public Material getRandomBlock(int blocksBroken) {
        Phase phase = getCurrentPhase(blocksBroken);
        Random random = new Random();
        return phase.blocks.get(random.nextInt(phase.blocks.size()));
    }

    public boolean shouldSpawnMob(int blocksBroken) {
        Phase phase = getCurrentPhase(blocksBroken);
        Random random = new Random();
        return random.nextInt(phase.mobSpawnChance) == 0;
    }

    public EntityType getRandomMob(int blocksBroken) {
        Phase phase = getCurrentPhase(blocksBroken);
        Random random = new Random();
        return phase.mobs.get(random.nextInt(phase.mobs.size()));
    }

    // ══════════════════════════════════════
    //  PROCESS BLOCK BREAK ON ONEBLOCK
    // ══════════════════════════════════════

    public void processBreak(Player player, Location center, int newBrokenCount) {
        UUID uuid = player.getUniqueId();
        int oldCount = newBrokenCount - 1;

        Phase oldPhase = getCurrentPhase(oldCount);
        Phase newPhase = getCurrentPhase(newBrokenCount);

        // Set new random block after 2 ticks
        new BukkitRunnable() {
            @Override
            public void run() {
                Material newBlock = getRandomBlock(newBrokenCount);
                center.getBlock().setType(newBlock);
            }
        }.runTaskLater(plugin, 2L);

        // Check phase transition
        if (oldPhase.id != newPhase.id) {
            announcePhaseChange(player, center, oldPhase, newPhase);
        }

        // Mob spawn check
        if (shouldSpawnMob(newBrokenCount)) {
            EntityType mobType = getRandomMob(newBrokenCount);
            Location spawnLoc = center.clone().add(0, 1.5, 0);
            try {
                center.getWorld().spawnEntity(spawnLoc, mobType);
                player.sendMessage("§c§l⚠ §7A §f" + formatEntityName(mobType) + " §7has appeared!");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
            } catch (Exception e) {
                // Some mobs can't be spawned, ignore
            }
        }

        // Progress notification (every 250 blocks)
        if (newBrokenCount % 250 == 0) {
            Phase current = getCurrentPhase(newBrokenCount);
            Phase next = getNextPhase(newBrokenCount);

            player.sendMessage("");
            player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("§f§l  PROGRESS UPDATE");
            player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("§7  Blocks Broken : §f" + newBrokenCount);
            player.sendMessage("§7  Current Phase : " + current.color + current.name);
            if (next != null) {
                int remaining = next.requiredBreaks - newBrokenCount;
                player.sendMessage("§7  Next Phase    : " + next.color + next.name + " §7(" + remaining + " blocks)");
            } else {
                player.sendMessage("§7  Status        : §d§l∞ INFINITY MODE");
            }
            player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("");

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
    }

    // ══════════════════════════════════════
    //  PHASE CHANGE ANNOUNCEMENT
    // ══════════════════════════════════════

    private void announcePhaseChange(Player player, Location center, Phase oldPhase, Phase newPhase) {
        // Set bedrock temporarily
        center.getBlock().setType(Material.BEDROCK);

        // Send messages
        player.sendMessage("");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        player.sendMessage("  " + newPhase.color + "§l⬆ PHASE " + newPhase.id + " UNLOCKED!");
        player.sendMessage("  " + newPhase.color + "§l" + newPhase.name.toUpperCase());
        player.sendMessage("");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        // Sound effects
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.5f);

        // Title
        player.sendTitle(
            newPhase.color + "§l⬆ PHASE " + newPhase.id,
            "§7" + newPhase.name + " Unlocked!",
            10, 60, 20
        );

        // Countdown on bedrock (5 seconds)
        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (countdown <= 0) {
                    // Replace bedrock with new phase block
                    Material newBlock = getRandomBlock(newPhase.requiredBreaks);
                    center.getBlock().setType(newBlock);

                    player.sendMessage(newPhase.color + "§l✦ §7Phase " + newPhase.id + " has begun. Break the block!");
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
                    cancel();
                    return;
                }

                // Show countdown via action bar
                String bar = "§e§l";
                for (int i = 0; i < countdown; i++) bar += "▮ ";
                for (int i = countdown; i < 5; i++) bar += "§8▮ ";

                player.sendActionBar(newPhase.color + "§l" + newPhase.name +
                    " §7starting in §f" + countdown + "s " + bar);

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f + (0.2f * (5 - countdown)));
                countdown--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // ══════════════════════════════════════
    //  GET PHASE INFO (for /islandsetting)
    // ══════════════════════════════════════

    public String getPhaseInfo(int blocksBroken) {
        Phase current = getCurrentPhase(blocksBroken);
        Phase next = getNextPhase(blocksBroken);

        StringBuilder sb = new StringBuilder();
        sb.append("§7  Phase   : ").append(current.color).append(current.name);
        sb.append(" §7(").append(current.id).append("/11)\n");
        sb.append("§7  Broken  : §f").append(blocksBroken).append(" blocks\n");

        if (next != null) {
            int remaining = next.requiredBreaks - blocksBroken;
            sb.append("§7  Next    : ").append(next.color).append(next.name);
            sb.append(" §7(").append(remaining).append(" blocks remaining)\n");

            // Progress bar
            int progress = blocksBroken - current.requiredBreaks;
            int total = next.requiredBreaks - current.requiredBreaks;
            int barLength = 20;
            int filled = (int) ((double) progress / total * barLength);

            sb.append("§7  Progress: §a");
            for (int i = 0; i < barLength; i++) {
                if (i < filled) sb.append("▮");
                else sb.append("§8▮");
            }
            sb.append(" §f").append((int) ((double) progress / total * 100)).append("%");
        } else {
            sb.append("§7  Status  : §d§l∞ INFINITY MODE\n");
            sb.append("§7  §7Random blocks & mobs from all 11 phases!");
        }

        return sb.toString();
    }

    // ══════════════════════════════════════
    //  UTILITY
    // ══════════════════════════════════════

    public Map<Integer, Phase> getPhases() {
        return phases;
    }

    public int getTotalPhases() {
        return phases.size();
    }

    private String formatEntityName(EntityType type) {
        String name = type.name().replace("_", " ");
        StringBuilder result = new StringBuilder();
        for (String word : name.split(" ")) {
            if (word.length() > 0) {
                result.append(word.substring(0, 1).toUpperCase())
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
}
