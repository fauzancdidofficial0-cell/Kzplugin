package com.kz.plugin;

import com.kz.plugin.commands.*;
import com.kz.plugin.listeners.*;
import com.kz.plugin.systems.*;
import com.kz.plugin.data.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class KZPlugin extends JavaPlugin {

    private static KZPlugin instance;
    private EconomyManager economyManager;
    private IslandSystem islandSystem;
    private OneBlockSystem oneBlockSystem;
    private LandSystem landSystem;
    private JobSystem jobSystem;
    private TPASystem tpaSystem;
    private LobbySystem lobbySystem;
    private ItemDatabase itemDatabase;
    private DailyRewardSystem dailyRewardSystem;
    private WeeklyRewardSystem weeklyRewardSystem;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        getLogger().info("  KZ PLUGIN - Initializing...");
        getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // ══════════════════════════════════
        //  Initialize Core Systems
        // ══════════════════════════════════
        itemDatabase = new ItemDatabase();
        economyManager = new EconomyManager(this);
        islandSystem = new IslandSystem(this);
        oneBlockSystem = new OneBlockSystem(this);
        landSystem = new LandSystem(this);
        jobSystem = new JobSystem(this);
        tpaSystem = new TPASystem(this);
        lobbySystem = new LobbySystem(this);
        dailyRewardSystem = new DailyRewardSystem(this);
        weeklyRewardSystem = new WeeklyRewardSystem(this);

        // ══════════════════════════════════
        //  Register Commands
        // ══════════════════════════════════
        registerCommands();

        // ══════════════════════════════════
        //  Register Listeners
        // ══════════════════════════════════
        registerListeners();

        // ══════════════════════════════════
        //  Start Scheduled Tasks
        // ══════════════════════════════════
        startTasks();

        getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        getLogger().info("  KZ PLUGIN v1.0.0 - Enabled");
        getLogger().info("  Systems : All operational");
        getLogger().info("  Items   : " + itemDatabase.getTotalItems() + " registered");
        getLogger().info("  Author  : KZ Team");
        getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public void onDisable() {
        if (economyManager != null) economyManager.saveAll();
        if (islandSystem != null) islandSystem.saveAll();
        if (landSystem != null) landSystem.saveAll();
        if (lobbySystem != null) lobbySystem.saveData();
        if (dailyRewardSystem != null) dailyRewardSystem.saveData();
        if (weeklyRewardSystem != null) weeklyRewardSystem.saveData();

        getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        getLogger().info("  KZ PLUGIN v1.0.0 - Disabled");
        getLogger().info("  All data saved successfully.");
        getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void registerCommands() {
        // Shop & Economy
        ShopCommand shopCmd = new ShopCommand(this);
        SellCommand sellCmd = new SellCommand(this);
        AuctionCommand ahCmd = new AuctionCommand(this);
        EconomyCommand ecoCmd = new EconomyCommand(this);

        getCommand("shop").setExecutor(shopCmd);
        getCommand("sell").setExecutor(sellCmd);
        getCommand("ah").setExecutor(ahCmd);
        getCommand("inbox").setExecutor(ahCmd);
        getCommand("bal").setExecutor(ecoCmd);
        getCommand("pay").setExecutor(ecoCmd);
        getCommand("baltop").setExecutor(ecoCmd);
        getCommand("cf").setExecutor(ecoCmd);

        // Island System
        IslandCommand islandCmd = new IslandCommand(this);
        getCommand("createisland").setExecutor(islandCmd);
        getCommand("deleteisland").setExecutor(islandCmd);
        getCommand("home").setExecutor(islandCmd);
        getCommand("upisland").setExecutor(islandCmd);
        getCommand("islandsetting").setExecutor(islandCmd);
        getCommand("nameisland").setExecutor(islandCmd);
        getCommand("visit").setExecutor(islandCmd);
        getCommand("topisland").setExecutor(islandCmd);
        getCommand("invite").setExecutor(islandCmd);
        getCommand("accept").setExecutor(islandCmd);
        getCommand("deny").setExecutor(islandCmd);
        getCommand("trust").setExecutor(islandCmd);
        getCommand("untrust").setExecutor(islandCmd);

        // TPA System
        TPACommand tpaCmd = new TPACommand(this);
        getCommand("tpa").setExecutor(tpaCmd);
        getCommand("tpaccept").setExecutor(tpaCmd);
        getCommand("tpadeny").setExecutor(tpaCmd);
        getCommand("tpcancel").setExecutor(tpaCmd);

        // Land System
        LandCommand landCmd = new LandCommand(this);
        getCommand("landinvite").setExecutor(landCmd);
        getCommand("landaccept").setExecutor(landCmd);
        getCommand("landdeny").setExecutor(landCmd);
        getCommand("landrole").setExecutor(landCmd);
        getCommand("landkick").setExecutor(landCmd);
        getCommand("trustland").setExecutor(landCmd);
        getCommand("memberrule").setExecutor(landCmd);
        getCommand("trustrule").setExecutor(landCmd);
        getCommand("setlandname").setExecutor(landCmd);
        getCommand("deleteland").setExecutor(landCmd);
        getCommand("cekcapasitas").setExecutor(landCmd);

        // Job & Reward
        JobCommand jobCmd = new JobCommand(this);
        DailyCommand dailyCmd = new DailyCommand(this);
        WeeklyCommand weeklyCmd = new WeeklyCommand(this);
        getCommand("job").setExecutor(jobCmd);
        getCommand("daily").setExecutor(dailyCmd);
        getCommand("weekly").setExecutor(weeklyCmd);

        // Lobby & Info
        LobbyCommand lobbyCmd = new LobbyCommand(this);
        getCommand("lobby").setExecutor(lobbyCmd);
        getCommand("spawn").setExecutor(lobbyCmd);
        getCommand("help").setExecutor(lobbyCmd);
        getCommand("stats").setExecutor(lobbyCmd);
        getCommand("rank").setExecutor(lobbyCmd);
        getCommand("discord").setExecutor(lobbyCmd);
        getCommand("website").setExecutor(lobbyCmd);
        getCommand("rules").setExecutor(lobbyCmd);

        // Admin
        AdminCommand adminCmd = new AdminCommand(this);
        getCommand("setlobby").setExecutor(adminCmd);
        getCommand("setspawn").setExecutor(adminCmd);
        getCommand("createnpc").setExecutor(adminCmd);
        getCommand("removenpc").setExecutor(adminCmd);
        getCommand("listnpc").setExecutor(adminCmd);
        getCommand("givebal").setExecutor(adminCmd);
        getCommand("removebal").setExecutor(adminCmd);
        getCommand("setrank").setExecutor(adminCmd);
        getCommand("maintenance").setExecutor(adminCmd);
        getCommand("announce").setExecutor(adminCmd);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
            new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(
            new PlayerEventListener(this), this);
        getServer().getPluginManager().registerEvents(
            new BlockEventListener(this), this);
        getServer().getPluginManager().registerEvents(
            new EntityEventListener(this), this);
    }

    private void startTasks() {
        // Scoreboard - every 1 second
        new BukkitRunnable() {
            @Override
            public void run() {
                lobbySystem.updateScoreboard();
            }
        }.runTaskTimer(this, 20L, 20L);

        // Playtime - every 1 minute
        new BukkitRunnable() {
            @Override
            public void run() {
                lobbySystem.trackPlaytime();
            }
        }.runTaskTimer(this, 1200L, 1200L);

        // ClearLag - every 10 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                lobbySystem.clearLag();
            }
        }.runTaskTimer(this, 12000L, 12000L);

        // Auction expire - every 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                AuctionCommand.checkExpired(getInstance());
            }
        }.runTaskTimer(this, 6000L, 6000L);

        // Auto-save - every 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                economyManager.saveAll();
                islandSystem.saveAll();
                landSystem.saveAll();
                lobbySystem.saveData();
                dailyRewardSystem.saveData();
                weeklyRewardSystem.saveData();
                getLogger().info("[KZ] Auto-save completed.");
            }
        }.runTaskTimer(this, 6000L, 6000L);

        // Fireworks - every 3 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                lobbySystem.spawnFireworks();
            }
        }.runTaskTimer(this, 3600L, 3600L);
    }

    // ══════════════════════════════════
    //  Getters
    // ══════════════════════════════════

    public static KZPlugin getInstance() {
        return instance;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public IslandSystem getIslandSystem() {
        return islandSystem;
    }

    public OneBlockSystem getOneBlockSystem() {
        return oneBlockSystem;
    }

    public LandSystem getLandSystem() {
        return landSystem;
    }

    public JobSystem getJobSystem() {
        return jobSystem;
    }

    public TPASystem getTpaSystem() {
        return tpaSystem;
    }

    public LobbySystem getLobbySystem() {
        return lobbySystem;
    }

    public ItemDatabase getItemDatabase() {
        return itemDatabase;
    }

    public DailyRewardSystem getDailyRewardSystem() {
        return dailyRewardSystem;
    }

    public WeeklyRewardSystem getWeeklyRewardSystem() {
        return weeklyRewardSystem;
    }
}
