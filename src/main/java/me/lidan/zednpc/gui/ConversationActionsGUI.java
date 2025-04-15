package me.lidan.zednpc.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import io.github.gonalez.znpcs.npc.NPCAction;
import io.github.gonalez.znpcs.npc.conversation.Conversation;
import io.github.gonalez.znpcs.npc.conversation.ConversationKey;
import io.github.gonalez.znpcs.utility.Utils;
import me.clip.placeholderapi.PlaceholderAPI;
import me.lidan.zednpc.ZedNpc;
import me.lidan.zednpc.npc.ActionType;
import me.lidan.zednpc.utils.MiniMessageUtils;
import me.lidan.zednpc.utils.PromptUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ConversationActionsGUI {
    private PaginatedGui gui;
    private Player player;
    private Conversation conversation;
    private ConversationKey conversationKey;
    private final BukkitAudiences adventure = ZedNpc.getInstance().adventure();

    public ConversationActionsGUI(Player player, Conversation conversation, ConversationKey conversationKey) {
        this.player = player;
        this.conversation = conversation;
        this.conversationKey = conversationKey;
        this.gui = Gui.paginated().rows(5).title(MiniMessageUtils.miniMessage("<blue>Conversation Actions")).create();
        gui.disableAllInteractions();
        update();
    }

    private void update() {
        gui.clearPageItems();
        gui.getFiller().fillBorder(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty()).asGuiItem());
        gui.setItem(5, 5, ItemBuilder.from(Material.EMERALD_BLOCK).name(MiniMessageUtils.miniMessage("<green>New</green>")).asGuiItem(event -> {
            // Add a new action to the conversation
            handleActionOperation(null, ChatColor.GREEN + "Action has been added.");
        }));
        gui.setItem(5, 9, ItemBuilder.from(Material.ARROW).name(MiniMessageUtils.miniMessage("<blue>Next Page")).asGuiItem(event -> {
            if (!gui.next()) {
                player.sendMessage(ChatColor.RED + "No next page.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }));
        gui.setItem(5, 1, ItemBuilder.from(Material.ARROW).name(MiniMessageUtils.miniMessage("<blue>Previous Page")).asGuiItem(event -> {
            if (!gui.previous()) {
                new EditConversationGUI(player, conversation).open();
            }
        }));
        for (NPCAction action : this.conversationKey.getActions()) {
            String actionName = String.valueOf(action.getActionType());
            String[] lore = {"&7this action type is &b" + action.getActionType(), "&f&lUSES", " &bLeft-Click &7to edit action.", " &bQ &7to remove action."};
            GuiItem item = ItemBuilder.from(getMaterialForAction(actionName)).setName(ChatColor.AQUA + action.getAction().substring(0, Math.min(action.getAction().length(), 24)) + "....").setLore(Arrays.stream(lore).map(s -> ChatColor.translateAlternateColorCodes('&', s)).toArray(String[]::new)).asGuiItem(event -> {
                if (event.getClick() == ClickType.DROP) {
                    conversationKey.getActions().remove(action);
                    player.sendMessage(ChatColor.GREEN + "Action has been removed.");
                    update();
                    gui.open(player);
                } else if (event.getClick() == ClickType.LEFT) {
                    handleActionOperation(action, ChatColor.GREEN + "Action has been edited.");
                }
            });

            gui.addItem(item);
        }
    }

    private void handleActionOperation(@Nullable NPCAction oldAction, String successMessage) {
        gui.close(player);
        createAction().thenAccept(npcAction -> {
            if (oldAction != null) {
                conversationKey.getActions().remove(oldAction);
            }
            conversationKey.getActions().add(npcAction);
            player.sendMessage(ChatColor.GREEN + successMessage);
        }).exceptionally(e -> {
            String message = e.getMessage();
            // remove exception name from message
            if (message != null && message.contains(":")) {
                message = message.substring(message.indexOf(":") + 1).trim();
            }
            player.sendMessage(ChatColor.RED + "Failed to create action: " + message);
            return null;
        }).whenComplete((unused, throwable) -> {
            update();
            open();
        });
    }

    private CompletableFuture<NPCAction> createAction() {
        CompletableFuture<NPCAction> future = new CompletableFuture<>();
        Audience audience = adventure.player(player);
        ActionType.helpActions(audience);
        PromptUtils.promptForString(player, "&e&lNew Action", "&7Enter action for new Conversation").thenAccept(res -> {
            String[] parts = res.split(" ");
            if (parts.length < 2) {
                future.completeExceptionally(new IllegalArgumentException("Invalid action"));
                return;
            }
            String actionType = parts[0].toUpperCase();
            try{
                ActionType.valueOf(actionType);
            } catch (IllegalArgumentException e) {
                future.completeExceptionally(new IllegalArgumentException("Invalid action type"));
                return;
            }
            NPCAction action = new NPCAction(actionType, String.join(" ", Arrays.copyOfRange(parts, 1, parts.length)));
            future.complete(action);
        });
        return future;
    }

    public Material getMaterialForAction(String action) {
        switch (action.toLowerCase()) {
            case "cmd":
                return Material.COMMAND_BLOCK;
            case "console":
                return Material.REPEATING_COMMAND_BLOCK;
            case "chat":
            case "message":
                return Material.PAPER;
            case "server":
                return Material.END_PORTAL_FRAME;
            default:
                return Material.BARRIER;
        }
    }

    public void open() {
        gui.open(player);
    }
}
