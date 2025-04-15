package me.lidan.zednpc.utils;

import io.github.gonalez.znpcs.user.EventService;
import io.github.gonalez.znpcs.user.ZUser;
import io.github.gonalez.znpcs.utility.Utils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class PromptUtils {
    public static CompletableFuture<String> promptForString(Player player, String title, String subtitle) {
        CompletableFuture<String> future = new CompletableFuture<>();
        Utils.sendTitle(player, title, subtitle);
        EventService.addService(ZUser.find(player), AsyncPlayerChatEvent.class).addConsumer(event -> {
            String input = event.getMessage();
            event.setCancelled(true);
            Utils.sendTitle(player, "", "");
            future.complete(input);
        });
        return future;
    }

    public static CompletableFuture<String> promptForString(Player player, String title) {
        return promptForString(player, title, "");
    }

    public static CompletableFuture<String> promptForString(Player player) {
        return promptForString(player, "Please enter a value", "");
    }

    public static CompletionStage<Integer> promptForInt(Player player, String title, String subtitle) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        promptForString(player, title, subtitle).thenAccept(input -> {
            try {
                int value = Integer.parseInt(input);
                future.complete(value);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "ERROR! Invalid input. Aborting.");
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public static CompletionStage<Double> promptForDouble(Player player, String title) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        promptForString(player, title).thenAccept(input -> {
            try {
                double value = Double.parseDouble(input);
                future.complete(value);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "ERROR! Invalid input. Aborting.");
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
