package com.kz.plugin.data;

import org.bukkit.Material;
import java.util.*;
import org.bukkit.entity.EntityType;

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

     // Tambah SETELAH class Category
    public static class SpawnerShopItem extends ShopItem {
    public final EntityType entityType;

    public SpawnerShopItem(EntityType entityType, int buyPrice) {
        super(Material.SPAWNER, buyPrice, 0, "spawners");
        this.entityType = entityType;
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
        //  CATEGORY 1: COMBAT
        // ══════════════════════════════════════
        Category combat = new Category("combat", "§c§lCombat", "§c", Material.DIAMOND_SWORD);

        // Wooden Tier
        combat.items.add(new ShopItem(Material.WOODEN_SWORD, 600, 60, "combat"));
        combat.items.add(new ShopItem(Material.WOODEN_PICKAXE, 500, 50, "combat"));
        combat.items.add(new ShopItem(Material.WOODEN_AXE, 500, 50, "combat"));
        combat.items.add(new ShopItem(Material.WOODEN_SHOVEL, 400, 40, "combat"));
        combat.items.add(new ShopItem(Material.WOODEN_HOE, 400, 40, "combat"));

        // Stone Tier
        combat.items.add(new ShopItem(Material.STONE_SWORD, 1800, 180, "combat"));
        combat.items.add(new ShopItem(Material.STONE_PICKAXE, 1500, 150, "combat"));
        combat.items.add(new ShopItem(Material.STONE_AXE, 1500, 150, "combat"));
        combat.items.add(new ShopItem(Material.STONE_SHOVEL, 1200, 120, "combat"));
        combat.items.add(new ShopItem(Material.STONE_HOE, 1200, 120, "combat"));

        // Iron Tier
        combat.items.add(new ShopItem(Material.IRON_SWORD, 6000, 600, "combat"));
        combat.items.add(new ShopItem(Material.IRON_PICKAXE, 5000, 500, "combat"));
        combat.items.add(new ShopItem(Material.IRON_AXE, 5000, 500, "combat"));
        combat.items.add(new ShopItem(Material.IRON_SHOVEL, 4000, 400, "combat"));
        combat.items.add(new ShopItem(Material.IRON_HOE, 4000, 400, "combat"));

        // Gold Tier
        combat.items.add(new ShopItem(Material.GOLDEN_SWORD, 10000, 1000, "combat"));
        combat.items.add(new ShopItem(Material.GOLDEN_PICKAXE, 8000, 800, "combat"));
        combat.items.add(new ShopItem(Material.GOLDEN_AXE, 8000, 800, "combat"));
        combat.items.add(new ShopItem(Material.GOLDEN_SHOVEL, 7000, 700, "combat"));
        combat.items.add(new ShopItem(Material.GOLDEN_HOE, 7000, 700, "combat"));

        // Diamond Tier
        combat.items.add(new ShopItem(Material.DIAMOND_SWORD, 30000, 3000, "combat"));
        combat.items.add(new ShopItem(Material.DIAMOND_PICKAXE, 25000, 2500, "combat"));
        combat.items.add(new ShopItem(Material.DIAMOND_AXE, 25000, 2500, "combat"));
        combat.items.add(new ShopItem(Material.DIAMOND_SHOVEL, 20000, 2000, "combat"));
        combat.items.add(new ShopItem(Material.DIAMOND_HOE, 20000, 2000, "combat"));

        // Netherite Tier
        combat.items.add(new ShopItem(Material.NETHERITE_SWORD, 120000, 12000, "combat"));
        combat.items.add(new ShopItem(Material.NETHERITE_PICKAXE, 100000, 10000, "combat"));
        combat.items.add(new ShopItem(Material.NETHERITE_AXE, 100000, 10000, "combat"));
        combat.items.add(new ShopItem(Material.NETHERITE_SHOVEL, 80000, 8000, "combat"));
        combat.items.add(new ShopItem(Material.NETHERITE_HOE, 80000, 8000, "combat"));

        // Ranged
        combat.items.add(new ShopItem(Material.BOW, 5000, 500, "combat"));
        combat.items.add(new ShopItem(Material.CROSSBOW, 15000, 1500, "combat"));
        combat.items.add(new ShopItem(Material.TRIDENT, 50000, 5000, "combat"));
        combat.items.add(new ShopItem(Material.SHIELD, 8000, 800, "combat"));

        // Leather Armor
        combat.items.add(new ShopItem(Material.LEATHER_HELMET, 800, 80, "combat"));
        combat.items.add(new ShopItem(Material.LEATHER_CHESTPLATE, 1200, 120, "combat"));
        combat.items.add(new ShopItem(Material.LEATHER_LEGGINGS, 1000, 100, "combat"));
        combat.items.add(new ShopItem(Material.LEATHER_BOOTS, 600, 60, "combat"));

        // Chainmail Armor
        combat.items.add(new ShopItem(Material.CHAINMAIL_HELMET, 3000, 300, "combat"));
        combat.items.add(new ShopItem(Material.CHAINMAIL_CHESTPLATE, 5000, 500, "combat"));
        combat.items.add(new ShopItem(Material.CHAINMAIL_LEGGINGS, 4000, 400, "combat"));
        combat.items.add(new ShopItem(Material.CHAINMAIL_BOOTS, 2500, 250, "combat"));

        // Iron Armor
        combat.items.add(new ShopItem(Material.IRON_HELMET, 5000, 500, "combat"));
        combat.items.add(new ShopItem(Material.IRON_CHESTPLATE, 8000, 800, "combat"));
        combat.items.add(new ShopItem(Material.IRON_LEGGINGS, 7000, 700, "combat"));
        combat.items.add(new ShopItem(Material.IRON_BOOTS, 4000, 400, "combat"));

        // Gold Armor
        combat.items.add(new ShopItem(Material.GOLDEN_HELMET, 8000, 800, "combat"));
        combat.items.add(new ShopItem(Material.GOLDEN_CHESTPLATE, 12000, 1200, "combat"));
        combat.items.add(new ShopItem(Material.GOLDEN_LEGGINGS, 10000, 1000, "combat"));
        combat.items.add(new ShopItem(Material.GOLDEN_BOOTS, 6000, 600, "combat"));

        // Diamond Armor
        combat.items.add(new ShopItem(Material.DIAMOND_HELMET, 20000, 2000, "combat"));
        combat.items.add(new ShopItem(Material.DIAMOND_CHESTPLATE, 35000, 3500, "combat"));
        combat.items.add(new ShopItem(Material.DIAMOND_LEGGINGS, 30000, 3000, "combat"));
        combat.items.add(new ShopItem(Material.DIAMOND_BOOTS, 18000, 1800, "combat"));

        // Netherite Armor
        combat.items.add(new ShopItem(Material.NETHERITE_HELMET, 80000, 8000, "combat"));
        combat.items.add(new ShopItem(Material.NETHERITE_CHESTPLATE, 120000, 12000, "combat"));
        combat.items.add(new ShopItem(Material.NETHERITE_LEGGINGS, 100000, 10000, "combat"));
        combat.items.add(new ShopItem(Material.NETHERITE_BOOTS, 70000, 7000, "combat"));

        // Special Armor
        combat.items.add(new ShopItem(Material.TURTLE_HELMET, 30000, 3000, "combat"));

        categories.put("combat", combat);

        // ══════════════════════════════════════
        //  CATEGORY 2: FOOD & FARMING
        // ══════════════════════════════════════
        Category food = new Category("food", "§6§lFood & Farming", "§6", Material.GOLDEN_CARROT);

        // Cooked Food
        food.items.add(new ShopItem(Material.BREAD, 200, 20, "food"));
        food.items.add(new ShopItem(Material.COOKED_BEEF, 300, 30, "food"));
        food.items.add(new ShopItem(Material.COOKED_PORKCHOP, 300, 30, "food"));
        food.items.add(new ShopItem(Material.COOKED_CHICKEN, 250, 25, "food"));
        food.items.add(new ShopItem(Material.COOKED_MUTTON, 250, 25, "food"));
        food.items.add(new ShopItem(Material.COOKED_RABBIT, 300, 30, "food"));
        food.items.add(new ShopItem(Material.COOKED_COD, 200, 20, "food"));
        food.items.add(new ShopItem(Material.COOKED_SALMON, 250, 25, "food"));
        food.items.add(new ShopItem(Material.BAKED_POTATO, 200, 20, "food"));
        food.items.add(new ShopItem(Material.PUMPKIN_PIE, 400, 40, "food"));
        food.items.add(new ShopItem(Material.CAKE, 1000, 100, "food"));
        food.items.add(new ShopItem(Material.COOKIE, 150, 15, "food"));

        // Stew
        food.items.add(new ShopItem(Material.MUSHROOM_STEW, 500, 50, "food"));
        food.items.add(new ShopItem(Material.RABBIT_STEW, 800, 80, "food"));
        food.items.add(new ShopItem(Material.BEETROOT_SOUP, 400, 40, "food"));
        food.items.add(new ShopItem(Material.SUSPICIOUS_STEW, 1000, 100, "food"));

        // Special Food
        food.items.add(new ShopItem(Material.APPLE, 200, 20, "food"));
        food.items.add(new ShopItem(Material.GOLDEN_CARROT, 3000, 300, "food"));
        food.items.add(new ShopItem(Material.GLISTERING_MELON_SLICE, 2000, 200, "food"));
        food.items.add(new ShopItem(Material.HONEY_BOTTLE, 1000, 100, "food"));
        food.items.add(new ShopItem(Material.CHORUS_FRUIT, 1500, 150, "food"));
        food.items.add(new ShopItem(Material.DRIED_KELP, 50, 5, "food"));

        // Raw Food
        food.items.add(new ShopItem(Material.BEEF, 200, 20, "food"));
        food.items.add(new ShopItem(Material.PORKCHOP, 200, 20, "food"));
        food.items.add(new ShopItem(Material.CHICKEN, 150, 15, "food"));
        food.items.add(new ShopItem(Material.MUTTON, 150, 15, "food"));
        food.items.add(new ShopItem(Material.RABBIT, 200, 20, "food"));
        food.items.add(new ShopItem(Material.COD, 100, 10, "food"));
        food.items.add(new ShopItem(Material.SALMON, 150, 15, "food"));
        food.items.add(new ShopItem(Material.TROPICAL_FISH, 300, 30, "food"));
        food.items.add(new ShopItem(Material.PUFFERFISH, 500, 50, "food"));
        food.items.add(new ShopItem(Material.CARROT, 100, 10, "food"));
        food.items.add(new ShopItem(Material.POTATO, 100, 10, "food"));
        food.items.add(new ShopItem(Material.POISONOUS_POTATO, 50, 5, "food"));
        food.items.add(new ShopItem(Material.BEETROOT, 100, 10, "food"));
        food.items.add(new ShopItem(Material.MELON_SLICE, 100, 10, "food"));
        food.items.add(new ShopItem(Material.SWEET_BERRIES, 150, 15, "food"));
        food.items.add(new ShopItem(Material.GLOW_BERRIES, 300, 30, "food"));

        // Seeds & Farming
        food.items.add(new ShopItem(Material.WHEAT_SEEDS, 50, 5, "food"));
        food.items.add(new ShopItem(Material.BEETROOT_SEEDS, 75, 7, "food"));
        food.items.add(new ShopItem(Material.MELON_SEEDS, 100, 10, "food"));
        food.items.add(new ShopItem(Material.PUMPKIN_SEEDS, 100, 10, "food"));
        food.items.add(new ShopItem(Material.TORCHFLOWER_SEEDS, 2000, 200, "food"));
        food.items.add(new ShopItem(Material.PITCHER_POD, 2000, 200, "food"));
        food.items.add(new ShopItem(Material.COCOA_BEANS, 200, 20, "food"));
        food.items.add(new ShopItem(Material.WHEAT, 150, 15, "food"));
        food.items.add(new ShopItem(Material.SUGAR, 100, 10, "food"));
        food.items.add(new ShopItem(Material.EGG, 150, 15, "food"));
        food.items.add(new ShopItem(Material.MILK_BUCKET, 1000, 100, "food"));

        // Brewing
        food.items.add(new ShopItem(Material.NETHER_WART, 500, 50, "food"));
        food.items.add(new ShopItem(Material.GLASS_BOTTLE, 100, 10, "food"));
        food.items.add(new ShopItem(Material.POTION, 500, 50, "food"));
        food.items.add(new ShopItem(Material.SPLASH_POTION, 1000, 100, "food"));
        food.items.add(new ShopItem(Material.LINGERING_POTION, 2000, 200, "food"));
        food.items.add(new ShopItem(Material.BREWING_STAND, 5000, 500, "food"));
        food.items.add(new ShopItem(Material.CAULDRON, 3000, 300, "food"));
        food.items.add(new ShopItem(Material.BLAZE_POWDER, 750, 75, "food"));
        food.items.add(new ShopItem(Material.FERMENTED_SPIDER_EYE, 500, 50, "food"));
        food.items.add(new ShopItem(Material.SPIDER_EYE, 200, 20, "food"));
        food.items.add(new ShopItem(Material.GHAST_TEAR, 3000, 300, "food"));
        food.items.add(new ShopItem(Material.MAGMA_CREAM, 1000, 100, "food"));
        food.items.add(new ShopItem(Material.BLAZE_ROD, 1000, 100, "food"));
        food.items.add(new ShopItem(Material.PHANTOM_MEMBRANE, 2000, 200, "food"));
        food.items.add(new ShopItem(Material.REDSTONE, 300, 30, "food"));
        food.items.add(new ShopItem(Material.GLOWSTONE_DUST, 400, 40, "food"));
        food.items.add(new ShopItem(Material.GUNPOWDER, 300, 30, "food"));
        food.items.add(new ShopItem(Material.DRAGON_BREATH, 10000, 1000, "food"));

        categories.put("food", food);

        // ══════════════════════════════════════
        //  CATEGORY 3: RESOURCES
        // ══════════════════════════════════════
        Category resources = new Category("resources", "§a§lResources", "§a", Material.DIAMOND);

        // Ores
        resources.items.add(new ShopItem(Material.COAL_ORE, 300, 30, "resources"));
        resources.items.add(new ShopItem(Material.DEEPSLATE_COAL_ORE, 350, 35, "resources"));
        resources.items.add(new ShopItem(Material.IRON_ORE, 800, 80, "resources"));
        resources.items.add(new ShopItem(Material.DEEPSLATE_IRON_ORE, 900, 90, "resources"));
        resources.items.add(new ShopItem(Material.COPPER_ORE, 400, 40, "resources"));
        resources.items.add(new ShopItem(Material.DEEPSLATE_COPPER_ORE, 450, 45, "resources"));
        resources.items.add(new ShopItem(Material.GOLD_ORE, 1200, 120, "resources"));
        resources.items.add(new ShopItem(Material.DEEPSLATE_GOLD_ORE, 1300, 130, "resources"));
        resources.items.add(new ShopItem(Material.NETHER_GOLD_ORE, 1000, 100, "resources"));
        resources.items.add(new ShopItem(Material.REDSTONE_ORE, 500, 50, "resources"));
        resources.items.add(new ShopItem(Material.DEEPSLATE_REDSTONE_ORE, 550, 55, "resources"));
        resources.items.add(new ShopItem(Material.EMERALD_ORE, 3000, 300, "resources"));
        resources.items.add(new ShopItem(Material.DEEPSLATE_EMERALD_ORE, 3200, 320, "resources"));
        resources.items.add(new ShopItem(Material.LAPIS_ORE, 600, 60, "resources"));
        resources.items.add(new ShopItem(Material.DEEPSLATE_LAPIS_ORE, 700, 70, "resources"));
        resources.items.add(new ShopItem(Material.DIAMOND_ORE, 5000, 500, "resources"));
        resources.items.add(new ShopItem(Material.DEEPSLATE_DIAMOND_ORE, 5500, 550, "resources"));
        resources.items.add(new ShopItem(Material.NETHER_QUARTZ_ORE, 400, 40, "resources"));
        resources.items.add(new ShopItem(Material.ANCIENT_DEBRIS, 30000, 3000, "resources"));

        // Raw Materials
        resources.items.add(new ShopItem(Material.RAW_IRON, 500, 50, "resources"));
        resources.items.add(new ShopItem(Material.RAW_COPPER, 300, 30, "resources"));
        resources.items.add(new ShopItem(Material.RAW_GOLD, 750, 75, "resources"));

        // Processed Materials
        resources.items.add(new ShopItem(Material.COAL, 200, 20, "resources"));
        resources.items.add(new ShopItem(Material.CHARCOAL, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.IRON_INGOT, 1000, 100, "resources"));
        resources.items.add(new ShopItem(Material.COPPER_INGOT, 500, 50, "resources"));
        resources.items.add(new ShopItem(Material.GOLD_INGOT, 1500, 150, "resources"));
        resources.items.add(new ShopItem(Material.IRON_NUGGET, 100, 10, "resources"));
        resources.items.add(new ShopItem(Material.GOLD_NUGGET, 150, 15, "resources"));

        // Gems & Minerals
        resources.items.add(new ShopItem(Material.DIAMOND, 5000, 500, "resources"));
        resources.items.add(new ShopItem(Material.EMERALD, 4000, 400, "resources"));
        resources.items.add(new ShopItem(Material.LAPIS_LAZULI, 800, 80, "resources"));
        resources.items.add(new ShopItem(Material.REDSTONE, 300, 30, "resources"));
        resources.items.add(new ShopItem(Material.QUARTZ, 400, 40, "resources"));
        resources.items.add(new ShopItem(Material.AMETHYST_SHARD, 500, 50, "resources"));

        // Raw Blocks
        resources.items.add(new ShopItem(Material.RAW_IRON_BLOCK, 4500, 450, "resources"));
        resources.items.add(new ShopItem(Material.RAW_COPPER_BLOCK, 2700, 270, "resources"));
        resources.items.add(new ShopItem(Material.RAW_GOLD_BLOCK, 6750, 675, "resources"));

        // Mineral Blocks
        resources.items.add(new ShopItem(Material.COAL_BLOCK, 1800, 180, "resources"));
        resources.items.add(new ShopItem(Material.IRON_BLOCK, 9000, 900, "resources"));
        resources.items.add(new ShopItem(Material.COPPER_BLOCK, 4500, 450, "resources"));
        resources.items.add(new ShopItem(Material.GOLD_BLOCK, 13500, 1350, "resources"));
        resources.items.add(new ShopItem(Material.REDSTONE_BLOCK, 2700, 270, "resources"));
        resources.items.add(new ShopItem(Material.EMERALD_BLOCK, 36000, 3600, "resources"));
        resources.items.add(new ShopItem(Material.LAPIS_BLOCK, 7200, 720, "resources"));
        resources.items.add(new ShopItem(Material.AMETHYST_BLOCK, 2000, 200, "resources"));
        resources.items.add(new ShopItem(Material.QUARTZ_BLOCK, 1600, 160, "resources"));

        // Crafting Materials
        resources.items.add(new ShopItem(Material.STICK, 25, 2, "resources"));
        resources.items.add(new ShopItem(Material.FLINT, 100, 10, "resources"));
        resources.items.add(new ShopItem(Material.FEATHER, 100, 10, "resources"));
        resources.items.add(new ShopItem(Material.LEATHER, 300, 30, "resources"));
        resources.items.add(new ShopItem(Material.RABBIT_HIDE, 200, 20, "resources"));
        resources.items.add(new ShopItem(Material.STRING, 200, 20, "resources"));
        resources.items.add(new ShopItem(Material.SLIME_BALL, 500, 50, "resources"));
        resources.items.add(new ShopItem(Material.BONE, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.BONE_MEAL, 200, 20, "resources"));
        resources.items.add(new ShopItem(Material.GUNPOWDER, 300, 30, "resources"));
        resources.items.add(new ShopItem(Material.INK_SAC, 200, 20, "resources"));
        resources.items.add(new ShopItem(Material.GLOW_INK_SAC, 500, 50, "resources"));
        resources.items.add(new ShopItem(Material.PRISMARINE_SHARD, 400, 40, "resources"));
        resources.items.add(new ShopItem(Material.PRISMARINE_CRYSTALS, 500, 50, "resources"));
        resources.items.add(new ShopItem(Material.ECHO_SHARD, 8000, 800, "resources"));
        resources.items.add(new ShopItem(Material.DISC_FRAGMENT_5, 5000, 500, "resources"));
        resources.items.add(new ShopItem(Material.HONEYCOMB, 500, 50, "resources"));

        // Mob Drops
        resources.items.add(new ShopItem(Material.ROTTEN_FLESH, 50, 5, "resources"));
        resources.items.add(new ShopItem(Material.SPIDER_EYE, 200, 20, "resources"));
        resources.items.add(new ShopItem(Material.ENDER_EYE, 5000, 500, "resources"));
        resources.items.add(new ShopItem(Material.TURTLE_SCUTE, 2000, 200, "resources"));
        resources.items.add(new ShopItem(Material.PHANTOM_MEMBRANE, 2000, 200, "resources"));
        resources.items.add(new ShopItem(Material.RABBIT_FOOT, 1000, 100, "resources"));

        // Dyes
        resources.items.add(new ShopItem(Material.WHITE_DYE, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.ORANGE_DYE, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.MAGENTA_DYE, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.LIGHT_BLUE_DYE, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.YELLOW_DYE, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.LIME_DYE, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.PINK_DYE, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.GRAY_DYE, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.LIGHT_GRAY_DYE, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.CYAN_DYE, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.PURPLE_DYE, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.BLUE_DYE, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.BROWN_DYE, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.GREEN_DYE, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.RED_DYE, 150, 15, "resources"));
        resources.items.add(new ShopItem(Material.BLACK_DYE, 150, 15, "resources"));

        categories.put("resources", resources);

        // ══════════════════════════════════════
        //  CATEGORY 4: NATURAL
        // ══════════════════════════════════════
        Category natural = new Category("natural", "§2§lNatural", "§2", Material.GRASS_BLOCK);

        // Dirt & Soil
        natural.items.add(new ShopItem(Material.DIRT, 50, 5, "natural"));
        natural.items.add(new ShopItem(Material.GRASS_BLOCK, 100, 10, "natural"));
        natural.items.add(new ShopItem(Material.PODZOL, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.COARSE_DIRT, 75, 7, "natural"));
        natural.items.add(new ShopItem(Material.ROOTED_DIRT, 100, 10, "natural"));
        natural.items.add(new ShopItem(Material.MYCELIUM, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.MUD, 100, 10, "natural"));
        natural.items.add(new ShopItem(Material.MUDDY_MANGROVE_ROOTS, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.CLAY, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.GRAVEL, 75, 7, "natural"));
        natural.items.add(new ShopItem(Material.SAND, 100, 10, "natural"));
        natural.items.add(new ShopItem(Material.RED_SAND, 150, 15, "natural"));

        // Stone
        natural.items.add(new ShopItem(Material.STONE, 100, 10, "natural"));
        natural.items.add(new ShopItem(Material.COBBLESTONE, 50, 5, "natural"));
        natural.items.add(new ShopItem(Material.MOSSY_COBBLESTONE, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.DEEPSLATE, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.COBBLED_DEEPSLATE, 100, 10, "natural"));
        natural.items.add(new ShopItem(Material.ANDESITE, 75, 7, "natural"));
        natural.items.add(new ShopItem(Material.DIORITE, 75, 7, "natural"));
        natural.items.add(new ShopItem(Material.GRANITE, 75, 7, "natural"));
        natural.items.add(new ShopItem(Material.TUFF, 100, 10, "natural"));
        natural.items.add(new ShopItem(Material.CALCITE, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.DRIPSTONE_BLOCK, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.POINTED_DRIPSTONE, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.BASALT, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.SMOOTH_BASALT, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.BLACKSTONE, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.GILDED_BLACKSTONE, 1000, 100, "natural"));
        natural.items.add(new ShopItem(Material.OBSIDIAN, 1500, 150, "natural"));
        natural.items.add(new ShopItem(Material.CRYING_OBSIDIAN, 3000, 300, "natural"));

        // Nether
        natural.items.add(new ShopItem(Material.NETHERRACK, 50, 5, "natural"));
        natural.items.add(new ShopItem(Material.SOUL_SAND, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.SOUL_SOIL, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.MAGMA_BLOCK, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.GLOWSTONE, 500, 50, "natural"));
        natural.items.add(new ShopItem(Material.SHROOMLIGHT, 750, 75, "natural"));
        natural.items.add(new ShopItem(Material.CRIMSON_NYLIUM, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.WARPED_NYLIUM, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.NETHER_WART_BLOCK, 400, 40, "natural"));
        natural.items.add(new ShopItem(Material.WARPED_WART_BLOCK, 400, 40, "natural"));
        natural.items.add(new ShopItem(Material.END_STONE, 500, 50, "natural"));

        // Ice & Snow
        natural.items.add(new ShopItem(Material.SNOW_BLOCK, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.SNOW, 50, 5, "natural"));
        natural.items.add(new ShopItem(Material.ICE, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.PACKED_ICE, 500, 50, "natural"));
        natural.items.add(new ShopItem(Material.BLUE_ICE, 1000, 100, "natural"));
        natural.items.add(new ShopItem(Material.POWDER_SNOW, 400, 40, "natural"));

        // Moss & Vines
        natural.items.add(new ShopItem(Material.MOSS_BLOCK, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.MOSS_CARPET, 100, 10, "natural"));
        natural.items.add(new ShopItem(Material.VINE, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.GLOW_LICHEN, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.SPORE_BLOSSOM, 500, 50, "natural"));
        natural.items.add(new ShopItem(Material.HANGING_ROOTS, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.SMALL_DRIPLEAF, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.BIG_DRIPLEAF, 500, 50, "natural"));
        natural.items.add(new ShopItem(Material.AZALEA, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.FLOWERING_AZALEA, 500, 50, "natural"));
        natural.items.add(new ShopItem(Material.AZALEA_LEAVES, 100, 10, "natural"));
        natural.items.add(new ShopItem(Material.FLOWERING_AZALEA_LEAVES, 150, 15, "natural"));

        // Nether Plants
        natural.items.add(new ShopItem(Material.CRIMSON_FUNGUS, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.WARPED_FUNGUS, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.CRIMSON_ROOTS, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.WARPED_ROOTS, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.NETHER_SPROUTS, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.TWISTING_VINES, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.WEEPING_VINES, 200, 20, "natural"));

        // End Plants
        natural.items.add(new ShopItem(Material.CHORUS_PLANT, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.CHORUS_FLOWER, 500, 50, "natural"));

        // Aquatic
        natural.items.add(new ShopItem(Material.KELP, 75, 7, "natural"));
        natural.items.add(new ShopItem(Material.SEAGRASS, 75, 7, "natural"));
        natural.items.add(new ShopItem(Material.SEA_PICKLE, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.SPONGE, 5000, 500, "natural"));
        natural.items.add(new ShopItem(Material.WET_SPONGE, 4500, 450, "natural"));

        // Coral
        natural.items.add(new ShopItem(Material.TUBE_CORAL_BLOCK, 500, 50, "natural"));
        natural.items.add(new ShopItem(Material.BRAIN_CORAL_BLOCK, 500, 50, "natural"));
        natural.items.add(new ShopItem(Material.BUBBLE_CORAL_BLOCK, 500, 50, "natural"));
        natural.items.add(new ShopItem(Material.FIRE_CORAL_BLOCK, 500, 50, "natural"));
        natural.items.add(new ShopItem(Material.HORN_CORAL_BLOCK, 500, 50, "natural"));
        natural.items.add(new ShopItem(Material.DEAD_TUBE_CORAL_BLOCK, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.DEAD_BRAIN_CORAL_BLOCK, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.DEAD_BUBBLE_CORAL_BLOCK, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.DEAD_FIRE_CORAL_BLOCK, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.DEAD_HORN_CORAL_BLOCK, 200, 20, "natural"));

        // Leaves
        natural.items.add(new ShopItem(Material.OAK_LEAVES, 50, 5, "natural"));
        natural.items.add(new ShopItem(Material.SPRUCE_LEAVES, 50, 5, "natural"));
        natural.items.add(new ShopItem(Material.BIRCH_LEAVES, 50, 5, "natural"));
        natural.items.add(new ShopItem(Material.JUNGLE_LEAVES, 50, 5, "natural"));
        natural.items.add(new ShopItem(Material.ACACIA_LEAVES, 50, 5, "natural"));
        natural.items.add(new ShopItem(Material.DARK_OAK_LEAVES, 50, 5, "natural"));
        natural.items.add(new ShopItem(Material.MANGROVE_LEAVES, 75, 7, "natural"));
        natural.items.add(new ShopItem(Material.CHERRY_LEAVES, 100, 10, "natural"));

        // Logs
        natural.items.add(new ShopItem(Material.OAK_LOG, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.SPRUCE_LOG, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.BIRCH_LOG, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.JUNGLE_LOG, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.ACACIA_LOG, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.DARK_OAK_LOG, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.MANGROVE_LOG, 250, 25, "natural"));
        natural.items.add(new ShopItem(Material.CHERRY_LOG, 300, 30, "natural"));

        // Wood
        natural.items.add(new ShopItem(Material.OAK_WOOD, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.SPRUCE_WOOD, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.BIRCH_WOOD, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.JUNGLE_WOOD, 250, 25, "natural"));
        natural.items.add(new ShopItem(Material.ACACIA_WOOD, 250, 25, "natural"));
        natural.items.add(new ShopItem(Material.DARK_OAK_WOOD, 250, 25, "natural"));
        natural.items.add(new ShopItem(Material.MANGROVE_WOOD, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.CHERRY_WOOD, 350, 35, "natural"));

        // Stripped Logs
        natural.items.add(new ShopItem(Material.STRIPPED_OAK_LOG, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_SPRUCE_LOG, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_BIRCH_LOG, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_JUNGLE_LOG, 250, 25, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_ACACIA_LOG, 250, 25, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_DARK_OAK_LOG, 250, 25, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_MANGROVE_LOG, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_CHERRY_LOG, 350, 35, "natural"));

        // Stripped Wood
        natural.items.add(new ShopItem(Material.STRIPPED_OAK_WOOD, 250, 25, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_SPRUCE_WOOD, 250, 25, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_BIRCH_WOOD, 250, 25, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_JUNGLE_WOOD, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_ACACIA_WOOD, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_DARK_OAK_WOOD, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_MANGROVE_WOOD, 350, 35, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_CHERRY_WOOD, 400, 40, "natural"));

        // Nether Stems
        natural.items.add(new ShopItem(Material.CRIMSON_STEM, 500, 50, "natural"));
        natural.items.add(new ShopItem(Material.WARPED_STEM, 500, 50, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_CRIMSON_STEM, 600, 60, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_WARPED_STEM, 600, 60, "natural"));
        natural.items.add(new ShopItem(Material.CRIMSON_HYPHAE, 600, 60, "natural"));
        natural.items.add(new ShopItem(Material.WARPED_HYPHAE, 600, 60, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_CRIMSON_HYPHAE, 700, 70, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_WARPED_HYPHAE, 700, 70, "natural"));

        // Bamboo
        natural.items.add(new ShopItem(Material.BAMBOO, 100, 10, "natural"));
        natural.items.add(new ShopItem(Material.BAMBOO_BLOCK, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.STRIPPED_BAMBOO_BLOCK, 250, 25, "natural"));

        // Flowers
        natural.items.add(new ShopItem(Material.DANDELION, 100, 10, "natural"));
        natural.items.add(new ShopItem(Material.POPPY, 100, 10, "natural"));
        natural.items.add(new ShopItem(Material.BLUE_ORCHID, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.ALLIUM, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.AZURE_BLUET, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.RED_TULIP, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.ORANGE_TULIP, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.WHITE_TULIP, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.PINK_TULIP, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.OXEYE_DAISY, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.CORNFLOWER, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.LILY_OF_THE_VALLEY, 200, 20, "natural"));
        natural.items.add(new ShopItem(Material.WITHER_ROSE, 5000, 500, "natural"));
        natural.items.add(new ShopItem(Material.TORCHFLOWER, 3000, 300, "natural"));
        natural.items.add(new ShopItem(Material.PITCHER_PLANT, 3000, 300, "natural"));
        natural.items.add(new ShopItem(Material.SUNFLOWER, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.LILAC, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.ROSE_BUSH, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.PEONY, 300, 30, "natural"));

        // Mushrooms
        natural.items.add(new ShopItem(Material.BROWN_MUSHROOM, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.RED_MUSHROOM, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.BROWN_MUSHROOM_BLOCK, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.RED_MUSHROOM_BLOCK, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.MUSHROOM_STEM, 250, 25, "natural"));

        // Grass & Fern
        natural.items.add(new ShopItem(Material.SHORT_GRASS, 50, 5, "natural"));
        natural.items.add(new ShopItem(Material.TALL_GRASS, 75, 7, "natural"));
        natural.items.add(new ShopItem(Material.FERN, 75, 7, "natural"));
        natural.items.add(new ShopItem(Material.LARGE_FERN, 100, 10, "natural"));
        natural.items.add(new ShopItem(Material.DEAD_BUSH, 50, 5, "natural"));

        // Saplings
        natural.items.add(new ShopItem(Material.OAK_SAPLING, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.SPRUCE_SAPLING, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.BIRCH_SAPLING, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.JUNGLE_SAPLING, 500, 50, "natural"));
        natural.items.add(new ShopItem(Material.ACACIA_SAPLING, 400, 40, "natural"));
        natural.items.add(new ShopItem(Material.DARK_OAK_SAPLING, 400, 40, "natural"));
        natural.items.add(new ShopItem(Material.MANGROVE_PROPAGULE, 500, 50, "natural"));
        natural.items.add(new ShopItem(Material.CHERRY_SAPLING, 750, 75, "natural"));

        // Crops & Plants
        natural.items.add(new ShopItem(Material.PUMPKIN, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.MELON, 500, 50, "natural"));
        natural.items.add(new ShopItem(Material.CACTUS, 150, 15, "natural"));
        natural.items.add(new ShopItem(Material.SUGAR_CANE, 200, 20, "natural"));

        // Eggs & Spawn
        natural.items.add(new ShopItem(Material.TURTLE_EGG, 3000, 300, "natural"));
        natural.items.add(new ShopItem(Material.SNIFFER_EGG, 5000, 500, "natural"));
        natural.items.add(new ShopItem(Material.FROGSPAWN, 2000, 200, "natural"));

        // Sculk
        natural.items.add(new ShopItem(Material.SCULK, 500, 50, "natural"));
        natural.items.add(new ShopItem(Material.SCULK_VEIN, 300, 30, "natural"));
        natural.items.add(new ShopItem(Material.SCULK_CATALYST, 3000, 300, "natural"));
        natural.items.add(new ShopItem(Material.SCULK_SENSOR, 5000, 500, "natural"));
        natural.items.add(new ShopItem(Material.CALIBRATED_SCULK_SENSOR, 8000, 800, "natural"));
        natural.items.add(new ShopItem(Material.SCULK_SHRIEKER, 5000, 500, "natural"));

        categories.put("natural", natural);

            // ══════════════════════════════════════
        //  CATEGORY 5: DECORATION
        // ══════════════════════════════════════
        Category decoration = new Category("decoration", "§d§lDecoration", "§d", Material.LANTERN);

        // Stone Bricks
        decoration.items.add(new ShopItem(Material.STONE_BRICKS, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.MOSSY_STONE_BRICKS, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.CRACKED_STONE_BRICKS, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.CHISELED_STONE_BRICKS, 500, 50, "decoration"));

        // Deepslate Bricks
        decoration.items.add(new ShopItem(Material.DEEPSLATE_BRICKS, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.CRACKED_DEEPSLATE_BRICKS, 450, 45, "decoration"));
        decoration.items.add(new ShopItem(Material.DEEPSLATE_TILES, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.CRACKED_DEEPSLATE_TILES, 450, 45, "decoration"));
        decoration.items.add(new ShopItem(Material.CHISELED_DEEPSLATE, 500, 50, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_DEEPSLATE, 350, 35, "decoration"));

        // Bricks
        decoration.items.add(new ShopItem(Material.BRICKS, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.BRICK_SLAB, 200, 20, "decoration"));
        decoration.items.add(new ShopItem(Material.BRICK_STAIRS, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.BRICK_WALL, 400, 40, "decoration"));

        // Mud Bricks
        decoration.items.add(new ShopItem(Material.MUD_BRICKS, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.MUD_BRICK_SLAB, 175, 17, "decoration"));
        decoration.items.add(new ShopItem(Material.MUD_BRICK_STAIRS, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.MUD_BRICK_WALL, 350, 35, "decoration"));

        // Prismarine
        decoration.items.add(new ShopItem(Material.PRISMARINE, 600, 60, "decoration"));
        decoration.items.add(new ShopItem(Material.PRISMARINE_BRICKS, 800, 80, "decoration"));
        decoration.items.add(new ShopItem(Material.DARK_PRISMARINE, 800, 80, "decoration"));

        // Nether Bricks
        decoration.items.add(new ShopItem(Material.NETHER_BRICKS, 500, 50, "decoration"));
        decoration.items.add(new ShopItem(Material.CRACKED_NETHER_BRICKS, 600, 60, "decoration"));
        decoration.items.add(new ShopItem(Material.CHISELED_NETHER_BRICKS, 700, 70, "decoration"));
        decoration.items.add(new ShopItem(Material.RED_NETHER_BRICKS, 700, 70, "decoration"));

        // End & Purpur
        decoration.items.add(new ShopItem(Material.END_STONE_BRICKS, 800, 80, "decoration"));
        decoration.items.add(new ShopItem(Material.PURPUR_BLOCK, 900, 90, "decoration"));
        decoration.items.add(new ShopItem(Material.PURPUR_PILLAR, 1000, 100, "decoration"));

        // Quartz
        decoration.items.add(new ShopItem(Material.CHISELED_QUARTZ_BLOCK, 600, 60, "decoration"));
        decoration.items.add(new ShopItem(Material.QUARTZ_BRICKS, 550, 55, "decoration"));
        decoration.items.add(new ShopItem(Material.QUARTZ_PILLAR, 600, 60, "decoration"));
        decoration.items.add(new ShopItem(Material.SMOOTH_QUARTZ, 500, 50, "decoration"));

        // Blackstone
        decoration.items.add(new ShopItem(Material.POLISHED_BLACKSTONE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_BLACKSTONE_BRICKS, 450, 45, "decoration"));
        decoration.items.add(new ShopItem(Material.CRACKED_POLISHED_BLACKSTONE_BRICKS, 500, 50, "decoration"));
        decoration.items.add(new ShopItem(Material.CHISELED_POLISHED_BLACKSTONE, 550, 55, "decoration"));

        // Cobblestone Variants
        decoration.items.add(new ShopItem(Material.COBBLESTONE_SLAB, 75, 7, "decoration"));
        decoration.items.add(new ShopItem(Material.COBBLESTONE_STAIRS, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.COBBLESTONE_WALL, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.MOSSY_COBBLESTONE_SLAB, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.MOSSY_COBBLESTONE_STAIRS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.MOSSY_COBBLESTONE_WALL, 300, 30, "decoration"));

        // Stone Variants
        decoration.items.add(new ShopItem(Material.STONE_SLAB, 100, 10, "decoration"));
        decoration.items.add(new ShopItem(Material.STONE_STAIRS, 200, 20, "decoration"));
        decoration.items.add(new ShopItem(Material.STONE_BRICK_SLAB, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.STONE_BRICK_STAIRS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.STONE_BRICK_WALL, 300, 30, "decoration"));

        // Polished Variants
        decoration.items.add(new ShopItem(Material.POLISHED_ANDESITE, 200, 20, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_ANDESITE_SLAB, 100, 10, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_ANDESITE_STAIRS, 200, 20, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_DIORITE, 200, 20, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_DIORITE_SLAB, 100, 10, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_DIORITE_STAIRS, 200, 20, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_GRANITE, 200, 20, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_GRANITE_SLAB, 100, 10, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_GRANITE_STAIRS, 200, 20, "decoration"));

        // Raw Stone Slabs/Stairs/Walls
        decoration.items.add(new ShopItem(Material.ANDESITE_SLAB, 75, 7, "decoration"));
        decoration.items.add(new ShopItem(Material.ANDESITE_STAIRS, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.ANDESITE_WALL, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.DIORITE_SLAB, 75, 7, "decoration"));
        decoration.items.add(new ShopItem(Material.DIORITE_STAIRS, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.DIORITE_WALL, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.GRANITE_SLAB, 75, 7, "decoration"));
        decoration.items.add(new ShopItem(Material.GRANITE_STAIRS, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.GRANITE_WALL, 150, 15, "decoration"));

        // Deepslate Variants
        decoration.items.add(new ShopItem(Material.DEEPSLATE_BRICK_SLAB, 200, 20, "decoration"));
        decoration.items.add(new ShopItem(Material.DEEPSLATE_BRICK_STAIRS, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.DEEPSLATE_BRICK_WALL, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.DEEPSLATE_TILE_SLAB, 200, 20, "decoration"));
        decoration.items.add(new ShopItem(Material.DEEPSLATE_TILE_STAIRS, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.DEEPSLATE_TILE_WALL, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_DEEPSLATE_SLAB, 200, 20, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_DEEPSLATE_STAIRS, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_DEEPSLATE_WALL, 400, 40, "decoration"));

        // Tuff Variants
        decoration.items.add(new ShopItem(Material.TUFF_SLAB, 100, 10, "decoration"));
        decoration.items.add(new ShopItem(Material.TUFF_STAIRS, 200, 20, "decoration"));
        decoration.items.add(new ShopItem(Material.TUFF_WALL, 200, 20, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_TUFF, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_TUFF_SLAB, 125, 12, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_TUFF_STAIRS, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.POLISHED_TUFF_WALL, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.TUFF_BRICKS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.TUFF_BRICK_SLAB, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.TUFF_BRICK_STAIRS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.TUFF_BRICK_WALL, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.CHISELED_TUFF, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.CHISELED_TUFF_BRICKS, 400, 40, "decoration"));

        // Smooth Stone
        decoration.items.add(new ShopItem(Material.SMOOTH_STONE, 200, 20, "decoration"));
        decoration.items.add(new ShopItem(Material.SMOOTH_STONE_SLAB, 100, 10, "decoration"));

        // Glass
        decoration.items.add(new ShopItem(Material.GLASS, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.GLASS_PANE, 125, 12, "decoration"));
        decoration.items.add(new ShopItem(Material.TINTED_GLASS, 1200, 120, "decoration"));

        // Stained Glass
        decoration.items.add(new ShopItem(Material.WHITE_STAINED_GLASS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.ORANGE_STAINED_GLASS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.MAGENTA_STAINED_GLASS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.LIGHT_BLUE_STAINED_GLASS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.YELLOW_STAINED_GLASS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.LIME_STAINED_GLASS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.PINK_STAINED_GLASS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.GRAY_STAINED_GLASS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.LIGHT_GRAY_STAINED_GLASS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.CYAN_STAINED_GLASS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.PURPLE_STAINED_GLASS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.BLUE_STAINED_GLASS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.BROWN_STAINED_GLASS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.GREEN_STAINED_GLASS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.RED_STAINED_GLASS, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.BLACK_STAINED_GLASS, 300, 30, "decoration"));

        // Stained Glass Pane
        decoration.items.add(new ShopItem(Material.WHITE_STAINED_GLASS_PANE, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.ORANGE_STAINED_GLASS_PANE, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.MAGENTA_STAINED_GLASS_PANE, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.YELLOW_STAINED_GLASS_PANE, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.LIME_STAINED_GLASS_PANE, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.PINK_STAINED_GLASS_PANE, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.GRAY_STAINED_GLASS_PANE, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.CYAN_STAINED_GLASS_PANE, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.PURPLE_STAINED_GLASS_PANE, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.BLUE_STAINED_GLASS_PANE, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.BROWN_STAINED_GLASS_PANE, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.GREEN_STAINED_GLASS_PANE, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.RED_STAINED_GLASS_PANE, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.BLACK_STAINED_GLASS_PANE, 150, 15, "decoration"));

        // Terracotta
        decoration.items.add(new ShopItem(Material.TERRACOTTA, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.WHITE_TERRACOTTA, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.ORANGE_TERRACOTTA, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.MAGENTA_TERRACOTTA, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.LIGHT_BLUE_TERRACOTTA, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.YELLOW_TERRACOTTA, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.LIME_TERRACOTTA, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.PINK_TERRACOTTA, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.GRAY_TERRACOTTA, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.LIGHT_GRAY_TERRACOTTA, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.CYAN_TERRACOTTA, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.PURPLE_TERRACOTTA, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.BLUE_TERRACOTTA, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.BROWN_TERRACOTTA, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.GREEN_TERRACOTTA, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.RED_TERRACOTTA, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.BLACK_TERRACOTTA, 300, 30, "decoration"));

        // Concrete
        decoration.items.add(new ShopItem(Material.WHITE_CONCRETE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.ORANGE_CONCRETE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.MAGENTA_CONCRETE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.LIGHT_BLUE_CONCRETE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.YELLOW_CONCRETE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.LIME_CONCRETE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.PINK_CONCRETE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.GRAY_CONCRETE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.LIGHT_GRAY_CONCRETE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.CYAN_CONCRETE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.PURPLE_CONCRETE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.BLUE_CONCRETE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.BROWN_CONCRETE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.GREEN_CONCRETE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.RED_CONCRETE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.BLACK_CONCRETE, 350, 35, "decoration"));

        // Concrete Powder
        decoration.items.add(new ShopItem(Material.WHITE_CONCRETE_POWDER, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.ORANGE_CONCRETE_POWDER, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.MAGENTA_CONCRETE_POWDER, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.LIGHT_BLUE_CONCRETE_POWDER, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.YELLOW_CONCRETE_POWDER, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.LIME_CONCRETE_POWDER, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.PINK_CONCRETE_POWDER, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.GRAY_CONCRETE_POWDER, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.LIGHT_GRAY_CONCRETE_POWDER, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.CYAN_CONCRETE_POWDER, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.PURPLE_CONCRETE_POWDER, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.BLUE_CONCRETE_POWDER, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.BROWN_CONCRETE_POWDER, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.GREEN_CONCRETE_POWDER, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.RED_CONCRETE_POWDER, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.BLACK_CONCRETE_POWDER, 250, 25, "decoration"));

        // Wool
        decoration.items.add(new ShopItem(Material.WHITE_WOOL, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.ORANGE_WOOL, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.MAGENTA_WOOL, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.LIGHT_BLUE_WOOL, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.YELLOW_WOOL, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.LIME_WOOL, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.PINK_WOOL, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.GRAY_WOOL, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.LIGHT_GRAY_WOOL, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.CYAN_WOOL, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.PURPLE_WOOL, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.BLUE_WOOL, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.BROWN_WOOL, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.GREEN_WOOL, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.RED_WOOL, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.BLACK_WOOL, 250, 25, "decoration"));

        // Lights
        decoration.items.add(new ShopItem(Material.TORCH, 75, 7, "decoration"));
        decoration.items.add(new ShopItem(Material.SOUL_TORCH, 150, 15, "decoration"));
        decoration.items.add(new ShopItem(Material.LANTERN, 600, 60, "decoration"));
        decoration.items.add(new ShopItem(Material.SOUL_LANTERN, 800, 80, "decoration"));
        decoration.items.add(new ShopItem(Material.CAMPFIRE, 1000, 100, "decoration"));
        decoration.items.add(new ShopItem(Material.SOUL_CAMPFIRE, 1200, 120, "decoration"));
        decoration.items.add(new ShopItem(Material.END_ROD, 1200, 120, "decoration"));
        decoration.items.add(new ShopItem(Material.SEA_LANTERN, 1200, 120, "decoration"));
        decoration.items.add(new ShopItem(Material.REDSTONE_LAMP, 1200, 120, "decoration"));
        decoration.items.add(new ShopItem(Material.OCHRE_FROGLIGHT, 2500, 250, "decoration"));
        decoration.items.add(new ShopItem(Material.VERDANT_FROGLIGHT, 2500, 250, "decoration"));
        decoration.items.add(new ShopItem(Material.PEARLESCENT_FROGLIGHT, 2500, 250, "decoration"));
        decoration.items.add(new ShopItem(Material.SHROOMLIGHT, 800, 80, "decoration"));
        decoration.items.add(new ShopItem(Material.GLOWSTONE, 600, 60, "decoration"));
        decoration.items.add(new ShopItem(Material.JACK_O_LANTERN, 500, 50, "decoration"));

        // Candles
        decoration.items.add(new ShopItem(Material.CANDLE, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.WHITE_CANDLE, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.ORANGE_CANDLE, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.MAGENTA_CANDLE, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.LIGHT_BLUE_CANDLE, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.YELLOW_CANDLE, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.LIME_CANDLE, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.PINK_CANDLE, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.GRAY_CANDLE, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.LIGHT_GRAY_CANDLE, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.CYAN_CANDLE, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.PURPLE_CANDLE, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.BLUE_CANDLE, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.BROWN_CANDLE, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.GREEN_CANDLE, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.RED_CANDLE, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.BLACK_CANDLE, 400, 40, "decoration"));

        // Decorative Items
        decoration.items.add(new ShopItem(Material.PAINTING, 600, 60, "decoration"));
        decoration.items.add(new ShopItem(Material.ITEM_FRAME, 500, 50, "decoration"));
        decoration.items.add(new ShopItem(Material.GLOW_ITEM_FRAME, 1200, 120, "decoration"));
        decoration.items.add(new ShopItem(Material.FLOWER_POT, 400, 40, "decoration"));
        decoration.items.add(new ShopItem(Material.DECORATED_POT, 2500, 250, "decoration"));
        decoration.items.add(new ShopItem(Material.ARMOR_STAND, 1800, 180, "decoration"));
        decoration.items.add(new ShopItem(Material.BELL, 3500, 350, "decoration"));
        decoration.items.add(new ShopItem(Material.CHAIN, 600, 60, "decoration"));
        decoration.items.add(new ShopItem(Material.LODESTONE, 8000, 800, "decoration"));
        decoration.items.add(new ShopItem(Material.LIGHTNING_ROD, 2500, 250, "decoration"));

        // Bookshelf
        decoration.items.add(new ShopItem(Material.BOOKSHELF, 1200, 120, "decoration"));
        decoration.items.add(new ShopItem(Material.CHISELED_BOOKSHELF, 1800, 180, "decoration"));

        // Jukebox & Note
        decoration.items.add(new ShopItem(Material.JUKEBOX, 3000, 300, "decoration"));
        decoration.items.add(new ShopItem(Material.NOTE_BLOCK, 500, 50, "decoration"));

        // Tinted Glass
        decoration.items.add(new ShopItem(Material.TINTED_GLASS, 1200, 120, "decoration"));

        // Sandstone Variants
        decoration.items.add(new ShopItem(Material.SANDSTONE, 200, 20, "decoration"));
        decoration.items.add(new ShopItem(Material.CHISELED_SANDSTONE, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.CUT_SANDSTONE, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.SMOOTH_SANDSTONE, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.RED_SANDSTONE, 250, 25, "decoration"));
        decoration.items.add(new ShopItem(Material.CHISELED_RED_SANDSTONE, 350, 35, "decoration"));
        decoration.items.add(new ShopItem(Material.CUT_RED_SANDSTONE, 300, 30, "decoration"));
        decoration.items.add(new ShopItem(Material.SMOOTH_RED_SANDSTONE, 300, 30, "decoration"));

        // Bamboo Mosaic
        decoration.items.add(new ShopItem(Material.BAMBOO_MOSAIC, 300, 30, "decoration"));

        categories.put("decoration", decoration);

        // ══════════════════════════════════════
        //  CATEGORY 6: MISC
        // ══════════════════════════════════════
        Category misc = new Category("misc", "§b§lMisc", "§b", Material.CHEST);

        // Buckets
        misc.items.add(new ShopItem(Material.BUCKET, 1200, 120, "misc"));
        misc.items.add(new ShopItem(Material.WATER_BUCKET, 1800, 180, "misc"));
        misc.items.add(new ShopItem(Material.LAVA_BUCKET, 3500, 350, "misc"));
        misc.items.add(new ShopItem(Material.POWDER_SNOW_BUCKET, 2500, 250, "misc"));
        misc.items.add(new ShopItem(Material.COD_BUCKET, 1500, 150, "misc"));
        misc.items.add(new ShopItem(Material.SALMON_BUCKET, 1500, 150, "misc"));
        misc.items.add(new ShopItem(Material.PUFFERFISH_BUCKET, 2500, 250, "misc"));
        misc.items.add(new ShopItem(Material.TROPICAL_FISH_BUCKET, 3000, 300, "misc"));
        misc.items.add(new ShopItem(Material.AXOLOTL_BUCKET, 5000, 500, "misc"));
        misc.items.add(new ShopItem(Material.TADPOLE_BUCKET, 2000, 200, "misc"));

        // Arrows
        misc.items.add(new ShopItem(Material.ARROW, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.SPECTRAL_ARROW, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.TIPPED_ARROW, 1000, 100, "misc"));

        // Utility Items
        misc.items.add(new ShopItem(Material.FISHING_ROD, 3500, 350, "misc"));
        misc.items.add(new ShopItem(Material.SHEARS, 2500, 250, "misc"));
        misc.items.add(new ShopItem(Material.FLINT_AND_STEEL, 3500, 350, "misc"));
        misc.items.add(new ShopItem(Material.BRUSH, 5500, 550, "misc"));
        misc.items.add(new ShopItem(Material.CARROT_ON_A_STICK, 1500, 150, "misc"));
        misc.items.add(new ShopItem(Material.WARPED_FUNGUS_ON_A_STICK, 2000, 200, "misc"));
        misc.items.add(new ShopItem(Material.LEAD, 3000, 300, "misc"));
        misc.items.add(new ShopItem(Material.NAME_TAG, 12000, 1200, "misc"));
        misc.items.add(new ShopItem(Material.COMPASS, 3500, 350, "misc"));
        misc.items.add(new ShopItem(Material.RECOVERY_COMPASS, 28000, 2800, "misc"));
        misc.items.add(new ShopItem(Material.CLOCK, 3500, 350, "misc"));
        misc.items.add(new ShopItem(Material.SPYGLASS, 6000, 600, "misc"));

        // Horse Equipment
        misc.items.add(new ShopItem(Material.SADDLE, 18000, 1800, "misc"));
        misc.items.add(new ShopItem(Material.LEATHER_HORSE_ARMOR, 5000, 500, "misc"));
        misc.items.add(new ShopItem(Material.IRON_HORSE_ARMOR, 12000, 1200, "misc"));
        misc.items.add(new ShopItem(Material.GOLDEN_HORSE_ARMOR, 22000, 2200, "misc"));
        misc.items.add(new ShopItem(Material.DIAMOND_HORSE_ARMOR, 40000, 4000, "misc"));

        // Boats
        misc.items.add(new ShopItem(Material.OAK_BOAT, 1200, 120, "misc"));
        misc.items.add(new ShopItem(Material.SPRUCE_BOAT, 1200, 120, "misc"));
        misc.items.add(new ShopItem(Material.BIRCH_BOAT, 1200, 120, "misc"));
        misc.items.add(new ShopItem(Material.JUNGLE_BOAT, 1500, 150, "misc"));
        misc.items.add(new ShopItem(Material.ACACIA_BOAT, 1500, 150, "misc"));
        misc.items.add(new ShopItem(Material.DARK_OAK_BOAT, 1500, 150, "misc"));
        misc.items.add(new ShopItem(Material.MANGROVE_BOAT, 1800, 180, "misc"));
        misc.items.add(new ShopItem(Material.CHERRY_BOAT, 2000, 200, "misc"));
        misc.items.add(new ShopItem(Material.BAMBOO_RAFT, 1500, 150, "misc"));

        // Chest Boats
        misc.items.add(new ShopItem(Material.OAK_CHEST_BOAT, 1800, 180, "misc"));
        misc.items.add(new ShopItem(Material.SPRUCE_CHEST_BOAT, 1800, 180, "misc"));
        misc.items.add(new ShopItem(Material.BIRCH_CHEST_BOAT, 1800, 180, "misc"));
        misc.items.add(new ShopItem(Material.JUNGLE_CHEST_BOAT, 2000, 200, "misc"));
        misc.items.add(new ShopItem(Material.ACACIA_CHEST_BOAT, 2000, 200, "misc"));
        misc.items.add(new ShopItem(Material.DARK_OAK_CHEST_BOAT, 2000, 200, "misc"));
        misc.items.add(new ShopItem(Material.MANGROVE_CHEST_BOAT, 2500, 250, "misc"));
        misc.items.add(new ShopItem(Material.CHERRY_CHEST_BOAT, 2800, 280, "misc"));
        misc.items.add(new ShopItem(Material.BAMBOO_CHEST_RAFT, 2000, 200, "misc"));

        // Minecarts
        misc.items.add(new ShopItem(Material.MINECART, 3500, 350, "misc"));
        misc.items.add(new ShopItem(Material.CHEST_MINECART, 4500, 450, "misc"));
        misc.items.add(new ShopItem(Material.FURNACE_MINECART, 5000, 500, "misc"));
        misc.items.add(new ShopItem(Material.TNT_MINECART, 8000, 800, "misc"));
        misc.items.add(new ShopItem(Material.HOPPER_MINECART, 7000, 700, "misc"));

        // Rails
        misc.items.add(new ShopItem(Material.RAIL, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.POWERED_RAIL, 1800, 180, "misc"));
        misc.items.add(new ShopItem(Material.DETECTOR_RAIL, 1200, 120, "misc"));
        misc.items.add(new ShopItem(Material.ACTIVATOR_RAIL, 1200, 120, "misc"));

        // Throwables
        misc.items.add(new ShopItem(Material.FIRE_CHARGE, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.SNOWBALL, 100, 10, "misc"));
        misc.items.add(new ShopItem(Material.WIND_CHARGE, 2000, 200, "misc"));
        misc.items.add(new ShopItem(Material.EXPERIENCE_BOTTLE, 1800, 180, "misc"));

        // Books & Maps
        misc.items.add(new ShopItem(Material.MAP, 1500, 150, "misc"));
        misc.items.add(new ShopItem(Material.WRITABLE_BOOK, 1200, 120, "misc"));
        misc.items.add(new ShopItem(Material.BOOK, 500, 50, "misc"));
        misc.items.add(new ShopItem(Material.ENCHANTED_BOOK, 6000, 600, "misc"));

        // Fireworks
        misc.items.add(new ShopItem(Material.FIREWORK_ROCKET, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.FIREWORK_STAR, 1200, 120, "misc"));

        // Goat Horn
        misc.items.add(new ShopItem(Material.GOAT_HORN, 5000, 500, "misc"));

        // Music Discs
        misc.items.add(new ShopItem(Material.MUSIC_DISC_13, 12000, 1200, "misc"));
        misc.items.add(new ShopItem(Material.MUSIC_DISC_CAT, 12000, 1200, "misc"));
        misc.items.add(new ShopItem(Material.MUSIC_DISC_BLOCKS, 12000, 1200, "misc"));
        misc.items.add(new ShopItem(Material.MUSIC_DISC_CHIRP, 12000, 1200, "misc"));
        misc.items.add(new ShopItem(Material.MUSIC_DISC_FAR, 12000, 1200, "misc"));
        misc.items.add(new ShopItem(Material.MUSIC_DISC_MALL, 12000, 1200, "misc"));
        misc.items.add(new ShopItem(Material.MUSIC_DISC_MELLOHI, 12000, 1200, "misc"));
        misc.items.add(new ShopItem(Material.MUSIC_DISC_STAL, 12000, 1200, "misc"));
        misc.items.add(new ShopItem(Material.MUSIC_DISC_STRAD, 12000, 1200, "misc"));
        misc.items.add(new ShopItem(Material.MUSIC_DISC_WARD, 12000, 1200, "misc"));
        misc.items.add(new ShopItem(Material.MUSIC_DISC_11, 15000, 1500, "misc"));
        misc.items.add(new ShopItem(Material.MUSIC_DISC_WAIT, 12000, 1200, "misc"));
        misc.items.add(new ShopItem(Material.MUSIC_DISC_OTHERSIDE, 20000, 2000, "misc"));
        misc.items.add(new ShopItem(Material.MUSIC_DISC_5, 25000, 2500, "misc"));
        misc.items.add(new ShopItem(Material.MUSIC_DISC_PIGSTEP, 30000, 3000, "misc"));
        misc.items.add(new ShopItem(Material.MUSIC_DISC_RELIC, 35000, 3500, "misc"));

        // Redstone
        misc.items.add(new ShopItem(Material.REDSTONE_TORCH, 250, 25, "misc"));
        misc.items.add(new ShopItem(Material.REPEATER, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.COMPARATOR, 900, 90, "misc"));
        misc.items.add(new ShopItem(Material.OBSERVER, 1800, 180, "misc"));
        misc.items.add(new ShopItem(Material.PISTON, 3000, 300, "misc"));
        misc.items.add(new ShopItem(Material.STICKY_PISTON, 4500, 450, "misc"));
        misc.items.add(new ShopItem(Material.SLIME_BLOCK, 5000, 500, "misc"));
        misc.items.add(new ShopItem(Material.HONEY_BLOCK, 5000, 500, "misc"));
        misc.items.add(new ShopItem(Material.DISPENSER, 3000, 300, "misc"));
        misc.items.add(new ShopItem(Material.DROPPER, 1800, 180, "misc"));
        misc.items.add(new ShopItem(Material.HOPPER, 6000, 600, "misc"));
        misc.items.add(new ShopItem(Material.TARGET, 3500, 350, "misc"));
        misc.items.add(new ShopItem(Material.DAYLIGHT_DETECTOR, 2500, 250, "misc"));
        misc.items.add(new ShopItem(Material.TRIPWIRE_HOOK, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.TNT, 6000, 600, "misc"));
        misc.items.add(new ShopItem(Material.LEVER, 250, 25, "misc"));
        misc.items.add(new ShopItem(Material.STONE_BUTTON, 200, 20, "misc"));
        misc.items.add(new ShopItem(Material.STONE_PRESSURE_PLATE, 250, 25, "misc"));
        misc.items.add(new ShopItem(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, 1500, 150, "misc"));
        misc.items.add(new ShopItem(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, 1500, 150, "misc"));
        misc.items.add(new ShopItem(Material.LECTERN, 2500, 250, "misc"));
        misc.items.add(new ShopItem(Material.CRAFTER, 5000, 500, "misc"));

        // Workstations
        misc.items.add(new ShopItem(Material.CRAFTING_TABLE, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.FURNACE, 900, 90, "misc"));
        misc.items.add(new ShopItem(Material.BLAST_FURNACE, 3500, 350, "misc"));
        misc.items.add(new ShopItem(Material.SMOKER, 3000, 300, "misc"));
        misc.items.add(new ShopItem(Material.ANVIL, 9000, 900, "misc"));
        misc.items.add(new ShopItem(Material.CHIPPED_ANVIL, 6000, 600, "misc"));
        misc.items.add(new ShopItem(Material.DAMAGED_ANVIL, 3000, 300, "misc"));
        misc.items.add(new ShopItem(Material.ENCHANTING_TABLE, 18000, 1800, "misc"));
        misc.items.add(new ShopItem(Material.GRINDSTONE, 3500, 350, "misc"));
        misc.items.add(new ShopItem(Material.SMITHING_TABLE, 3500, 350, "misc"));
        misc.items.add(new ShopItem(Material.CARTOGRAPHY_TABLE, 2500, 250, "misc"));
        misc.items.add(new ShopItem(Material.FLETCHING_TABLE, 1800, 180, "misc"));
        misc.items.add(new ShopItem(Material.LOOM, 1800, 180, "misc"));
        misc.items.add(new ShopItem(Material.STONECUTTER, 3000, 300, "misc"));
        misc.items.add(new ShopItem(Material.COMPOSTER, 1200, 120, "misc"));

        // Storage
        misc.items.add(new ShopItem(Material.CHEST, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.TRAPPED_CHEST, 900, 90, "misc"));
        misc.items.add(new ShopItem(Material.BARREL, 900, 90, "misc"));
        misc.items.add(new ShopItem(Material.ENDER_CHEST, 12000, 1200, "misc"));
        misc.items.add(new ShopItem(Material.SHULKER_BOX, 28000, 2800, "misc"));

        // Building Utility
        misc.items.add(new ShopItem(Material.LADDER, 200, 20, "misc"));
        misc.items.add(new ShopItem(Material.SCAFFOLDING, 400, 40, "misc"));
        misc.items.add(new ShopItem(Material.IRON_BARS, 300, 30, "misc"));
        misc.items.add(new ShopItem(Material.IRON_DOOR, 1800, 180, "misc"));
        misc.items.add(new ShopItem(Material.IRON_TRAPDOOR, 1500, 150, "misc"));
        misc.items.add(new ShopItem(Material.HAY_BLOCK, 600, 60, "misc"));

        // Beds
        misc.items.add(new ShopItem(Material.WHITE_BED, 1000, 100, "misc"));
        misc.items.add(new ShopItem(Material.ORANGE_BED, 1000, 100, "misc"));
        misc.items.add(new ShopItem(Material.MAGENTA_BED, 1000, 100, "misc"));
        misc.items.add(new ShopItem(Material.LIGHT_BLUE_BED, 1000, 100, "misc"));
        misc.items.add(new ShopItem(Material.YELLOW_BED, 1000, 100, "misc"));
        misc.items.add(new ShopItem(Material.LIME_BED, 1000, 100, "misc"));
        misc.items.add(new ShopItem(Material.PINK_BED, 1000, 100, "misc"));
        misc.items.add(new ShopItem(Material.GRAY_BED, 1000, 100, "misc"));
        misc.items.add(new ShopItem(Material.LIGHT_GRAY_BED, 1000, 100, "misc"));
        misc.items.add(new ShopItem(Material.CYAN_BED, 1000, 100, "misc"));
        misc.items.add(new ShopItem(Material.PURPLE_BED, 1000, 100, "misc"));
        misc.items.add(new ShopItem(Material.BLUE_BED, 1000, 100, "misc"));
        misc.items.add(new ShopItem(Material.BROWN_BED, 1000, 100, "misc"));
        misc.items.add(new ShopItem(Material.GREEN_BED, 1000, 100, "misc"));
        misc.items.add(new ShopItem(Material.RED_BED, 1000, 100, "misc"));
        misc.items.add(new ShopItem(Material.BLACK_BED, 1000, 100, "misc"));

        // Banners
        misc.items.add(new ShopItem(Material.WHITE_BANNER, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.ORANGE_BANNER, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.MAGENTA_BANNER, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.LIGHT_BLUE_BANNER, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.YELLOW_BANNER, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.LIME_BANNER, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.PINK_BANNER, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.GRAY_BANNER, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.LIGHT_GRAY_BANNER, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.CYAN_BANNER, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.PURPLE_BANNER, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.BLUE_BANNER, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.BROWN_BANNER, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.GREEN_BANNER, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.RED_BANNER, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.BLACK_BANNER, 800, 80, "misc"));

        // Carpets
        misc.items.add(new ShopItem(Material.WHITE_CARPET, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.ORANGE_CARPET, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.MAGENTA_CARPET, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.LIGHT_BLUE_CARPET, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.YELLOW_CARPET, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.LIME_CARPET, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.PINK_CARPET, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.GRAY_CARPET, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.LIGHT_GRAY_CARPET, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.CYAN_CARPET, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.PURPLE_CARPET, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.BLUE_CARPET, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.BROWN_CARPET, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.GREEN_CARPET, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.RED_CARPET, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.BLACK_CARPET, 150, 15, "misc"));

        // Signs
        misc.items.add(new ShopItem(Material.OAK_SIGN, 400, 40, "misc"));
        misc.items.add(new ShopItem(Material.SPRUCE_SIGN, 400, 40, "misc"));
        misc.items.add(new ShopItem(Material.BIRCH_SIGN, 400, 40, "misc"));
        misc.items.add(new ShopItem(Material.JUNGLE_SIGN, 400, 40, "misc"));
        misc.items.add(new ShopItem(Material.ACACIA_SIGN, 400, 40, "misc"));
        misc.items.add(new ShopItem(Material.DARK_OAK_SIGN, 400, 40, "misc"));
        misc.items.add(new ShopItem(Material.MANGROVE_SIGN, 500, 50, "misc"));
        misc.items.add(new ShopItem(Material.CHERRY_SIGN, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.BAMBOO_SIGN, 500, 50, "misc"));
        misc.items.add(new ShopItem(Material.CRIMSON_SIGN, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.WARPED_SIGN, 600, 60, "misc"));

        // Hanging Signs
        misc.items.add(new ShopItem(Material.OAK_HANGING_SIGN, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.SPRUCE_HANGING_SIGN, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.BIRCH_HANGING_SIGN, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.JUNGLE_HANGING_SIGN, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.ACACIA_HANGING_SIGN, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.DARK_OAK_HANGING_SIGN, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.MANGROVE_HANGING_SIGN, 700, 70, "misc"));
        misc.items.add(new ShopItem(Material.CHERRY_HANGING_SIGN, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.BAMBOO_HANGING_SIGN, 700, 70, "misc"));
        misc.items.add(new ShopItem(Material.CRIMSON_HANGING_SIGN, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.WARPED_HANGING_SIGN, 800, 80, "misc"));

        // Doors & Trapdoors
        misc.items.add(new ShopItem(Material.OAK_DOOR, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.SPRUCE_DOOR, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.BIRCH_DOOR, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.JUNGLE_DOOR, 700, 70, "misc"));
        misc.items.add(new ShopItem(Material.ACACIA_DOOR, 700, 70, "misc"));
        misc.items.add(new ShopItem(Material.DARK_OAK_DOOR, 700, 70, "misc"));
        misc.items.add(new ShopItem(Material.MANGROVE_DOOR, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.CHERRY_DOOR, 900, 90, "misc"));
        misc.items.add(new ShopItem(Material.BAMBOO_DOOR, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.CRIMSON_DOOR, 900, 90, "misc"));
        misc.items.add(new ShopItem(Material.WARPED_DOOR, 900, 90, "misc"));
        misc.items.add(new ShopItem(Material.OAK_TRAPDOOR, 500, 50, "misc"));
        misc.items.add(new ShopItem(Material.SPRUCE_TRAPDOOR, 500, 50, "misc"));
        misc.items.add(new ShopItem(Material.BIRCH_TRAPDOOR, 500, 50, "misc"));
        misc.items.add(new ShopItem(Material.JUNGLE_TRAPDOOR, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.ACACIA_TRAPDOOR, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.DARK_OAK_TRAPDOOR, 600, 60, "misc"));
        misc.items.add(new ShopItem(Material.MANGROVE_TRAPDOOR, 700, 70, "misc"));
        misc.items.add(new ShopItem(Material.CHERRY_TRAPDOOR, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.BAMBOO_TRAPDOOR, 700, 70, "misc"));
        misc.items.add(new ShopItem(Material.CRIMSON_TRAPDOOR, 800, 80, "misc"));
        misc.items.add(new ShopItem(Material.WARPED_TRAPDOOR, 800, 80, "misc"));

        // Fences & Gates
        misc.items.add(new ShopItem(Material.OAK_FENCE, 250, 25, "misc"));
        misc.items.add(new ShopItem(Material.SPRUCE_FENCE, 250, 25, "misc"));
        misc.items.add(new ShopItem(Material.BIRCH_FENCE, 250, 25, "misc"));
        misc.items.add(new ShopItem(Material.JUNGLE_FENCE, 300, 30, "misc"));
        misc.items.add(new ShopItem(Material.ACACIA_FENCE, 300, 30, "misc"));
        misc.items.add(new ShopItem(Material.DARK_OAK_FENCE, 300, 30, "misc"));
        misc.items.add(new ShopItem(Material.MANGROVE_FENCE, 350, 35, "misc"));
        misc.items.add(new ShopItem(Material.CHERRY_FENCE, 400, 40, "misc"));
        misc.items.add(new ShopItem(Material.BAMBOO_FENCE, 350, 35, "misc"));
        misc.items.add(new ShopItem(Material.CRIMSON_FENCE, 400, 40, "misc"));
        misc.items.add(new ShopItem(Material.WARPED_FENCE, 400, 40, "misc"));
        misc.items.add(new ShopItem(Material.NETHER_BRICK_FENCE, 500, 50, "misc"));
        misc.items.add(new ShopItem(Material.OAK_FENCE_GATE, 350, 35, "misc"));
        misc.items.add(new ShopItem(Material.SPRUCE_FENCE_GATE, 350, 35, "misc"));
        misc.items.add(new ShopItem(Material.BIRCH_FENCE_GATE, 350, 35, "misc"));
        misc.items.add(new ShopItem(Material.JUNGLE_FENCE_GATE, 400, 40, "misc"));
        misc.items.add(new ShopItem(Material.ACACIA_FENCE_GATE, 400, 40, "misc"));
        misc.items.add(new ShopItem(Material.DARK_OAK_FENCE_GATE, 400, 40, "misc"));
        misc.items.add(new ShopItem(Material.MANGROVE_FENCE_GATE, 450, 45, "misc"));
        misc.items.add(new ShopItem(Material.CHERRY_FENCE_GATE, 500, 50, "misc"));
        misc.items.add(new ShopItem(Material.BAMBOO_FENCE_GATE, 450, 45, "misc"));
        misc.items.add(new ShopItem(Material.CRIMSON_FENCE_GATE, 500, 50, "misc"));
        misc.items.add(new ShopItem(Material.WARPED_FENCE_GATE, 500, 50, "misc"));

        // Planks
        misc.items.add(new ShopItem(Material.OAK_PLANKS, 75, 7, "misc"));
        misc.items.add(new ShopItem(Material.SPRUCE_PLANKS, 75, 7, "misc"));
        misc.items.add(new ShopItem(Material.BIRCH_PLANKS, 75, 7, "misc"));
        misc.items.add(new ShopItem(Material.JUNGLE_PLANKS, 100, 10, "misc"));
        misc.items.add(new ShopItem(Material.ACACIA_PLANKS, 100, 10, "misc"));
        misc.items.add(new ShopItem(Material.DARK_OAK_PLANKS, 100, 10, "misc"));
        misc.items.add(new ShopItem(Material.MANGROVE_PLANKS, 125, 12, "misc"));
        misc.items.add(new ShopItem(Material.CHERRY_PLANKS, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.BAMBOO_PLANKS, 125, 12, "misc"));
        misc.items.add(new ShopItem(Material.CRIMSON_PLANKS, 200, 20, "misc"));
        misc.items.add(new ShopItem(Material.WARPED_PLANKS, 200, 20, "misc"));

        // Wood Stairs & Slabs
        misc.items.add(new ShopItem(Material.OAK_STAIRS, 125, 12, "misc"));
        misc.items.add(new ShopItem(Material.SPRUCE_STAIRS, 125, 12, "misc"));
        misc.items.add(new ShopItem(Material.BIRCH_STAIRS, 125, 12, "misc"));
        misc.items.add(new ShopItem(Material.JUNGLE_STAIRS, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.ACACIA_STAIRS, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.DARK_OAK_STAIRS, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.MANGROVE_STAIRS, 175, 17, "misc"));
        misc.items.add(new ShopItem(Material.CHERRY_STAIRS, 200, 20, "misc"));
        misc.items.add(new ShopItem(Material.BAMBOO_STAIRS, 175, 17, "misc"));
        misc.items.add(new ShopItem(Material.BAMBOO_MOSAIC_STAIRS, 200, 20, "misc"));
        misc.items.add(new ShopItem(Material.CRIMSON_STAIRS, 250, 25, "misc"));
        misc.items.add(new ShopItem(Material.WARPED_STAIRS, 250, 25, "misc"));
        misc.items.add(new ShopItem(Material.OAK_SLAB, 60, 6, "misc"));
        misc.items.add(new ShopItem(Material.SPRUCE_SLAB, 60, 6, "misc"));
        misc.items.add(new ShopItem(Material.BIRCH_SLAB, 60, 6, "misc"));
        misc.items.add(new ShopItem(Material.JUNGLE_SLAB, 75, 7, "misc"));
        misc.items.add(new ShopItem(Material.ACACIA_SLAB, 75, 7, "misc"));
        misc.items.add(new ShopItem(Material.DARK_OAK_SLAB, 75, 7, "misc"));
        misc.items.add(new ShopItem(Material.MANGROVE_SLAB, 100, 10, "misc"));
        misc.items.add(new ShopItem(Material.CHERRY_SLAB, 125, 12, "misc"));
        misc.items.add(new ShopItem(Material.BAMBOO_SLAB, 100, 10, "misc"));
        misc.items.add(new ShopItem(Material.BAMBOO_MOSAIC_SLAB, 125, 12, "misc"));
        misc.items.add(new ShopItem(Material.CRIMSON_SLAB, 150, 15, "misc"));
        misc.items.add(new ShopItem(Material.WARPED_SLAB, 150, 15, "misc"));

        categories.put("misc", misc);

        // ══════════════════════════════════════
        //  CATEGORY 7: EXCLUSIVE
        // ══════════════════════════════════════
        Category exclusive = new Category("exclusive", "§5§lExclusive", "§5", Material.NETHER_STAR);

        exclusive.items.add(new ShopItem(Material.ELYTRA, 150000, 125000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.GOLDEN_APPLE, 10000, 7500, "exclusive"));
        exclusive.items.add(new ShopItem(Material.ENCHANTED_GOLDEN_APPLE, 85000, 70000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.ENDER_PEARL, 3500, 2500, "exclusive"));
        exclusive.items.add(new ShopItem(Material.TOTEM_OF_UNDYING, 45000, 35000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.NETHER_STAR, 90000, 75000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.HEART_OF_THE_SEA, 40000, 30000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.SHULKER_SHELL, 18000, 14000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.WITHER_SKELETON_SKULL, 30000, 22000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.DRAGON_BREATH, 12000, 8500, "exclusive"));
        exclusive.items.add(new ShopItem(Material.NETHERITE_SCRAP, 28000, 22000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.NETHERITE_INGOT, 110000, 95000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.NETHERITE_BLOCK, 950000, 820000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.DIAMOND_BLOCK, 85000, 70000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.EMERALD_BLOCK, 70000, 55000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.OMINOUS_TRIAL_KEY, 35000, 25000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.TRIAL_KEY, 18000, 13000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.MACE, 250000, 210000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.HEAVY_CORE, 120000, 95000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.BREEZE_ROD, 15000, 11000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.BEACON, 200000, 170000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.END_CRYSTAL, 25000, 18000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.CONDUIT, 80000, 65000, "exclusive"));
        exclusive.items.add(new ShopItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 50000, 40000, "exclusive"));

        categories.put("exclusive", exclusive);

       Category spawners = new Category("spawners", "§4§lSpawners", "§4", Material.SPAWNER);

       spawners.items.add(new SpawnerShopItem(EntityType.SKELETON,    150000));
       spawners.items.add(new SpawnerShopItem(EntityType.ZOMBIE,      300000));
       spawners.items.add(new SpawnerShopItem(EntityType.SPIDER,      250000));
       spawners.items.add(new SpawnerShopItem(EntityType.CAVE_SPIDER, 350000));
       spawners.items.add(new SpawnerShopItem(EntityType.SLIME,       650000));
       spawners.items.add(new SpawnerShopItem(EntityType.BLAZE,       900000));
       spawners.items.add(new SpawnerShopItem(EntityType.MAGMA_CUBE,  1000000));
       spawners.items.add(new SpawnerShopItem(EntityType.CREEPER,     1250000));
       spawners.items.add(new SpawnerShopItem(EntityType.ENDERMAN,    4000000));
       spawners.items.add(new SpawnerShopItem(EntityType.WITCH,       2500000));

                categories.put("spawners", spawners);

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
