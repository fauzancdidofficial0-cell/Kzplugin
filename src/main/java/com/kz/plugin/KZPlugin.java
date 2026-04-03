// ============================================================
// PATH: src/main/java/com/kz/plugin/KZPlugin.java
// ============================================================
package com.kz.plugin;

import com.kz.plugin.commands.*;
import com.kz.plugin.listeners.*;
import com.kz.plugin.systems.*;
import com.kz.plugin.data.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class KZPlugin extends JavaPlugin {

    private static KZPlugin instance;

    // ══════════════════════════════════
    //  System Instances
    // ══════════════════════════════════
    private DatabaseManager databaseManager;
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
    private SpawnerItemFactory spawnerItemFactory;
    private CrateSystem crateSystem;
    private BedrockFormManager bedrockFormManager;
    private ProxyMessageListener proxyMessageListener;
    private QuizSystem quizSystem;
    private AntiSpamSystem antiSpamSystem;
    private CommandBlocker commandBlocker;

    // ══════════════════════════════════
    //  ENABLE
    // ══════════════════════════════════

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        getLogger().info("  KZ PLUGIN - Initializing...");
        getLogger().info("  Server: " + getConfig().getString("server-name", "unknown"));
        getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // ══════════════════════════════════
        //  1. Database Connection
        // ══════════════════════════════════
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.connect()) {
            getLogger().severe("[KZ] Database connection failed! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // ══════════════════════════════════
        //  2. Register BungeeCord Channel
        // ══════════════════════════════════
        proxyMessageListener = new ProxyMessageListener(this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord",
                proxyMessageListener);
        getLogger().info("[KZ] BungeeCord plugin messaging channel registered.");

        // ══════════════════════════════════
        //  3. Initialize Systems
        // ══════════════════════════════════
        itemDatabase = new ItemDatabase();
        economyManager = new EconomyManager(this, databaseManager);
        islandSystem = new IslandSystem(this);
        oneBlockSystem = new OneBlockSystem(this);
        landSystem = new LandSystem(this);
        jobSystem = new JobSystem(this);
        tpaSystem = new TPASystem(this);
        lobbySystem = new LobbySystem(this);
        dailyRewardSystem = new DailyRewardSystem(this);
        weeklyRewardSystem = new WeeklyRewardSystem(this);
        spawnerItemFactory = new SpawnerItemFactory(this);
        crateSystem = new CrateSystem(this);
        bedrockFormManager = new BedrockFormManager(this);
        quizSystem = new QuizSystem(this);
        antiSpamSystem = new AntiSpamSystem(this);
        commandBlocker = new CommandBlocker(this);   // ← LAST (needs EconomyManager)

        // ══════════════════════════════════
        //  4. Register Commands
        // ══════════════════════════════════
        registerCommands();

        // ══════════════════════════════════
        //  5. Register Listeners
        // ══════════════════════════════════
        registerListeners();

        // ══════════════════════════════════
        //  6. Start Scheduled Tasks
        // ══════════════════════════════════
        startTasks();

        getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        getLogger().info("  KZ PLUGIN v2.0.0 - Enabled ✔");
        getLogger().info("  Mode     : Multi-Server (Velocity)");
        getLogger().info("  Server   : " + getConfig().getString("server-name", "unknown"));
        getLogger().info("  Database : Connected (" + databaseManager.getPoolStats() + ")");
        getLogger().info("  Items    : " + itemDatabase.getTotalItems() + " registered");
        getLogger().info("  Crates   : " + crateSystem.getAllCrates().size() + " loaded");
        getLogger().info("  Bedrock  : " + (bedrockFormManager.isFloodgateAvailable() ? "Forms" : "Fallback"));
        getLogger().info("  Quiz     : Auto/" + getConfig().getInt("quiz.auto-interval-minutes", 15) + "min");
        getLogger().info("  AntiSpam : Active");
        getLogger().info("  CmdBlock : " + (commandBlocker.isEnabled() ? "Active" : "Disabled"));
        getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ══════════════════════════════════
    //  DISABLE
    // ══════════════════════════════════

    @Override
    public void onDisable() {
        if (economyManager != null) economyManager.shutdown();
        if (crateSystem != null) crateSystem.shutdown();
        if (quizSystem != null) quizSystem.shutdown();
        if (antiSpamSystem != null) antiSpamSystem.shutdown();
        if (islandSystem != null) islandSystem.saveAll();
        if (landSystem != null) landSystem.saveAll();
        if (lobbySystem != null) lobbySystem.saveData();
        if (dailyRewardSystem != null) dailyRewardSystem.saveData();
        if (weeklyRewardSystem != null) weeklyRewardSystem.saveData();

        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);

        if (databaseManager != null) databaseManager.disconnect();

        getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        getLogger().info("  KZ PLUGIN v2.0.0 - Disabled");
        getLogger().info("  All data saved. Database closed.");
        getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ══════════════════════════════════
    //  REGISTER COMMANDS
    // ══════════════════════════════════

    private void registerCommands() {
        // ─── Shop & Economy ───
        ShopCommand shopCmd = new ShopCommand(this);
        SellCommand sellCmd = new SellCommand(this);
        AuctionCommand ahCmd = new AuctionCommand(this);
        EconomyCommand ecoCmd = new EconomyCommand(this);

        setCmd("shop", shopCmd);
        setCmd("sell", sellCmd);
        setCmd("ah", ahCmd);
        setCmd("inbox", ahCmd);
        setCmd("bal", ecoCmd);
        setCmd("pay", ecoCmd);
        setCmd("baltop", ecoCmd);
        setCmd("cf", ecoCmd);

        // ─── Island ───
        IslandCommand islandCmd = new IslandCommand(this);
        setCmd("createisland", islandCmd);
        setCmd("deleteisland", islandCmd);
        setCmd("home", islandCmd);
        setCmd("upisland", islandCmd);
        setCmd("islandsetting", islandCmd);
        setCmd("nameisland", islandCmd);
        setCmd("visit", islandCmd);
        setCmd("topisland", islandCmd);
        setCmd("invite", islandCmd);
        setCmd("accept", islandCmd);
        setCmd("deny", islandCmd);
        setCmd("trust", islandCmd);
        setCmd("untrust", islandCmd);

        // ─── TPA ───
        TPACommand tpaCmd = new TPACommand(this);
        setCmd("tpa", tpaCmd);
        setCmd("tpaccept", tpaCmd);
        setCmd("tpadeny", tpaCmd);
        setCmd("tpcancel", tpaCmd);

        // ─── Land ───
        LandCommand landCmd = new LandCommand(this);
        setCmd("landinvite", landCmd);
        setCmd("landaccept", landCmd);
        setCmd("landdeny", landCmd);
        setCmd("landrole", landCmd);
        setCmd("landkick", landCmd);
        setCmd("trustland", landCmd);
        setCmd("memberrule", landCmd);
        setCmd("trustrule", landCmd);
        setCmd("setlandname", landCmd);
        setCmd("deleteland", landCmd);
        setCmd("cekcapasitas", landCmd);

        // ─── Job & Reward ───
        setCmd("job", new JobCommand(this));
        setCmd("daily", new DailyCommand(this));
        setCmd("weekly", new WeeklyCommand(this));

        // ─── Lobby & Info ───
        LobbyCommand lobbyCmd = new LobbyCommand(this);
        setCmd("lobby", lobbyCmd);
        setCmd("hub", lobbyCmd);
        setCmd("spawn", lobbyCmd);
        setCmd("help", lobbyCmd);
        setCmd("stats", lobbyCmd);
        setCmd("rank", lobbyCmd);
        setCmd("discord", lobbyCmd);
        setCmd("website", lobbyCmd);
        setCmd("rules", lobbyCmd);

        // ─── Menu (Bedrock + Java) ───
        setCmd("menu", new MenuCommand(this));
        setCmd("givekey", crateCmd);

        // ─── Admin ───
        AdminCommand adminCmd = new AdminCommand(this);
        setCmd("setlobby", adminCmd);
        setCmd("setspawn", adminCmd);
        setCmd("createnpc", adminCmd);
        setCmd("removenpc", adminCmd);
        setCmd("listnpc", adminCmd);
        setCmd("givebal", adminCmd);
        setCmd("removebal", adminCmd);
        setCmd("setrank", adminCmd);
        setCmd("maintenance", adminCmd);
        setCmd("announce", adminCmd);

        // ─── Crate / Gacha ───
        CrateCommand crateCmd = new CrateCommand(this);
        setCmd("gachacreate", crateCmd);
        setCmd("gachadelete", crateCmd);
        setCmd("gachalist", crateCmd);
        setCmd("gachapreview", crateCmd);

        // ─── Quiz ───
        setCmd("quiz", new QuizCommand(this));
    }

    private void setCmd(String name, Object executor) {
        var cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor((org.bukkit.command.CommandExecutor) executor);
            if (executor instanceof org.bukkit.command.TabCompleter tabCompleter) {
                cmd.setTabCompleter(tabCompleter);
            }
        } else {
            getLogger().warning("[KZ] Command '/" + name + "' not found in plugin.yml!");
        }
    }

    // ══════════════════════════════════
    //  REGISTER LISTENERS
    // ══════════════════════════════════

    private void registerListeners() {
        var pm = getServer().getPluginManager();

        pm.registerEvents(new GUIListener(this), this);
        pm.registerEvents(new PlayerEventListener(this), this);
        pm.registerEvents(new BlockEventListener(this), this);
        pm.registerEvents(new EntityEventListener(this), this);
        pm.registerEvents(new SpawnerPlaceListener(this), this);
        pm.registerEvents(new SpawnerDropListener(this), this);
        pm.registerEvents(new CrateListener(this), this);

        // NOTE: QuizSystem, AntiSpamSystem, CommandBlocker
        // register themselves as listeners in constructors
    }

    // ══════════════════════════════════
    //  START SCHEDULED TASKS
    // ══════════════════════════════════

    private void startTasks() {
        // Scoreboard + Nametag - every 3 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (lobbySystem != null) {
                    lobbySystem.updateAllScoreboards();
                    lobbySystem.updateAllNametags();
                }
            }
        }.runTaskTimer(this, 60L, 60L);

        // Playtime - every 1 minute
        new BukkitRunnable() {
            @Override
            public void run() {
                if (lobbySystem != null) lobbySystem.trackPlaytime();
            }
        }.runTaskTimer(this, 1200L, 1200L);

        // ClearLag - every 10 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                if (lobbySystem != null) lobbySystem.clearLag();
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
                if (islandSystem != null) islandSystem.saveAll();
                if (landSystem != null) landSystem.saveAll();
                if (lobbySystem != null) lobbySystem.saveData();
                if (dailyRewardSystem != null) dailyRewardSystem.saveData();
                if (weeklyRewardSystem != null) weeklyRewardSystem.saveData();
                if (crateSystem != null) crateSystem.saveData();
                getLogger().info("[KZ] Auto-save completed.");
            }
        }.runTaskTimer(this, 6000L, 6000L);

        // Lobby fireworks - every 3 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                if (lobbySystem != null) lobbySystem.spawnFireworks();
            }
        }.runTaskTimer(this, 3600L, 3600L);

        // Request server name from proxy
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!Bukkit.getOnlinePlayers().isEmpty() && proxyMessageListener != null
                        && proxyMessageListener.getCachedServerName() == null) {
                    var any = Bukkit.getOnlinePlayers().iterator().next();
                    proxyMessageListener.requestServerName(any, name ->
                            getLogger().info("[KZ] Proxy confirmed: " + name));
                    this.cancel();
                }
            }
        }.runTaskTimer(this, 100L, 200L);
    }

    // ══════════════════════════════════
    //  GETTERS
    // ══════════════════════════════════

    public static KZPlugin getInstance() { return instance; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public IslandSystem getIslandSystem() { return islandSystem; }
    public OneBlockSystem getOneBlockSystem() { return oneBlockSystem; }
    public LandSystem getLandSystem() { return landSystem; }
    public JobSystem getJobSystem() { return jobSystem; }
    public TPASystem getTpaSystem() { return tpaSystem; }
    public LobbySystem getLobbySystem() { return lobbySystem; }
    public ItemDatabase getItemDatabase() { return itemDatabase; }
    public DailyRewardSystem getDailyRewardSystem() { return dailyRewardSystem; }
    public WeeklyRewardSystem getWeeklyRewardSystem() { return weeklyRewardSystem; }
    public SpawnerItemFactory getSpawnerItemFactory() { return spawnerItemFactory; }
    public CrateSystem getCrateSystem() { return crateSystem; }
    public BedrockFormManager getBedrockFormManager() { return bedrockFormManager; }
    public ProxyMessageListener getProxyMessageListener() { return proxyMessageListener; }
    public QuizSystem getQuizSystem() { return quizSystem; }
    public AntiSpamSystem getAntiSpamSystem() { return antiSpamSystem; }
    public CommandBlocker getCommandBlocker() { return commandBlocker; }
}
