// ============================================================
// PATH: src/main/java/com/kz/plugin/systems/QuizSystem.java
// ============================================================
package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * QuizSystem - Auto-broadcast quiz with 3 types
 *
 * Types:
 * 1. MATH    - Solve math equations (+ - ×)
 * 2. SPEED   - Type exact text (case-sensitive)
 * 3. FILL    - Fill in the blank letters
 *
 * Features:
 * - Auto quiz every X minutes (configurable)
 * - Timer per quiz type
 * - Winner announcement with timing
 * - Prize: economy money
 * - Only 1 active quiz at a time
 * - Min 2 players online to start
 */
public class QuizSystem implements Listener {

    private final KZPlugin plugin;

    // ════════════════════════════════════════════════════════════════
    //  QUIZ STATE
    // ════════════════════════════════════════════════════════════════

    public enum QuizType {
        MATH, SPEED, FILL
    }

    private boolean quizActive = false;
    private QuizType activeType = null;
    private String activeAnswer = null;       // The correct answer
    private String activeDisplay = null;      // What was shown to players
    private long quizStartTime = 0;           // When quiz started (ms)
    private BukkitTask timerTask = null;       // Timeout task
    private BukkitTask autoSchedulerTask = null;
    private boolean caseSensitive = false;     // Whether answer check is case-sensitive

    // ════════════════════════════════════════════════════════════════
    //  CONFIG
    // ════════════════════════════════════════════════════════════════

    private int autoIntervalMinutes = 15;     // Auto quiz interval
    private int mathTimerSeconds = 300;       // 5 minutes
    private int speedTimerSeconds = 120;      // 2 minutes
    private int fillTimerSeconds = 180;       // 3 minutes
    private double mathPrize = 500.0;
    private double speedPrize = 300.0;
    private double fillPrize = 400.0;
    private int minPlayersForAuto = 2;

    // ════════════════════════════════════════════════════════════════
    //  WORD BANK - For fill-the-blank quiz
    // ════════════════════════════════════════════════════════════════

    private static final String[] WORD_BANK = {
            "DIAMOND", "EMERALD", "CREEPER", "ENDERMAN", "SKELETON",
            "MINECRAFT", "OBSIDIAN", "ENCHANT", "PICKAXE", "FURNACE",
            "VILLAGER", "REDSTONE", "NETHER", "PORTAL", "BEACON",
            "BLAZE", "WITHER", "ZOMBIE", "SPIDER", "PHANTOM",
            "TRIDENT", "ELYTRA", "SHULKER", "ANVIL", "CRAFTING",
            "POTION", "DRAGON", "IRON", "GOLD", "COPPER",
            "AMETHYST", "NETHERITE", "ENDER", "CHEST", "HOPPER",
            "PISTON", "LANTERN", "CANDLE", "BAMBOO", "CHERRY",
            "SCULK", "WARDEN", "SNIFFER", "CAMEL", "BREEZE",
            "ARMADILLO", "MACE", "TRIAL", "VAULT", "OMINOUS"
    };

    // ════════════════════════════════════════════════════════════════
    //  CHARACTERS - For speed-type quiz
    // ════════════════════════════════════════════════════════════════

    private static final String SPEED_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789#$@!%&*";

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════

    public QuizSystem(KZPlugin plugin) {
        this.plugin = plugin;

        // Load config values
        loadConfig();

        // Register self as listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Start auto scheduler
        startAutoScheduler();

        plugin.getLogger().info("[Quiz] System initialized. Auto quiz every "
                + autoIntervalMinutes + " minutes.");
    }

    private void loadConfig() {
        var config = plugin.getConfig();
        autoIntervalMinutes = config.getInt("quiz.auto-interval-minutes", 15);
        mathTimerSeconds = config.getInt("quiz.math-timer-seconds", 300);
        speedTimerSeconds = config.getInt("quiz.speed-timer-seconds", 120);
        fillTimerSeconds = config.getInt("quiz.fill-timer-seconds", 180);
        mathPrize = config.getDouble("quiz.math-prize", 500.0);
        speedPrize = config.getDouble("quiz.speed-prize", 300.0);
        fillPrize = config.getDouble("quiz.fill-prize", 400.0);
        minPlayersForAuto = config.getInt("quiz.min-players", 2);
    }

    // ════════════════════════════════════════════════════════════════
    //  AUTO SCHEDULER - Random quiz every X minutes
    // ════════════════════════════════════════════════════════════════

