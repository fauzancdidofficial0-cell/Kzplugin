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
    private DatabaseManager      databaseManager;
    private EconomyManager       economyManager;
    private IslandSystem         islandSystem;
    private OneBlockSystem       oneBlockSystem;
    private LandSystem           landSystem;
    private JobSystem            jobSystem;
    private TPASystem            tpaSystem;
    private LobbySystem          lobbySystem;
    private ItemDatabase         itemDatabase;
    private DailyRewardSystem    dailyRewardSystem;
    private WeeklyRewardSystem   weeklyRewardSystem;
    private SpawnerItemFactory   spawnerItemFactory;
    private CrateSystem          crateSystem;
    private BedrockFormManager   bedrockFormManager;
    private ProxyMessageListener proxyMessageListener;
    private QuizSystem           quizSystem;
    private AntiSpamSystem       antiSpamSystem;
    private CommandBlocker       commandBlocker;

    // ── Order System ──────────────────
    private AdvancedOrderSystem  orderSystem;
    private OrderGUI             orderGUI;

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
        itemDatabase      = new ItemDatabase();
        economyManager    = new EconomyManager(this, databaseManager);
        islandSystem      = new IslandSystem(this);
        oneBlockSystem    = new OneBlockSystem(this);
        landSystem        = new LandSystem(this);
        jobSystem         = new JobSystem(this);
        tpaSystem         = new TPASystem(this);
        lobbySystem       = new LobbySystem(this);
        dailyRewardSystem = new DailyRewardSystem(this);
        weeklyRewardSystem= new WeeklyRewardSystem(this);
        spawnerItemFactory= new SpawnerItemFactory(this);
        crateSystem       = new CrateSystem(this);
        bedrockFormManager= new BedrockFormManager(this);
        quizSystem        = new QuizSystem(this);
        antiSpamSystem    = new AntiSpamSystem(this);
        commandBlocker    = new CommandBlocker(this);

        // ── Order System (setelah economyManager & lobbySystem) ──
        orderSystem = new AdvancedOrderSystem(this);
        orderGUI    = new OrderGUI(this, orderSystem);

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

        // ══════════════════════════════════
        //  7. Startup Log
        // ══════════════════════════════════
        getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        getLogger().info("  KZ PLUGIN v2.0.0 - Enabled ✔");
        getLogger().info("  Mode     : Multi-Server (Velocity)");
        getLogger().info("  Server   : " + getConfig().getString("server-name", "unknown"));
        getLogger().info("  Database : Connected (" + databaseManager.getPoolStats() + ")");
        getLogger().info("  Items    : " + itemDatabase.getTotalItems() + " registered");
        getLogger().info("  Crates   : " + crateSystem.getAllCrates().size() + " loaded");
        getLogger().info("  Orders   : " + orderSystem.getTotalOrders() + " loaded");
        getLogger().info("  Bedrock  : " + (bedrockFormManager.isFloodgateAvailable()
                ? "Forms" : "Fallback"));
        getLogger().info("  Quiz     : Auto/"
                + getConfig().getInt("quiz.auto-interval-minutes", 15) + "min");
        getLogger().info("  AntiSpam : Active");
        getLogger().info("  CmdBlock : " + (commandBlocker.isEnabled() ? "Active" : "Disabled"));
        getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ══════════════════════════════════
    //  DISABLE
    // ══════════════════════════════════

    @Override
    public void onDisable() {
        // Save semua system
        if (economyManager    != null) economyManager.shutdown();
        if (crateSystem       != null) crateSystem.shutdown();
        if (quizSystem        != null) quizSystem.shutdown();
        if (antiSpamSystem    != null) antiSpamSystem.shutdown();
        if (islandSystem      != null) islandSystem.saveAll();
        if (landSystem        != null) landSystem.saveAll();
        if (lobbySystem       != null) lobbySystem.saveData();
        if (dailyRewardSystem != null) dailyRewardSystem.saveData();
        if (weeklyRewardSystem!= null) weeklyRewardSystem.saveData();

        // Save order system (sync karena server mau mati)
        if (orderSystem != null) orderSystem.saveDataSync();

        // Unregister channels
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);

        // Database terakhir
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

        // ─── Shop & Economy ───────────────────────────────────────
        ShopCommand    shopCmd = new ShopCommand(this);
        SellCommand    sellCmd = new SellCommand(this);
        AuctionCommand ahCmd   = new AuctionCommand(this);
        EconomyCommand ecoCmd  = new EconomyCommand(this);

        setCmd("shop",   shopCmd);
        setCmd("sell",   sellCmd);
        setCmd("ah",     ahCmd);
        setCmd("inbox",  ahCmd);
        setCmd("bal",    ecoCmd);
        setCmd("pay",    ecoCmd);
        setCmd("baltop", ecoCmd);
        setCmd("cf",     ecoCmd);

        // ─── Island ───────────────────────────────────────────────
        IslandCommand islandCmd = new IslandCommand(this);
        setCmd("createisland",  islandCmd);
        setCmd("deleteisland",  islandCmd);
        setCmd("home",          islandCmd);
        setCmd("upisland",      islandCmd);
        setCmd("islandsetting", islandCmd);
        setCmd("nameisland",    islandCmd);
        setCmd("visit",         islandCmd);
        setCmd("topisland",     islandCmd);
        setCmd("invite",        islandCmd);
        setCmd("accept",        islandCmd);
        setCmd("deny",          islandCmd);
        setCmd("trust",         islandCmd);
        setCmd("untrust",       islandCmd);

        // ─── TPA ──────────────────────────────────────────────────
        TPACommand tpaCmd = new TPACommand(this);
        setCmd("tpa",      tpaCmd);
        setCmd("tpaccept", tpaCmd);
        setCmd("tpadeny",  tpaCmd);
        setCmd("tpcancel", tpaCmd);

        // ─── Land ─────────────────────────────────────────────────
        LandCommand landCmd = new LandCommand(this);
        setCmd("landinvite",  landCmd);
        setCmd("landaccept",  landCmd);
        setCmd("landdeny",    landCmd);
        setCmd("landrole",    landCmd);
        setCmd("landkick",    landCmd);
        setCmd("trustland",   landCmd);
        setCmd("memberrule",  landCmd);
        setCmd("trustrule",   landCmd);
        setCmd("setlandname", landCmd);
        setCmd("deleteland",  landCmd);
        setCmd("cekcapasitas",landCmd);

        // ─── Job & Reward ─────────────────────────────────────────
        setCmd("job",    new JobCommand(this));
        setCmd("daily",  new DailyCommand(this));
        setCmd("weekly", new WeeklyCommand(this));

        // ─── Lobby & Info ─────────────────────────────────────────
        LobbyCommand lobbyCmd = new LobbyCommand(this);
        setCmd("lobby",   lobbyCmd);
        setCmd("hub",     lobbyCmd);
        setCmd("spawn",   lobbyCmd);
        setCmd("help",    lobbyCmd);
        setCmd("stats",   lobbyCmd);
        setCmd("rank",    lobbyCmd);
        setCmd("discord", lobbyCmd);
        setCmd("website", lobbyCmd);
        setCmd("rules",   lobbyCmd);
        setCmd("fly",     lobbyCmd);
        setCmd("vanish",  lobbyCmd);

        // ─── Crate / Gacha ────────────────────────────────────────
        CrateCommand crateCmd = new CrateCommand(this);
        setCmd("gachacreate",  crateCmd);
        setCmd("gachadelete",  crateCmd);
        setCmd("gachalist",    crateCmd);
        setCmd("gachapreview", crateCmd);
        setCmd("givekey",      crateCmd);

        // ─── Menu ─────────────────────────────────────────────────
        MenuCommand menuCmd = new MenuCommand(this);
        setCmd("menu",      menuCmd);
        setCmd("settppasar",menuCmd);

        // ─── Create Key ───────────────────────────────────────────
        setCmd("createkey", new CreateKeyCommand(this));

        // ─── Admin ────────────────────────────────────────────────
        AdminCommand adminCmd = new AdminCommand(this);
        setCmd("setlobby",    adminCmd);
        setCmd("setspawn",    adminCmd);
        setCmd("createnpc",   adminCmd);
        setCmd("removenpc",   adminCmd);
        setCmd("listnpc",     adminCmd);
        setCmd("givebal",     adminCmd);
        setCmd("removebal",   adminCmd);
        setCmd("setrank",     adminCmd);
        setCmd("maintenance", adminCmd);
        setCmd("announce",    adminCmd);
        setCmd("clearlag",    adminCmd);
        setCmd("reloadconfig",adminCmd);

        // ─── Quiz ─────────────────────────────────────────────────
        setCmd("quiz", new QuizCommand(this));

        // ─── Order System ─────────────────────────────────────────
        OrderCommand orderCmd = new OrderCommand(this, orderSystem, orderGUI);
        setCmd("order",    orderCmd);
        setCmd("myorders", orderCmd);
    }

    // ══════════════════════════════════
    //  REGISTER LISTENERS
    // ══════════════════════════════════

    private void registerListeners() {
        var pm = getServer().getPluginManager();

        // Core listeners
        pm.registerEvents(new GUIListener(this),           this);
        pm.registerEvents(new PlayerEventListener(this),   this);
        pm.registerEvents(new BlockEventListener(this),    this);
        pm.registerEvents(new EntityEventListener(this),   this);
        pm.registerEvents(new SpawnerPlaceListener(this),  this);
        pm.registerEvents(new SpawnerDropListener(this),   this);
        pm.registerEvents(new CrateListener(this),         this);
        pm.registerEvents(new MenuListener(this),          this);

        // Order system listener
        pm.registerEvents(new OrderListener(this, orderSystem, orderGUI), this);

        // NOTE: QuizSystem, AntiSpamSystem, CommandBlocker
        // register themselves in constructors
    }

    // ══════════════════════════════════
    //  SCHEDULED TASKS
    // ══════════════════════════════════

    private void startTasks() {

        // Scoreboard + Nametag update - setiap 3 detik (60 ticks)
        new BukkitRunnable() {
            @Override public void run() {
                if (lobbySystem != null) {
                    lobbySystem.updateAllScoreboards();
                    lobbySystem.updateAllNametags();
                }
            }
        }.runTaskTimer(this, 60L, 60L);

        // Playtime tracking - setiap 1 menit (1200 ticks)
        new BukkitRunnable() {
            @Override public void run() {
                if (lobbySystem != null) lobbySystem.trackPlaytime();
            }
        }.runTaskTimer(this, 1200L, 1200L);

        // NOTE: ClearLag sudah dihandle oleh LobbySystem internal scheduler
        // (setiap 5 menit, countdown 5 detik)
        // Tidak perlu task duplikat di sini

        // Auction expire check - setiap 5 menit (6000 ticks)
        new BukkitRunnable() {
            @Override public void run() {
                AuctionCommand.checkExpired(getInstance());
            }
        }.runTaskTimer(this, 6000L, 6000L);

        // Auto-save semua data - setiap 5 menit (6000 ticks)
        new BukkitRunnable() {
            @Override public void run() {
                if (islandSystem      != null) islandSystem.saveAll();
                if (landSystem        != null) landSystem.saveAll();
                if (lobbySystem       != null) lobbySystem.saveData();
                if (dailyRewardSystem != null) dailyRewardSystem.saveData();
                if (weeklyRewardSystem!= null) weeklyRewardSystem.saveData();
                if (crateSystem       != null) crateSystem.saveData();
                if (orderSystem       != null) orderSystem.saveData();
                getLogger().info("[KZ] Auto-save completed.");
            }
        }.runTaskTimer(this, 6000L, 6000L);

        // Lobby fireworks - setiap 3 menit (3600 ticks)
        new BukkitRunnable() {
            @Override public void run() {
                if (lobbySystem != null) lobbySystem.spawnFireworks();
            }
        }.runTaskTimer(this, 3600L, 3600L);

        // Request server name dari proxy - cek tiap 10 detik sampai dapat
        new BukkitRunnable() {
            @Override public void run() {
                if (!Bukkit.getOnlinePlayers().isEmpty()
                        && proxyMessageListener != null
                        && proxyMessageListener.getCachedServerName() == null) {
                    var any = Bukkit.getOnlinePlayers().iterator().next();
                    proxyMessageListener.requestServerName(any, name ->
                            getLogger().info("[KZ] Proxy confirmed server: " + name));
                    this.cancel();
                }
            }
        }.runTaskTimer(this, 100L, 200L);
    }

    // ══════════════════════════════════
    //  HELPER
    // ══════════════════════════════════

    private void setCmd(String name, Object executor) {
        var cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor((org.bukkit.command.CommandExecutor) executor);
            if (executor instanceof org.bukkit.command.TabCompleter tc) {
                cmd.setTabCompleter(tc);
            }
        } else {
            getLogger().warning("[KZ] Command '/" + name
                    + "' not found in plugin.yml!");
        }
    }

    // ══════════════════════════════════
    //  GETTERS
    // ══════════════════════════════════

    public static KZPlugin       getInstance()            { return instance; }
    public DatabaseManager       getDatabaseManager()     { return databaseManager; }
    public EconomyManager        getEconomyManager()      { return economyManager; }
    public IslandSystem          getIslandSystem()        { return islandSystem; }
    public OneBlockSystem        getOneBlockSystem()      { return oneBlockSystem; }
    public LandSystem            getLandSystem()          { return landSystem; }
    public JobSystem             getJobSystem()           { return jobSystem; }
    public TPASystem             getTpaSystem()           { return tpaSystem; }
    public LobbySystem           getLobbySystem()         { return lobbySystem; }
    public ItemDatabase          getItemDatabase()        { return itemDatabase; }
    public DailyRewardSystem     getDailyRewardSystem()   { return dailyRewardSystem; }
    public WeeklyRewardSystem    getWeeklyRewardSystem()  { return weeklyRewardSystem; }
    public SpawnerItemFactory    getSpawnerItemFactory()  { return spawnerItemFactory; }
    public CrateSystem           getCrateSystem()         { return crateSystem; }
    public BedrockFormManager    getBedrockFormManager()  { return bedrockFormManager; }
    public ProxyMessageListener  getProxyMessageListener(){ return proxyMessageListener; }
    public QuizSystem            getQuizSystem()          { return quizSystem; }
    public AntiSpamSystem        getAntiSpamSystem()      { return antiSpamSystem; }
    public CommandBlocker        getCommandBlocker()      { return commandBlocker; }
    public AdvancedOrderSystem   getOrderSystem()         { return orderSystem; }
    public OrderGUI              getOrderGUI()            { return orderGUI; }
}
