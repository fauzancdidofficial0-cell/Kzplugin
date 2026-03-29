package com.kz.plugin.data;

import org.bukkit.Material;
import java.util.*;

public class ItemDatabase {

    public static class ShopItem {
        public Material material;
        public int buyPrice;
        public int sellPrice;
        public String category;

        public ShopItem(Material material, int buyPrice, int sellPrice, String category) {
            this.material = material;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.category = category;
        }
    }

    public static class Category {
        public String id;
        public String name;
        public String color;
        public Material icon;
        public List<ShopItem> items = new ArrayList<>();

        public Category(String id, String name, String color, Material icon) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.icon = icon;
        }
    }

    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final Map<Material, Integer> sellPrices = new HashMap<>();
    private int totalItems = 0;

    public ItemDatabase() {
        loadCategories();
    }

    private void loadCategories() {

        // ══════════════════════════════════════
        //  CATEGORY 1: TOOLS & WEAPONS
        // ══════════════════════════════════════
        Category tools = new Category("tools", "§b§lTools & Weapons", "§b", Material.DIAMOND_SWORD);

        // Wooden Tier
        tools.items.add(new ShopItem(Material.WOODEN_PICKAXE, 500, 50, "tools"));
        tools.items.add(new ShopItem(Material.WOODEN_AXE, 500, 50, "tools"));
        tools.items.add(new ShopItem(Material.WOODEN_SHOVEL, 400, 40, "tools"));
        tools.items.add(new ShopItem(Material.WOODEN_HOE, 400, 40, "tools"));
        tools.items.add(new ShopItem(Material.WOODEN_SWORD, 600, 60, "tools"));

        // Stone Tier
        tools.items.add(new ShopItem(Material.STONE_PICKAXE, 1500, 150, "tools"));
        tools.items.add(new ShopItem(Material.STONE_AXE, 1500, 150, "tools"));
        tools.items.add(new ShopItem(Material.STONE_SHOVEL, 1200, 120, "tools"));
        tools.items.add(new ShopItem(Material.STONE_HOE, 1200, 120, "tools"));
        tools.items.add(new ShopItem(Material.STONE_SWORD, 1800, 180, "tools"));

        // Iron Tier
        tools.items.add(new ShopItem(Material.IRON_PICKAXE, 5000, 500, "tools"));
        tools.items.add(new ShopItem(Material.IRON_AXE, 5000, 500, "tools"));
        tools.items.add(new ShopItem(Material.IRON_SHOVEL, 4000, 400, "tools"));
        tools.items.add(new ShopItem(Material.IRON_HOE, 4000, 400, "tools"));
        tools.items.add(new ShopItem(Material.IRON_SWORD, 6000, 600, "tools"));

        // Gold Tier
        tools.items.add(new ShopItem(Material.GOLDEN_PICKAXE, 8000, 800, "tools"));
        tools.items.add(new ShopItem(Material.GOLDEN_AXE, 8000, 800, "tools"));
        tools.items.add(new ShopItem(Material.GOLDEN_SHOVEL, 7000, 700, "tools"));
        tools.items.add(new ShopItem(Material.GOLDEN_HOE, 7000, 700, "tools"));
        tools.items.add(new ShopItem(Material.GOLDEN_SWORD, 10000, 1000, "tools"));

        // Diamond Tier
        tools.items.add(new ShopItem(Material.DIAMOND_PICKAXE, 25000, 2500, "tools"));
        tools.items.add(new ShopItem(Material.DIAMOND_AXE, 25000, 2500, "tools"));
        tools.items.add(new ShopItem(Material.DIAMOND_SHOVEL, 20000, 2000, "tools"));
        tools.items.add(new ShopItem(Material.DIAMOND_HOE, 20000, 2000, "tools"));
        tools.items.add(new ShopItem(Material.DIAMOND_SWORD, 30000, 3000, "tools"));

        // Netherite Tier
        tools.items.add(new ShopItem(Material.NETHERITE_PICKAXE, 100000, 10000, "tools"));
        tools.items.add(new ShopItem(Material.NETHERITE_AXE, 100000, 10000, "tools"));
        tools.items.add(new ShopItem(Material.NETHERITE_SHOVEL, 80000, 8000, "tools"));
        tools.items.add(new ShopItem(Material.NETHERITE_HOE, 80000, 8000, "tools"));
        tools.items.add(new ShopItem(Material.NETHERITE_SWORD, 120000, 12000, "tools"));

        // Ranged
        tools.items.add(new ShopItem(Material.BOW, 5000, 500, "tools"));
        tools.items.add(new ShopItem(Material.CROSSBOW, 15000, 1500, "tools"));
        tools.items.add(new ShopItem(Material.ARROW, 100, 10, "tools"));
        tools.items.add(new ShopItem(Material.SPECTRAL_ARROW, 500, 50, "tools"));
        tools.items.add(new ShopItem(Material.TIPPED_ARROW, 800, 80, "tools"));
        tools.items.add(new ShopItem(Material.TRIDENT, 50000, 5000, "tools"));

        // Utility
        tools.items.add(new ShopItem(Material.SHIELD, 8000, 800, "tools"));
        tools.items.add(new ShopItem(Material.FISHING_ROD, 3000, 300, "tools"));
        tools.items.add(new ShopItem(Material.SHEARS, 2000, 200, "tools"));
        tools.items.add(new ShopItem(Material.FLINT_AND_STEEL, 3000, 300, "tools"));
        tools.items.add(new ShopItem(Material.LEAD, 2500, 250, "tools"));
        tools.items.add(new ShopItem(Material.NAME_TAG, 10000, 1000, "tools"));
        tools.items.add(new ShopItem(Material.SPYGLASS, 5000, 500, "tools"));
        tools.items.add(new ShopItem(Material.COMPASS, 3000, 300, "tools"));
        tools.items.add(new ShopItem(Material.CLOCK, 3000, 300, "tools"));
        tools.items.add(new ShopItem(Material.RECOVERY_COMPASS, 25000, 2500, "tools"));
        tools.items.add(new ShopItem(Material.BRUSH, 5000, 500, "tools"));

        categories.put("tools", tools);

        // ══════════════════════════════════════
        //  CATEGORY 2: ARMOR
        // ══════════════════════════════════════
        Category armor = new Category("armor", "§d§lArmor", "§d", Material.DIAMOND_CHESTPLATE);

        // Leather
        armor.items.add(new ShopItem(Material.LEATHER_HELMET, 800, 80, "armor"));
        armor.items.add(new ShopItem(Material.LEATHER_CHESTPLATE, 1200, 120, "armor"));
        armor.items.add(new ShopItem(Material.LEATHER_LEGGINGS, 1000, 100, "armor"));
        armor.items.add(new ShopItem(Material.LEATHER_BOOTS, 600, 60, "armor"));

        // Chainmail
        armor.items.add(new ShopItem(Material.CHAINMAIL_HELMET, 3000, 300, "armor"));
        armor.items.add(new ShopItem(Material.CHAINMAIL_CHESTPLATE, 5000, 500, "armor"));
        armor.items.add(new ShopItem(Material.CHAINMAIL_LEGGINGS, 4000, 400, "armor"));
        armor.items.add(new ShopItem(Material.CHAINMAIL_BOOTS, 2500, 250, "armor"));

        // Iron
        armor.items.add(new ShopItem(Material.IRON_HELMET, 5000, 500, "armor"));
        armor.items.add(new ShopItem(Material.IRON_CHESTPLATE, 8000, 800, "armor"));
        armor.items.add(new ShopItem(Material.IRON_LEGGINGS, 7000, 700, "armor"));
        armor.items.add(new ShopItem(Material.IRON_BOOTS, 4000, 400, "armor"));

        // Gold
        armor.items.add(new ShopItem(Material.GOLDEN_HELMET, 8000, 800, "armor"));
        armor.items.add(new ShopItem(Material.GOLDEN_CHESTPLATE, 12000, 1200, "armor"));
        armor.items.add(new ShopItem(Material.GOLDEN_LEGGINGS, 10000, 1000, "armor"));
        armor.items.add(new ShopItem(Material.GOLDEN_BOOTS, 6000, 600, "armor"));

        // Diamond
        armor.items.add(new ShopItem(Material.DIAMOND_HELMET, 20000, 2000, "armor"));
        armor.items.add(new ShopItem(Material.DIAMOND_CHESTPLATE, 35000, 3500, "armor"));
        armor.items.add(new ShopItem(Material.DIAMOND_LEGGINGS, 30000, 3000, "armor"));
        armor.items.add(new ShopItem(Material.DIAMOND_BOOTS, 18000, 1800, "armor"));

        // Netherite
        armor.items.add(new ShopItem(Material.NETHERITE_HELMET, 80000, 8000, "armor"));
        armor.items.add(new ShopItem(Material.NETHERITE_CHESTPLATE, 120000, 12000, "armor"));
        armor.items.add(new ShopItem(Material.NETHERITE_LEGGINGS, 100000, 10000, "armor"));
        armor.items.add(new ShopItem(Material.NETHERITE_BOOTS, 70000, 7000, "armor"));

        // Special
        armor.items.add(new ShopItem(Material.ELYTRA, 250000, 25000, "armor"));
        armor.items.add(new ShopItem(Material.TURTLE_HELMET, 30000, 3000, "armor"));
        armor.items.add(new ShopItem(Material.SADDLE, 15000, 1500, "armor"));
        armor.items.add(new ShopItem(Material.HORSE_ARMOR_IRON, 10000, 1000, "armor"));
        armor.items.add(new ShopItem(Material.HORSE_ARMOR_GOLD, 20000, 2000, "armor"));
        armor.items.add(new ShopItem(Material.HORSE_ARMOR_DIAMOND, 35000, 3500, "armor"));

        categories.put("armor", armor);

        // ══════════════════════════════════════
        //  CATEGORY 3: BLOCKS
        // ══════════════════════════════════════
        Category blocks = new Category("blocks", "§a§lBlocks", "§a", Material.GRASS_BLOCK);

        // Natural
        blocks.items.add(new ShopItem(Material.DIRT, 50, 5, "blocks"));
        blocks.items.add(new ShopItem(Material.GRASS_BLOCK, 100, 10, "blocks"));
        blocks.items.add(new ShopItem(Material.PODZOL, 150, 15, "blocks"));
        blocks.items.add(new ShopItem(Material.MYCELIUM, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.COARSE_DIRT, 75, 7, "blocks"));
        blocks.items.add(new ShopItem(Material.ROOTED_DIRT, 100, 10, "blocks"));
        blocks.items.add(new ShopItem(Material.MUD, 100, 10, "blocks"));
        blocks.items.add(new ShopItem(Material.COBBLESTONE, 50, 5, "blocks"));
        blocks.items.add(new ShopItem(Material.STONE, 100, 10, "blocks"));
        blocks.items.add(new ShopItem(Material.SMOOTH_STONE, 150, 15, "blocks"));
        blocks.items.add(new ShopItem(Material.DEEPSLATE, 150, 15, "blocks"));
        blocks.items.add(new ShopItem(Material.COBBLED_DEEPSLATE, 100, 10, "blocks"));
        blocks.items.add(new ShopItem(Material.SAND, 100, 10, "blocks"));
        blocks.items.add(new ShopItem(Material.RED_SAND, 150, 15, "blocks"));
        blocks.items.add(new ShopItem(Material.GRAVEL, 75, 7, "blocks"));
        blocks.items.add(new ShopItem(Material.CLAY, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.SANDSTONE, 150, 15, "blocks"));
        blocks.items.add(new ShopItem(Material.RED_SANDSTONE, 200, 20, "blocks"));

        // Stone Variants
        blocks.items.add(new ShopItem(Material.ANDESITE, 75, 7, "blocks"));
        blocks.items.add(new ShopItem(Material.DIORITE, 75, 7, "blocks"));
        blocks.items.add(new ShopItem(Material.GRANITE, 75, 7, "blocks"));
        blocks.items.add(new ShopItem(Material.POLISHED_ANDESITE, 150, 15, "blocks"));
        blocks.items.add(new ShopItem(Material.POLISHED_DIORITE, 150, 15, "blocks"));
        blocks.items.add(new ShopItem(Material.POLISHED_GRANITE, 150, 15, "blocks"));
        blocks.items.add(new ShopItem(Material.CALCITE, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.TUFF, 100, 10, "blocks"));
        blocks.items.add(new ShopItem(Material.DRIPSTONE_BLOCK, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.BASALT, 150, 15, "blocks"));
        blocks.items.add(new ShopItem(Material.SMOOTH_BASALT, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.BLACKSTONE, 200, 20, "blocks"));

        // Logs
        blocks.items.add(new ShopItem(Material.OAK_LOG, 150, 15, "blocks"));
        blocks.items.add(new ShopItem(Material.SPRUCE_LOG, 150, 15, "blocks"));
        blocks.items.add(new ShopItem(Material.BIRCH_LOG, 150, 15, "blocks"));
        blocks.items.add(new ShopItem(Material.JUNGLE_LOG, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.ACACIA_LOG, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.DARK_OAK_LOG, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.MANGROVE_LOG, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.CHERRY_LOG, 300, 30, "blocks"));
        blocks.items.add(new ShopItem(Material.CRIMSON_STEM, 500, 50, "blocks"));
        blocks.items.add(new ShopItem(Material.WARPED_STEM, 500, 50, "blocks"));

        // Planks
        blocks.items.add(new ShopItem(Material.OAK_PLANKS, 50, 5, "blocks"));
        blocks.items.add(new ShopItem(Material.SPRUCE_PLANKS, 50, 5, "blocks"));
        blocks.items.add(new ShopItem(Material.BIRCH_PLANKS, 50, 5, "blocks"));
        blocks.items.add(new ShopItem(Material.JUNGLE_PLANKS, 75, 7, "blocks"));
        blocks.items.add(new ShopItem(Material.ACACIA_PLANKS, 75, 7, "blocks"));
        blocks.items.add(new ShopItem(Material.DARK_OAK_PLANKS, 75, 7, "blocks"));
        blocks.items.add(new ShopItem(Material.MANGROVE_PLANKS, 100, 10, "blocks"));
        blocks.items.add(new ShopItem(Material.CHERRY_PLANKS, 125, 12, "blocks"));
        blocks.items.add(new ShopItem(Material.CRIMSON_PLANKS, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.WARPED_PLANKS, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.BAMBOO_PLANKS, 100, 10, "blocks"));

        // Glass & Ice
        blocks.items.add(new ShopItem(Material.GLASS, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.TINTED_GLASS, 1000, 100, "blocks"));
        blocks.items.add(new ShopItem(Material.ICE, 300, 30, "blocks"));
        blocks.items.add(new ShopItem(Material.PACKED_ICE, 500, 50, "blocks"));
        blocks.items.add(new ShopItem(Material.BLUE_ICE, 1000, 100, "blocks"));

        // Nether & End
        blocks.items.add(new ShopItem(Material.NETHERRACK, 50, 5, "blocks"));
        blocks.items.add(new ShopItem(Material.SOUL_SAND, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.SOUL_SOIL, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.MAGMA_BLOCK, 300, 30, "blocks"));
        blocks.items.add(new ShopItem(Material.OBSIDIAN, 1500, 150, "blocks"));
        blocks.items.add(new ShopItem(Material.CRYING_OBSIDIAN, 3000, 300, "blocks"));
        blocks.items.add(new ShopItem(Material.END_STONE, 500, 50, "blocks"));
        blocks.items.add(new ShopItem(Material.END_STONE_BRICKS, 750, 75, "blocks"));
        blocks.items.add(new ShopItem(Material.PURPUR_BLOCK, 800, 80, "blocks"));

        // Ore blocks
        blocks.items.add(new ShopItem(Material.COAL_BLOCK, 2000, 200, "blocks"));
        blocks.items.add(new ShopItem(Material.IRON_BLOCK, 10000, 1000, "blocks"));
        blocks.items.add(new ShopItem(Material.GOLD_BLOCK, 15000, 1500, "blocks"));
        blocks.items.add(new ShopItem(Material.DIAMOND_BLOCK, 50000, 5000, "blocks"));
        blocks.items.add(new ShopItem(Material.EMERALD_BLOCK, 40000, 4000, "blocks"));
        blocks.items.add(new ShopItem(Material.NETHERITE_BLOCK, 500000, 50000, "blocks"));
        blocks.items.add(new ShopItem(Material.LAPIS_BLOCK, 8000, 800, "blocks"));
        blocks.items.add(new ShopItem(Material.REDSTONE_BLOCK, 5000, 500, "blocks"));
        blocks.items.add(new ShopItem(Material.COPPER_BLOCK, 3000, 300, "blocks"));
        blocks.items.add(new ShopItem(Material.AMETHYST_BLOCK, 5000, 500, "blocks"));

        // Wool
        blocks.items.add(new ShopItem(Material.WHITE_WOOL, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.RED_WOOL, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.BLUE_WOOL, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.GREEN_WOOL, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.YELLOW_WOOL, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.BLACK_WOOL, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.ORANGE_WOOL, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.PURPLE_WOOL, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.PINK_WOOL, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.CYAN_WOOL, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.LIGHT_BLUE_WOOL, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.LIME_WOOL, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.MAGENTA_WOOL, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.BROWN_WOOL, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.GRAY_WOOL, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.LIGHT_GRAY_WOOL, 200, 20, "blocks"));

        // Concrete
        blocks.items.add(new ShopItem(Material.WHITE_CONCRETE, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.RED_CONCRETE, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.BLUE_CONCRETE, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.GREEN_CONCRETE, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.YELLOW_CONCRETE, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.BLACK_CONCRETE, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.ORANGE_CONCRETE, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.PURPLE_CONCRETE, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.PINK_CONCRETE, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.CYAN_CONCRETE, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.LIGHT_BLUE_CONCRETE, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.LIME_CONCRETE, 250, 25, "blocks"));

        // Terracotta
        blocks.items.add(new ShopItem(Material.TERRACOTTA, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.WHITE_TERRACOTTA, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.RED_TERRACOTTA, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.BLUE_TERRACOTTA, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.GREEN_TERRACOTTA, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.YELLOW_TERRACOTTA, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.BLACK_TERRACOTTA, 250, 25, "blocks"));
        blocks.items.add(new ShopItem(Material.ORANGE_TERRACOTTA, 250, 25, "blocks"));

        // Prismarine & Ocean
        blocks.items.add(new ShopItem(Material.PRISMARINE, 500, 50, "blocks"));
        blocks.items.add(new ShopItem(Material.PRISMARINE_BRICKS, 750, 75, "blocks"));
        blocks.items.add(new ShopItem(Material.DARK_PRISMARINE, 750, 75, "blocks"));
        blocks.items.add(new ShopItem(Material.SEA_LANTERN, 1000, 100, "blocks"));
        blocks.items.add(new ShopItem(Material.SPONGE, 5000, 500, "blocks"));

        // Stone Bricks
        blocks.items.add(new ShopItem(Material.STONE_BRICKS, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.MOSSY_STONE_BRICKS, 300, 30, "blocks"));
        blocks.items.add(new ShopItem(Material.CRACKED_STONE_BRICKS, 300, 30, "blocks"));
        blocks.items.add(new ShopItem(Material.CHISELED_STONE_BRICKS, 400, 40, "blocks"));
        blocks.items.add(new ShopItem(Material.MOSSY_COBBLESTONE, 200, 20, "blocks"));
        blocks.items.add(new ShopItem(Material.BRICKS, 300, 30, "blocks"));
        blocks.items.add(new ShopItem(Material.SNOW_BLOCK, 200, 20, "blocks"));

        categories.put("blocks", blocks);

        // ══════════════════════════════════════
        //  CATEGORY 4: BUILDING
        // ══════════════════════════════════════
        Category building = new Category("building", "§6§lBuilding", "§6", Material.BRICKS);

        // Stairs
        building.items.add(new ShopItem(Material.OAK_STAIRS, 100, 10, "building"));
        building.items.add(new ShopItem(Material.SPRUCE_STAIRS, 100, 10, "building"));
        building.items.add(new ShopItem(Material.BIRCH_STAIRS, 100, 10, "building"));
        building.items.add(new ShopItem(Material.JUNGLE_STAIRS, 125, 12, "building"));
        building.items.add(new ShopItem(Material.DARK_OAK_STAIRS, 125, 12, "building"));
        building.items.add(new ShopItem(Material.COBBLESTONE_STAIRS, 100, 10, "building"));
        building.items.add(new ShopItem(Material.STONE_BRICK_STAIRS, 150, 15, "building"));
        building.items.add(new ShopItem(Material.BRICK_STAIRS, 200, 20, "building"));
        building.items.add(new ShopItem(Material.QUARTZ_STAIRS, 300, 30, "building"));
        building.items.add(new ShopItem(Material.PURPUR_STAIRS, 400, 40, "building"));

        // Slabs
        building.items.add(new ShopItem(Material.OAK_SLAB, 50, 5, "building"));
        building.items.add(new ShopItem(Material.SPRUCE_SLAB, 50, 5, "building"));
        building.items.add(new ShopItem(Material.BIRCH_SLAB, 50, 5, "building"));
        building.items.add(new ShopItem(Material.COBBLESTONE_SLAB, 50, 5, "building"));
        building.items.add(new ShopItem(Material.STONE_BRICK_SLAB, 100, 10, "building"));
        building.items.add(new ShopItem(Material.QUARTZ_SLAB, 150, 15, "building"));
        building.items.add(new ShopItem(Material.SMOOTH_STONE_SLAB, 100, 10, "building"));

        // Fences & Gates
        building.items.add(new ShopItem(Material.OAK_FENCE, 200, 20, "building"));
        building.items.add(new ShopItem(Material.SPRUCE_FENCE, 200, 20, "building"));
        building.items.add(new ShopItem(Material.BIRCH_FENCE, 200, 20, "building"));
        building.items.add(new ShopItem(Material.OAK_FENCE_GATE, 300, 30, "building"));
        building.items.add(new ShopItem(Material.SPRUCE_FENCE_GATE, 300, 30, "building"));
        building.items.add(new ShopItem(Material.IRON_BARS, 250, 25, "building"));
        building.items.add(new ShopItem(Material.COBBLESTONE_WALL, 150, 15, "building"));

        // Doors & Trapdoors
        building.items.add(new ShopItem(Material.OAK_DOOR, 500, 50, "building"));
        building.items.add(new ShopItem(Material.SPRUCE_DOOR, 500, 50, "building"));
        building.items.add(new ShopItem(Material.BIRCH_DOOR, 500, 50, "building"));
        building.items.add(new ShopItem(Material.IRON_DOOR, 1500, 150, "building"));
        building.items.add(new ShopItem(Material.OAK_TRAPDOOR, 400, 40, "building"));
        building.items.add(new ShopItem(Material.IRON_TRAPDOOR, 1200, 120, "building"));

        // Glass Panes
        building.items.add(new ShopItem(Material.GLASS_PANE, 100, 10, "building"));
        building.items.add(new ShopItem(Material.WHITE_STAINED_GLASS_PANE, 150, 15, "building"));
        building.items.add(new ShopItem(Material.RED_STAINED_GLASS_PANE, 150, 15, "building"));
        building.items.add(new ShopItem(Material.BLUE_STAINED_GLASS_PANE, 150, 15, "building"));
        building.items.add(new ShopItem(Material.BLACK_STAINED_GLASS_PANE, 150, 15, "building"));

        // Other
        building.items.add(new ShopItem(Material.LADDER, 150, 15, "building"));
        building.items.add(new ShopItem(Material.SCAFFOLDING, 300, 30, "building"));
        building.items.add(new ShopItem(Material.LEVER, 200, 20, "building"));
        building.items.add(new ShopItem(Material.STONE_BUTTON, 150, 15, "building"));
        building.items.add(new ShopItem(Material.STONE_PRESSURE_PLATE, 200, 20, "building"));

        categories.put("building", building);

        // ══════════════════════════════════════
        //  CATEGORY 5: DECORATIONS
        // ══════════════════════════════════════
        Category deco = new Category("decorations", "§d§lDecorations", "§d", Material.LANTERN);

        // Lights
        deco.items.add(new ShopItem(Material.LANTERN, 500, 50, "decorations"));
        deco.items.add(new ShopItem(Material.SOUL_LANTERN, 750, 75, "decorations"));
        deco.items.add(new ShopItem(Material.TORCH, 50, 5, "decorations"));
        deco.items.add(new ShopItem(Material.SOUL_TORCH, 100, 10, "decorations"));
        deco.items.add(new ShopItem(Material.GLOWSTONE, 500, 50, "decorations"));
        deco.items.add(new ShopItem(Material.SHROOMLIGHT, 750, 75, "decorations"));
        deco.items.add(new ShopItem(Material.JACK_O_LANTERN, 400, 40, "decorations"));
        deco.items.add(new ShopItem(Material.CAMPFIRE, 800, 80, "decorations"));
        deco.items.add(new ShopItem(Material.SOUL_CAMPFIRE, 1000, 100, "decorations"));
        deco.items.add(new ShopItem(Material.END_ROD, 1000, 100, "decorations"));
        deco.items.add(new ShopItem(Material.REDSTONE_LAMP, 1000, 100, "decorations"));
        deco.items.add(new ShopItem(Material.CANDLE, 300, 30, "decorations"));
        deco.items.add(new ShopItem(Material.FROGLIGHT, 2000, 200, "decorations"));

        // Decorative Items
        deco.items.add(new ShopItem(Material.PAINTING, 500, 50, "decorations"));
        deco.items.add(new ShopItem(Material.ITEM_FRAME, 400, 40, "decorations"));
        deco.items.add(new ShopItem(Material.GLOW_ITEM_FRAME, 1000, 100, "decorations"));
        deco.items.add(new ShopItem(Material.FLOWER_POT, 300, 30, "decorations"));
        deco.items.add(new ShopItem(Material.ARMOR_STAND, 1500, 150, "decorations"));
        deco.items.add(new ShopItem(Material.BELL, 3000, 300, "decorations"));
        deco.items.add(new ShopItem(Material.CHAIN, 500, 50, "decorations"));
        deco.items.add(new ShopItem(Material.LIGHTNING_ROD, 2000, 200, "decorations"));

        // Beds
        deco.items.add(new ShopItem(Material.RED_BED, 800, 80, "decorations"));
        deco.items.add(new ShopItem(Material.BLUE_BED, 800, 80, "decorations"));
        deco.items.add(new ShopItem(Material.WHITE_BED, 800, 80, "decorations"));
        deco.items.add(new ShopItem(Material.BLACK_BED, 800, 80, "decorations"));
        deco.items.add(new ShopItem(Material.GREEN_BED, 800, 80, "decorations"));
        deco.items.add(new ShopItem(Material.YELLOW_BED, 800, 80, "decorations"));
        deco.items.add(new ShopItem(Material.PINK_BED, 800, 80, "decorations"));

        // Signs & Banners
        deco.items.add(new ShopItem(Material.OAK_SIGN, 300, 30, "decorations"));
        deco.items.add(new ShopItem(Material.SPRUCE_SIGN, 300, 30, "decorations"));
        deco.items.add(new ShopItem(Material.OAK_HANGING_SIGN, 500, 50, "decorations"));
        deco.items.add(new ShopItem(Material.WHITE_BANNER, 600, 60, "decorations"));
        deco.items.add(new ShopItem(Material.RED_BANNER, 600, 60, "decorations"));
        deco.items.add(new ShopItem(Material.BLUE_BANNER, 600, 60, "decorations"));
        deco.items.add(new ShopItem(Material.BLACK_BANNER, 600, 60, "decorations"));

        // Carpets
        deco.items.add(new ShopItem(Material.WHITE_CARPET, 100, 10, "decorations"));
        deco.items.add(new ShopItem(Material.RED_CARPET, 100, 10, "decorations"));
        deco.items.add(new ShopItem(Material.BLUE_CARPET, 100, 10, "decorations"));
        deco.items.add(new ShopItem(Material.GREEN_CARPET, 100, 10, "decorations"));
        deco.items.add(new ShopItem(Material.BLACK_CARPET, 100, 10, "decorations"));

        // Other
        deco.items.add(new ShopItem(Material.BOOKSHELF, 1000, 100, "decorations"));
        deco.items.add(new ShopItem(Material.CHISELED_BOOKSHELF, 1500, 150, "decorations"));
        deco.items.add(new ShopItem(Material.DECORATED_POT, 2000, 200, "decorations"));
        deco.items.add(new ShopItem(Material.SKULL_BANNER_PATTERN, 5000, 500, "decorations"));
        deco.items.add(new ShopItem(Material.MUSIC_DISC_CAT, 10000, 1000, "decorations"));
        deco.items.add(new ShopItem(Material.MUSIC_DISC_13, 10000, 1000, "decorations"));

        // Flowers
        deco.items.add(new ShopItem(Material.POPPY, 100, 10, "decorations"));
        deco.items.add(new ShopItem(Material.DANDELION, 100, 10, "decorations"));
        deco.items.add(new ShopItem(Material.BLUE_ORCHID, 200, 20, "decorations"));
        deco.items.add(new ShopItem(Material.ALLIUM, 200, 20, "decorations"));
        deco.items.add(new ShopItem(Material.AZURE_BLUET, 200, 20, "decorations"));
        deco.items.add(new ShopItem(Material.SUNFLOWER, 300, 30, "decorations"));
        deco.items.add(new ShopItem(Material.LILAC, 300, 30, "decorations"));
        deco.items.add(new ShopItem(Material.ROSE_BUSH, 300, 30, "decorations"));
        deco.items.add(new ShopItem(Material.WITHER_ROSE, 5000, 500, "decorations"));
        deco.items.add(new ShopItem(Material.TORCHFLOWER, 3000, 300, "decorations"));

        categories.put("decorations", deco);

        // ══════════════════════════════════════
        //  CATEGORY 6: REDSTONE & WORKER
        // ══════════════════════════════════════
        Category worker = new Category("worker", "§e§lRedstone & Worker", "§e", Material.REDSTONE);

        // Workstations
        worker.items.add(new ShopItem(Material.CRAFTING_TABLE, 500, 50, "worker"));
        worker.items.add(new ShopItem(Material.FURNACE, 750, 75, "worker"));
        worker.items.add(new ShopItem(Material.BLAST_FURNACE, 3000, 300, "worker"));
        worker.items.add(new ShopItem(Material.SMOKER, 2500, 250, "worker"));
        worker.items.add(new ShopItem(Material.ANVIL, 8000, 800, "worker"));
        worker.items.add(new ShopItem(Material.ENCHANTING_TABLE, 15000, 1500, "worker"));
        worker.items.add(new ShopItem(Material.BREWING_STAND, 5000, 500, "worker"));
        worker.items.add(new ShopItem(Material.GRINDSTONE, 3000, 300, "worker"));
        worker.items.add(new ShopItem(Material.SMITHING_TABLE, 3000, 300, "worker"));
        worker.items.add(new ShopItem(Material.CARTOGRAPHY_TABLE, 2000, 200, "worker"));
        worker.items.add(new ShopItem(Material.FLETCHING_TABLE, 1500, 150, "worker"));
        worker.items.add(new ShopItem(Material.LOOM, 1500, 150, "worker"));
        worker.items.add(new ShopItem(Material.STONECUTTER, 2500, 250, "worker"));
        worker.items.add(new ShopItem(Material.COMPOSTER, 1000, 100, "worker"));
        worker.items.add(new ShopItem(Material.CAULDRON, 3000, 300, "worker"));
        worker.items.add(new ShopItem(Material.LECTERN, 2000, 200, "worker"));
        worker.items.add(new ShopItem(Material.BARREL, 750, 75, "worker"));

        // Storage
        worker.items.add(new ShopItem(Material.CHEST, 500, 50, "worker"));
        worker.items.add(new ShopItem(Material.TRAPPED_CHEST, 750, 75, "worker"));
        worker.items.add(new ShopItem(Material.ENDER_CHEST, 10000, 1000, "worker"));
        worker.items.add(new ShopItem(Material.SHULKER_BOX, 25000, 2500, "worker"));

        // Redstone
        worker.items.add(new ShopItem(Material.REDSTONE, 300, 30, "worker"));
        worker.items.add(new ShopItem(Material.REDSTONE_TORCH, 200, 20, "worker"));
        worker.items.add(new ShopItem(Material.REPEATER, 500, 50, "worker"));
        worker.items.add(new ShopItem(Material.COMPARATOR, 750, 75, "worker"));
        worker.items.add(new ShopItem(Material.PISTON, 2500, 250, "worker"));
        worker.items.add(new ShopItem(Material.STICKY_PISTON, 4000, 400, "worker"));
        worker.items.add(new ShopItem(Material.OBSERVER, 1500, 150, "worker"));
        worker.items.add(new ShopItem(Material.HOPPER, 5000, 500, "worker"));
        worker.items.add(new ShopItem(Material.DROPPER, 1500, 150, "worker"));
        worker.items.add(new ShopItem(Material.DISPENSER, 2500, 250, "worker"));
        worker.items.add(new ShopItem(Material.TNT, 5000, 500, "worker"));
        worker.items.add(new ShopItem(Material.TARGET, 3000, 300, "worker"));
        worker.items.add(new ShopItem(Material.DAYLIGHT_DETECTOR, 2000, 200, "worker"));
        worker.items.add(new ShopItem(Material.TRIPWIRE_HOOK, 500, 50, "worker"));
        worker.items.add(new ShopItem(Material.SCULK_SENSOR, 5000, 500, "worker"));

        // Buckets
        worker.items.add(new ShopItem(Material.BUCKET, 1000, 100, "worker"));
        worker.items.add(new ShopItem(Material.WATER_BUCKET, 1500, 150, "worker"));
        worker.items.add(new ShopItem(Material.LAVA_BUCKET, 3000, 300, "worker"));
        worker.items.add(new ShopItem(Material.MILK_BUCKET, 1000, 100, "worker"));
        worker.items.add(new ShopItem(Material.POWDER_SNOW_BUCKET, 2000, 200, "worker"));

        // Transport
        worker.items.add(new ShopItem(Material.MINECART, 3000, 300, "worker"));
        worker.items.add(new ShopItem(Material.CHEST_MINECART, 4000, 400, "worker"));
        worker.items.add(new ShopItem(Material.HOPPER_MINECART, 6000, 600, "worker"));
        worker.items.add(new ShopItem(Material.TNT_MINECART, 7000, 700, "worker"));
        worker.items.add(new ShopItem(Material.RAIL, 500, 50, "worker"));
        worker.items.add(new ShopItem(Material.POWERED_RAIL, 1500, 150, "worker"));
        worker.items.add(new ShopItem(Material.DETECTOR_RAIL, 1000, 100, "worker"));
        worker.items.add(new ShopItem(Material.ACTIVATOR_RAIL, 1000, 100, "worker"));
        worker.items.add(new ShopItem(Material.OAK_BOAT, 1000, 100, "worker"));

        categories.put("worker", worker);

        // ══════════════════════════════════════
        //  CATEGORY 7: FOOD & FARMING
        // ══════════════════════════════════════
        Category food = new Category("food", "§c§lFood & Farming", "§c", Material.GOLDEN_APPLE);

        // Cooked Food
        food.items.add(new ShopItem(Material.BREAD, 200, 20, "food"));
        food.items.add(new ShopItem(Material.COOKED_BEEF, 300, 30, "food"));
        food.items.add(new ShopItem(Material.COOKED_CHICKEN, 250, 25, "food"));
        food.items.add(new ShopItem(Material.COOKED_PORKCHOP, 300, 30, "food"));
        food.items.add(new ShopItem(Material.COOKED_MUTTON, 250, 25, "food"));
        food.items.add(new ShopItem(Material.COOKED_SALMON, 250, 25, "food"));
        food.items.add(new ShopItem(Material.COOKED_COD, 200, 20, "food"));
        food.items.add(new ShopItem(Material.COOKED_RABBIT, 300, 30, "food"));
        food.items.add(new ShopItem(Material.BAKED_POTATO, 200, 20, "food"));
        food.items.add(new ShopItem(Material.PUMPKIN_PIE, 400, 40, "food"));
        food.items.add(new ShopItem(Material.CAKE, 1000, 100, "food"));
        food.items.add(new ShopItem(Material.COOKIE, 150, 15, "food"));
        food.items.add(new ShopItem(Material.MUSHROOM_STEW, 500, 50, "food"));
        food.items.add(new ShopItem(Material.RABBIT_STEW, 800, 80, "food"));
        food.items.add(new ShopItem(Material.BEETROOT_SOUP, 400, 40, "food"));
        food.items.add(new ShopItem(Material.SUSPICIOUS_STEW, 1000, 100, "food"));

        // Special
        food.items.add(new ShopItem(Material.GOLDEN_APPLE, 5000, 500, "food"));
        food.items.add(new ShopItem(Material.ENCHANTED_GOLDEN_APPLE, 100000, 10000, "food"));
        food.items.add(new ShopItem(Material.GOLDEN_CARROT, 3000, 300, "food"));
        food.items.add(new ShopItem(Material.GLISTERING_MELON_SLICE, 2000, 200, "food"));
        food.items.add(new ShopItem(Material.HONEY_BOTTLE, 1000, 100, "food"));
        food.items.add(new ShopItem(Material.CHORUS_FRUIT, 1500, 150, "food"));

        // Raw Food
        food.items.add(new ShopItem(Material.APPLE, 200, 20, "food"));
        food.items.add(new ShopItem(Material.CARROT, 100, 10, "food"));
        food.items.add(new ShopItem(Material.POTATO, 100, 10, "food"));
        food.items.add(new ShopItem(Material.MELON_SLICE, 100, 10, "food"));
        food.items.add(new ShopItem(Material.SWEET_BERRIES, 150, 15, "food"));
        food.items.add(new ShopItem(Material.GLOW_BERRIES, 300, 30, "food"));
        food.items.add(new ShopItem(Material.DRIED_KELP, 50, 5, "food"));

        // Seeds & Farming
        food.items.add(new ShopItem(Material.WHEAT_SEEDS, 50, 5, "food"));
        food.items.add(new ShopItem(Material.BEETROOT_SEEDS, 75, 7, "food"));
        food.items.add(new ShopItem(Material.MELON_SEEDS, 100, 10, "food"));
        food.items.add(new ShopItem(Material.PUMPKIN_SEEDS, 100, 10, "food"));
        food.items.add(new ShopItem(Material.TORCHFLOWER_SEEDS, 2000, 200, "food"));
        food.items.add(new ShopItem(Material.WHEAT, 150, 15, "food"));
        food.items.add(new ShopItem(Material.SUGAR_CANE, 200, 20, "food"));
        food.items.add(new ShopItem(Material.BAMBOO, 100, 10, "food"));
        food.items.add(new ShopItem(Material.CACTUS, 150, 15, "food"));
        food.items.add(new ShopItem(Material.KELP, 75, 7, "food"));
        food.items.add(new ShopItem(Material.COCOA_BEANS, 200, 20, "food"));
        food.items.add(new ShopItem(Material.NETHER_WART, 500, 50, "food"));
        food.items.add(new ShopItem(Material.BONE_MEAL, 200, 20, "food"));

        // Saplings
        food.items.add(new ShopItem(Material.OAK_SAPLING, 300, 30, "food"));
        food.items.add(new ShopItem(Material.SPRUCE_SAPLING, 300, 30, "food"));
        food.items.add(new ShopItem(Material.BIRCH_SAPLING, 300, 30, "food"));
        food.items.add(new ShopItem(Material.JUNGLE_SAPLING, 500, 50, "food"));
        food.items.add(new ShopItem(Material.ACACIA_SAPLING, 400, 40, "food"));
        food.items.add(new ShopItem(Material.DARK_OAK_SAPLING, 400, 40, "food"));
        food.items.add(new ShopItem(Material.CHERRY_SAPLING, 750, 75, "food"));
        food.items.add(new ShopItem(Material.MANGROVE_PROPAGULE, 500, 50, "food"));

        // Full blocks
        food.items.add(new ShopItem(Material.PUMPKIN, 300, 30, "food"));
        food.items.add(new ShopItem(Material.MELON, 500, 50, "food"));
        food.items.add(new ShopItem(Material.HAY_BLOCK, 500, 50, "food"));

        // Egg & Spawn
        food.items.add(new ShopItem(Material.EGG, 150, 15, "food"));

        categories.put("food", food);

        // ══════════════════════════════════════
        //  CATEGORY 8: ORES & MATERIALS
        // ══════════════════════════════════════
        Category ores = new Category("ores", "§3§lOres & Materials", "§3", Material.DIAMOND);

        // Raw Ores
        ores.items.add(new ShopItem(Material.COAL, 200, 20, "ores"));
        ores.items.add(new ShopItem(Material.RAW_IRON, 500, 50, "ores"));
        ores.items.add(new ShopItem(Material.RAW_GOLD, 750, 75, "ores"));
        ores.items.add(new ShopItem(Material.RAW_COPPER, 300, 30, "ores"));
        ores.items.add(new ShopItem(Material.DIAMOND, 5000, 500, "ores"));
        ores.items.add(new ShopItem(Material.EMERALD, 4000, 400, "ores"));
        ores.items.add(new ShopItem(Material.LAPIS_LAZULI, 800, 80, "ores"));
        ores.items.add(new ShopItem(Material.AMETHYST_SHARD, 500, 50, "ores"));
        ores.items.add(new ShopItem(Material.QUARTZ, 400, 40, "ores"));
        ores.items.add(new ShopItem(Material.NETHERITE_SCRAP, 25000, 2500, "ores"));
        ores.items.add(new ShopItem(Material.NETHERITE_INGOT, 50000, 5000, "ores"));
        ores.items.add(new ShopItem(Material.ANCIENT_DEBRIS, 30000, 3000, "ores"));

        // Ingots
        ores.items.add(new ShopItem(Material.IRON_INGOT, 1000, 100, "ores"));
        ores.items.add(new ShopItem(Material.GOLD_INGOT, 1500, 150, "ores"));
        ores.items.add(new ShopItem(Material.COPPER_INGOT, 500, 50, "ores"));
        ores.items.add(new ShopItem(Material.IRON_NUGGET, 100, 10, "ores"));
        ores.items.add(new ShopItem(Material.GOLD_NUGGET, 150, 15, "ores"));

        // Crafting Materials
        ores.items.add(new ShopItem(Material.STICK, 25, 2, "ores"));
        ores.items.add(new ShopItem(Material.FLINT, 100, 10, "ores"));
        ores.items.add(new ShopItem(Material.FEATHER, 100, 10, "ores"));
        ores.items.add(new ShopItem(Material.LEATHER, 300, 30, "ores"));
        ores.items.add(new ShopItem(Material.STRING, 200, 20, "ores"));
        ores.items.add(new ShopItem(Material.SLIME_BALL, 500, 50, "ores"));
        ores.items.add(new ShopItem(Material.HONEY_BLOCK, 1000, 100, "ores"));
        ores.items.add(new ShopItem(Material.HONEYCOMB, 500, 50, "ores"));
        ores.items.add(new ShopItem(Material.INK_SAC, 200, 20, "ores"));
        ores.items.add(new ShopItem(Material.GLOW_INK_SAC, 500, 50, "ores"));
        ores.items.add(new ShopItem(Material.BONE, 150, 15, "ores"));
        ores.items.add(new ShopItem(Material.GUNPOWDER, 300, 30, "ores"));
        ores.items.add(new ShopItem(Material.BLAZE_ROD, 1000, 100, "ores"));
        ores.items.add(new ShopItem(Material.BLAZE_POWDER, 750, 75, "ores"));
        ores.items.add(new ShopItem(Material.ENDER_PEARL, 2000, 200, "ores"));
        ores.items.add(new ShopItem(Material.ENDER_EYE, 5000, 500, "ores"));
        ores.items.add(new ShopItem(Material.GHAST_TEAR, 3000, 300, "ores"));
        ores.items.add(new ShopItem(Material.MAGMA_CREAM, 1000, 100, "ores"));
        ores.items.add(new ShopItem(Material.PRISMARINE_SHARD, 400, 40, "ores"));
        ores.items.add(new ShopItem(Material.PRISMARINE_CRYSTALS, 500, 50, "ores"));
        ores.items.add(new ShopItem(Material.PHANTOM_MEMBRANE, 2000, 200, "ores"));
        ores.items.add(new ShopItem(Material.NAUTILUS_SHELL, 5000, 500, "ores"));
        ores.items.add(new ShopItem(Material.HEART_OF_THE_SEA, 25000, 2500, "ores"));
        ores.items.add(new ShopItem(Material.NETHER_STAR, 100000, 10000, "ores"));
        ores.items.add(new ShopItem(Material.DRAGON_BREATH, 10000, 1000, "ores"));
        ores.items.add(new ShopItem(Material.SHULKER_SHELL, 15000, 1500, "ores"));
        ores.items.add(new ShopItem(Material.TOTEM_OF_UNDYING, 50000, 5000, "ores"));
        ores.items.add(new ShopItem(Material.EXPERIENCE_BOTTLE, 1500, 150, "ores"));
        ores.items.add(new ShopItem(Material.ENCHANTED_BOOK, 5000, 500, "ores"));

        // Dyes
        ores.items.add(new ShopItem(Material.WHITE_DYE, 150, 15, "ores"));
        ores.items.add(new ShopItem(Material.RED_DYE, 150, 15, "ores"));
        ores.items.add(new ShopItem(Material.BLUE_DYE, 150, 15, "ores"));
        ores.items.add(new ShopItem(Material.GREEN_DYE, 150, 15, "ores"));
        ores.items.add(new ShopItem(Material.YELLOW_DYE, 150, 15, "ores"));
        ores.items.add(new ShopItem(Material.BLACK_DYE, 150, 15, "ores"));
        ores.items.add(new ShopItem(Material.ORANGE_DYE, 150, 15, "ores"));
        ores.items.add(new ShopItem(Material.PURPLE_DYE, 150, 15, "ores"));
        ores.items.add(new ShopItem(Material.PINK_DYE, 150, 15, "ores"));
        ores.items.add(new ShopItem(Material.CYAN_DYE, 150, 15, "ores"));
        ores.items.add(new ShopItem(Material.LIGHT_BLUE_DYE, 150, 15, "ores"));
        ores.items.add(new ShopItem(Material.LIME_DYE, 150, 15, "ores"));
        ores.items.add(new ShopItem(Material.MAGENTA_DYE, 150, 15, "ores"));
        ores.items.add(new ShopItem(Material.BROWN_DYE, 150, 15, "ores"));
        ores.items.add(new ShopItem(Material.GRAY_DYE, 150, 15, "ores"));
        ores.items.add(new ShopItem(Material.LIGHT_GRAY_DYE, 150, 15, "ores"));

        // Mob Drops
        ores.items.add(new ShopItem(Material.ROTTEN_FLESH, 50, 5, "ores"));
        ores.items.add(new ShopItem(Material.SPIDER_EYE, 200, 20, "ores"));
        ores.items.add(new ShopItem(Material.FERMENTED_SPIDER_EYE, 500, 50, "ores"));
        ores.items.add(new ShopItem(Material.RABBIT_FOOT, 1000, 100, "ores"));
        ores.items.add(new ShopItem(Material.RABBIT_HIDE, 200, 20, "ores"));
        ores.items.add(new ShopItem(Material.TURTLE_EGG, 3000, 300, "ores"));
        ores.items.add(new ShopItem(Material.SCUTE, 2000, 200, "ores"));
        ores.items.add(new ShopItem(Material.WITHER_SKELETON_SKULL, 20000, 2000, "ores"));

        categories.put("ores", ores);

        // ══════════════════════════════════════
        //  BUILD SELL PRICES MAP
        // ══════════════════════════════════════
        for (Category cat : categories.values()) {
            for (ShopItem item : cat.items) {
                sellPrices.put(item.material, item.sellPrice);
                totalItems++;
            }
        }
    }

    // ══════════════════════════════════════
    //  GETTERS
    // ══════════════════════════════════════

    public Map<String, Category> getCategories() {
        return categories;
    }

    public Category getCategory(String id) {
        return categories.get(id);
    }

    public int getSellPrice(Material material) {
        return sellPrices.getOrDefault(material, 0);
    }

    public boolean isSellable(Material material) {
        return sellPrices.containsKey(material);
    }

    public int getTotalItems() {
        return totalItems;
    }

    public String formatItemName(Material material) {
        String name = material.name().replace("_", " ");
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