    private void startAutoScheduler() {
        long intervalTicks = autoIntervalMinutes * 60L * 20L;

        autoSchedulerTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Only start if enough players and no active quiz
                if (Bukkit.getOnlinePlayers().size() >= minPlayersForAuto && !quizActive) {
                    startRandomQuiz();
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    // ════════════════════════════════════════════════════════════════
    //  START QUIZ - Public methods
    // ════════════════════════════════════════════════════════════════

    /**
     * Start a random quiz type
     */
    public void startRandomQuiz() {
        if (quizActive) return;

        QuizType[] types = QuizType.values();
        QuizType type = types[ThreadLocalRandom.current().nextInt(types.length)];
        startQuiz(type);
    }

    /**
     * Start a specific quiz type
     */
    public void startQuiz(QuizType type) {
        if (quizActive) return;

        switch (type) {
            case MATH -> startMathQuiz();
            case SPEED -> startSpeedQuiz();
            case FILL -> startFillQuiz();
        }
    }

    /**
     * Force stop current quiz
     */
    public void stopQuiz() {
        if (!quizActive) return;

        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }

        broadcastAll("");
        broadcastAll("  §c§l⚡ QUIZ CANCELLED §8» §7The quiz has been cancelled by an admin.");
        broadcastAll("");

        resetQuizState();
    }

    // ════════════════════════════════════════════════════════════════
    //  TYPE 1: MATH QUIZ
    // ════════════════════════════════════════════════════════════════

    private void startMathQuiz() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        // Pick operation
        int op = rand.nextInt(3);
        int a, b, answer;
        String symbol;

        switch (op) {
            case 0 -> { // Addition
                a = rand.nextInt(10, 100);
                b = rand.nextInt(10, 100);
                answer = a + b;
                symbol = "+";
            }
            case 1 -> { // Subtraction (always positive result)
                a = rand.nextInt(50, 200);
                b = rand.nextInt(10, a);
                answer = a - b;
                symbol = "-";
            }
            default -> { // Multiplication
                a = rand.nextInt(2, 15);
                b = rand.nextInt(2, 15);
                answer = a * b;
                symbol = "×";
            }
        }

        String equation = a + " " + symbol + " " + b;
        activeAnswer = String.valueOf(answer);
        activeDisplay = equation;
        activeType = QuizType.MATH;
        caseSensitive = false;

        int timerMinutes = mathTimerSeconds / 60;

        // Broadcast
        broadcastAll("");
        broadcastAll("§6§l┌─────────────────────────────────────┐");
        broadcastAll("§6§l│     §e§l⚡ MATH QUIZ                     §6§l│");
        broadcastAll("§6§l└─────────────────────────────────────┘");
        broadcastAll("");
        broadcastAll("  §fSolve the following equation to win a prize!");
        broadcastAll("");
        broadcastAll("  §e§l     " + equation + " = ?");
        broadcastAll("");
        broadcastAll("  §7Type your answer in chat!");
        broadcastAll("  §7⏱ Timer : §f" + timerMinutes + " minutes");
        broadcastAll("  §7🎁 Prize : §a$" + formatNumber(mathPrize));
        broadcastAll("");
        broadcastAll("§6§l─────────────────────────────────────── ");
        broadcastAll("");

        soundAll(Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.5f);

        quizActive = true;
        quizStartTime = System.currentTimeMillis();

        // Start timer
        startTimer(mathTimerSeconds);
    }

    // ════════════════════════════════════════════════════════════════
    //  TYPE 2: SPEED TYPE QUIZ
    // ════════════════════════════════════════════════════════════════

