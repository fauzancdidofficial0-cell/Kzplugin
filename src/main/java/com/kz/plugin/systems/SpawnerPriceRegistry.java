package com.kz.plugin.systems;

import org.bukkit.Material;
import java.util.*;

public final class SpawnerPriceRegistry {

    private static final Map<Material, Integer> SPAWNER_SELL_PRICES = new EnumMap<>(Material.class);

    static {
        SPAWNER_SELL_PRICES.put(Material.ROTTEN_FLESH, 4);
        SPAWNER_SELL_PRICES.put(Material.BONE, 11);
        SPAWNER_SELL_PRICES.put(Material.ARROW, 11);
        SPAWNER_SELL_PRICES.put(Material.STRING, 15);
        SPAWNER_SELL_PRICES.put(Material.SPIDER_EYE, 15);
        SPAWNER_SELL_PRICES.put(Material.GUNPOWDER, 23);
        SPAWNER_SELL_PRICES.put(Material.FEATHER, 8);
        SPAWNER_SELL_PRICES.put(Material.SLIME_BALL, 35);
        SPAWNER_SELL_PRICES.put(Material.MAGMA_CREAM, 65);
        SPAWNER_SELL_PRICES.put(Material.BLAZE_ROD, 70);
        SPAWNER_SELL_PRICES.put(Material.BLAZE_POWDER, 50);
        SPAWNER_SELL_PRICES.put(Material.GHAST_TEAR, 225);
        SPAWNER_SELL_PRICES.put(Material.PHANTOM_MEMBRANE, 150);
        SPAWNER_SELL_PRICES.put(Material.GLOW_INK_SAC, 38);
        SPAWNER_SELL_PRICES.put(Material.INK_SAC, 15);
        SPAWNER_SELL_PRICES.put(Material.PRISMARINE_SHARD, 30);
        SPAWNER_SELL_PRICES.put(Material.PRISMARINE_CRYSTALS, 38);
        SPAWNER_SELL_PRICES.put(Material.RABBIT_HIDE, 15);
        SPAWNER_SELL_PRICES.put(Material.RABBIT_FOOT, 75);
        SPAWNER_SELL_PRICES.put(Material.SCUTE, 150);
        SPAWNER_SELL_PRICES.put(Material.ENDER_PEARL, 600);
        SPAWNER_SELL_PRICES.put(Material.EXPERIENCE_BOTTLE, 100);
    }

    private SpawnerPriceRegistry() {}

    public static boolean isSpawnerSellable(Material material) {
        return SPAWNER_SELL_PRICES.containsKey(material);
    }

    public static int getSpawnerSellPrice(Material material) {
        return SPAWNER_SELL_PRICES.getOrDefault(material, 0);
    }

    public static Map<Material, Integer> getAllSpawnerSellPrices() {
        return Collections.unmodifiableMap(SPAWNER_SELL_PRICES);
    }
}
