// ============================================================
// PATH: src/main/java/com/kz/plugin/commands/QuizCommand.java
// ============================================================
package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.systems.QuizSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class QuizCommand implements CommandExecutor, TabCompleter {

    private final KZPlugin plugin;

    public QuizCommand(KZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kzplugin.admin")) {
            sender.sendMessage("§c§lKZ §8» §cYou don't have permission to do this.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        QuizSystem quiz = plugin.getQuizSystem();
        if (quiz == null) {
            sender.sendMessage("§c§lKZ §8» §cQuiz system is not available.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (quiz.isQuizActive()) {
                    sender.sendMessage("§c§lKZ §8» §cA quiz is already active! Use §f/quiz stop §cfirst.");
                    return true;
                }

                if (args.length >= 2) {
                    // Specific type
                    switch (args[1].toLowerCase()) {
                        case "math" -> quiz.startQuiz(QuizSystem.QuizType.MATH);
                        case "speed" -> quiz.startQuiz(QuizSystem.QuizType.SPEED);
                        case "fill" -> quiz.startQuiz(QuizSystem.QuizType.FILL);
                        default -> {
                            sender.sendMessage("§c§lKZ §8» §cInvalid type. Use: §fmath, speed, fill");
                            return true;
                        }
                    }
                    sender.sendMessage("§a§lKZ §8» §aStarted §f" + args[1] + " §aquiz!");
                } else {
                    quiz.startRandomQuiz();
                    sender.sendMessage("§a§lKZ §8» §aStarted random quiz!");
                }
            }

            case "stop" -> {
                if (!quiz.isQuizActive()) {
                    sender.sendMessage("§c§lKZ §8» §cNo active quiz to stop.");
                    return true;
                }
                quiz.stopQuiz();
                sender.sendMessage("§a§lKZ §8» §aQuiz stopped.");
            }

            case "status" -> {
                if (quiz.isQuizActive()) {
                    sender.sendMessage("§a§lKZ §8» §7Active quiz: §f" + quiz.getActiveType());
                } else {
                    sender.sendMessage("§a§lKZ §8» §7No active quiz.");
                }
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§6§l┌────────────────────────────────────┐");
        sender.sendMessage("§6§l│       §f§lQUIZ COMMANDS                 §6§l│");
        sender.sendMessage("§6§l└────────────────────────────────────┘");
        sender.sendMessage("");
        sender.sendMessage("  §e/quiz start          §8- §7Start random quiz");
        sender.sendMessage("  §e/quiz start math     §8- §7Start math quiz");
        sender.sendMessage("  §e/quiz start speed    §8- §7Start speed type quiz");
        sender.sendMessage("  §e/quiz start fill     §8- §7Start fill blank quiz");
        sender.sendMessage("  §e/quiz stop           §8- §7Stop current quiz");
        sender.sendMessage("  §e/quiz status         §8- §7Check quiz status");
        sender.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String alias, String[] args) {
        if (!sender.hasPermission("kzplugin.admin")) return List.of();

        return switch (args.length) {
            case 1 -> List.of("start", "stop", "status").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
            case 2 -> {
                if (args[0].equalsIgnoreCase("start")) {
                    yield List.of("math", "speed", "fill").stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .toList();
                }
                yield List.of();
            }
            default -> List.of();
        };
    }
}