    private void startSpeedQuiz() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        // Generate random string (8-12 chars)
        int length = rand.nextInt(8, 13);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(SPEED_CHARS.charAt(rand.nextInt(SPEED_CHARS.length())));
        }

        String randomText = sb.toString();
        activeAnswer = randomText;
        activeDisplay = randomText;
        activeType = QuizType.SPEED;
        caseSensitive = true; // Case-sensitive!

        int timerMinutes = speedTimerSeconds / 60;

        // Broadcast
        broadcastAll("");
        broadcastAll("§b§l┌─────────────────────────────────────┐");
        broadcastAll("§b§l│     §f§l⚡ SPEED TYPE QUIZ               §b§l│");
        broadcastAll("§b§l└─────────────────────────────────────┘");
        broadcastAll("");
        broadcastAll("  §fQuickly type the following text exactly!");
        broadcastAll("  §c§o(Case-sensitive! Capital letters matter!)");
        broadcastAll("");
        broadcastAll("  §e§l     " + randomText);
        broadcastAll("");
        broadcastAll("  §7First to type it correctly wins!");
        broadcastAll("  §7⏱ Timer : §f" + timerMinutes + " minutes");
        broadcastAll("  §7🎁 Prize : §a$" + formatNumber(speedPrize));
        broadcastAll("");
        broadcastAll("§b§l─────────────────────────────────────── ");
        broadcastAll("");

        soundAll(Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2f);

        quizActive = true;
        quizStartTime = System.currentTimeMillis();

        startTimer(speedTimerSeconds);
    }

    // ════════════════════════════════════════════════════════════════
    //  TYPE 3: FILL THE BLANK QUIZ
    // ════════════════════════════════════════════════════════════════

    private void startFillQuiz() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        // Pick random word
        String word = WORD_BANK[rand.nextInt(WORD_BANK.length)];
        activeAnswer = word;
        activeType = QuizType.FILL;
        caseSensitive = false; // Case-insensitive for fill

        // Create blanked version (hide 40-60% of letters)
        char[] display = word.toCharArray();
        int blanksNeeded = (int) (display.length * (0.4 + rand.nextDouble() * 0.2));
        Set<Integer> blankedIndices = new HashSet<>();

        // Always show first and last character
        while (blankedIndices.size() < blanksNeeded) {
            int idx = rand.nextInt(1, display.length - 1); // Skip first & last
            blankedIndices.add(idx);
        }

        StringBuilder blanked = new StringBuilder();
        for (int i = 0; i < display.length; i++) {
            if (blankedIndices.contains(i)) {
                blanked.append("_");
            } else {
                blanked.append(display[i]);
            }
        }

        // Add spaces between characters for readability
        String displayText = String.join(" ", blanked.toString().split(""));
        activeDisplay = blanked.toString();

        int timerMinutes = fillTimerSeconds / 60;

        // Broadcast
        broadcastAll("");
        broadcastAll("§d§l┌─────────────────────────────────────┐");
        broadcastAll("§d§l│     §f§l⚡ FILL THE BLANK                §d§l│");
        broadcastAll("§d§l└─────────────────────────────────────┘");
        broadcastAll("");
        broadcastAll("  §fComplete the word below to win a prize!");
        broadcastAll("");
        broadcastAll("  §e§l     " + displayText);
        broadcastAll("");
        broadcastAll("  §7Type the full word in chat!");
        broadcastAll("  §7⏱ Timer : §f" + timerMinutes + " minutes");
        broadcastAll("  §7🎁 Prize : §a$" + formatNumber(fillPrize));
        broadcastAll("");
        broadcastAll("§d§l─────────────────────────────────────── ");
        broadcastAll("");

        soundAll(Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);

        quizActive = true;
        quizStartTime = System.currentTimeMillis();

        startTimer(fillTimerSeconds);
    }

    // ════════════════════════════════════════════════════════════════
    //  TIMER - Countdown & expiry
    // ════════════════════════════════════════════════════════════════

    private void startTimer(int seconds) {
        // Warning at 30 seconds left
        if (seconds > 30) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (quizActive) {
                    broadcastAll("");
                    broadcastAll("  §c§l⚡ QUIZ §8» §c30 seconds remaining! Hurry up!");
                    broadcastAll("");
                    soundAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                }
            }, (seconds - 30) * 20L);
        }

        // Warning at 10 seconds left
        if (seconds > 10) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (quizActive) {
                    broadcastAll("");
                    broadcastAll("  §c§l⚡ QUIZ §8» §c§l10 seconds! Last chance!");
                    broadcastAll("");
                    soundAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                }
            }, (seconds - 10) * 20L);
        }

        // Expire
        timerTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (quizActive) {
                expireQuiz();
            }
        }, seconds * 20L);
    }

    private void expireQuiz() {
        broadcastAll("");
        broadcastAll("§c§l┌─────────────────────────────────────┐");
        broadcastAll("§c§l│     §f§l⏰ QUIZ EXPIRED                  §c§l│");
        broadcastAll("§c§l└─────────────────────────────────────┘");
        broadcastAll("");
        broadcastAll("  §7No one answered in time!");
        broadcastAll("  §7The correct answer was: §e§l" + activeAnswer);
        broadcastAll("");
        broadcastAll("§c§l─────────────────────────────────────── ");
        broadcastAll("");

        soundAll(Sound.ENTITY_VILLAGER_NO, 1f, 1f);

        resetQuizState();
    }

    // ════════════════════════════════════════════════════════════════
    //  ANSWER CHECKER - Chat listener
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!quizActive || activeAnswer == null) return;

        Player player = event.getPlayer();
        String message = event.getMessage().trim();

        // Check answer
        boolean correct;
        if (caseSensitive) {
            correct = message.equals(activeAnswer);
        } else {
            correct = message.equalsIgnoreCase(activeAnswer);
        }

        if (correct) {
            // Calculate timing
            long elapsed = System.currentTimeMillis() - quizStartTime;
            String timing = formatTiming(elapsed);

            // Determine prize
            double prize = switch (activeType) {
                case MATH -> mathPrize;
                case SPEED -> speedPrize;
                case FILL -> fillPrize;
            };

            // Get quiz type name
            String typeName = switch (activeType) {
                case MATH -> "§6§lMath Quiz";
                case SPEED -> "§b§lSpeed Type";
                case FILL -> "§d§lFill the Blank";
            };

            // Build answer display
            String answerDisplay = switch (activeType) {
                case MATH -> activeDisplay + " = §a§l" + activeAnswer;
                case SPEED -> activeAnswer;
                case FILL -> activeAnswer;
            };

            // Save state before reset
            String savedAnswer = activeAnswer;
            QuizType savedType = activeType;

            // Cancel timer
            if (timerTask != null) {
                timerTask.cancel();
                timerTask = null;
            }

            // Announce winner (must run on main thread)
            Bukkit.getScheduler().runTask(plugin, () -> {
                broadcastAll("");
                broadcastAll("§a§l┌─────────────────────────────────────┐");
                broadcastAll("§a§l│     §f§l🏆 QUIZ WINNER!                  §a§l│");
                broadcastAll("§a§l└─────────────────────────────────────┘");
                broadcastAll("");
                broadcastAll("  §7Quiz   : " + typeName);
                broadcastAll("  §7Winner : §b§l" + player.getName());
                broadcastAll("  §7Answer : §e" + answerDisplay);
                broadcastAll("  §7Time   : §e§l" + timing);
                broadcastAll("  §7Prize  : §a§l$" + formatNumber(prize));
                broadcastAll("");
                broadcastAll("  §7Congratulations! 🎉");
                broadcastAll("");
                broadcastAll("§a§l─────────────────────────────────────── ");
                broadcastAll("");

                // Sound
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.equals(player)) {
                        online.playSound(online.getLocation(),
                                Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    } else {
                        online.playSound(online.getLocation(),
                                Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                    }
                }

                // Give prize
                if (plugin.getEconomyManager() != null) {
                    plugin.getEconomyManager().addBalance(player, prize);
                    player.sendMessage("§a§lKZ §8» §a$" + formatNumber(prize)
                            + " §7has been added to your balance!");
                }
            });

            resetQuizState();
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  STATE MANAGEMENT
    // ════════════════════════════════════════════════════════════════

    private void resetQuizState() {
        quizActive = false;
        activeType = null;
        activeAnswer = null;
        activeDisplay = null;
        quizStartTime = 0;
        caseSensitive = false;
    }

    // ════════════════════════════════════════════════════════════════
    //  UTILITY METHODS
    // ════════════════════════════════════════════════════════════════

    private void broadcastAll(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    private void soundAll(Sound sound, float volume, float pitch) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        });
    }

    private String formatTiming(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        } else if (millis < 60000) {
            double seconds = millis / 1000.0;
            return String.format("%.1fs", seconds);
        } else {
            long minutes = millis / 60000;
            double seconds = (millis % 60000) / 1000.0;
            return minutes + "m " + String.format("%.1fs", seconds);
        }
    }

    private String formatNumber(double amount) {
        if (amount == (long) amount) {
            return String.format("%,d", (long) amount);
        }
        return String.format("%,.2f", amount);
    }

    // ════════════════════════════════════════════════════════════════
    //  PUBLIC GETTERS
    // ════════════════════════════════════════════════════════════════

    public boolean isQuizActive() {
        return quizActive;
    }

    public QuizType getActiveType() {
        return activeType;
    }

    // ════════════════════════════════════════════════════════════════
    //  SHUTDOWN
    // ════════════════════════════════════════════════════════════════

    public void shutdown() {
        if (timerTask != null) timerTask.cancel();
        if (autoSchedulerTask != null) autoSchedulerTask.cancel();
        resetQuizState();
        plugin.getLogger().info("[Quiz] System shutdown.");
    }
}
