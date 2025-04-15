package me.lidan.zednpc.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import io.github.gonalez.znpcs.configuration.Configuration;
import io.github.gonalez.znpcs.configuration.ConfigurationConstants;
import io.github.gonalez.znpcs.configuration.ConfigurationValue;
import io.github.gonalez.znpcs.npc.conversation.Conversation;
import io.github.gonalez.znpcs.npc.conversation.ConversationKey;
import me.lidan.zednpc.npc.ConversationService;
import me.lidan.zednpc.utils.MiniMessageUtils;
import me.lidan.zednpc.utils.PromptUtils;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ConversationsGUI {
    private final PaginatedGui gui;
    private final Player player;

    public ConversationsGUI(Player player) {
        this.player = player;
        this.gui = Gui.paginated().pageSize(21).title(MiniMessageUtils.miniMessage("<blue>Conversations")).rows(5).create();
        gui.disableAllInteractions();
        update();
    }

    private void update() {
        gui.clearPageItems();
        gui.getFiller().fillBorder(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty()).asGuiItem());
        gui.setItem(5, 5, ItemBuilder.from(Material.EMERALD_BLOCK).name(MiniMessageUtils.miniMessage("<green>New</green>")).asGuiItem(event -> {
            gui.close(player);
            PromptUtils.promptForString(player, "&e&lNew Conversation", "&7Enter name for new Conversation").thenAccept(res -> {
                createConversation(player, res);
                update();
                gui.open(player);
            });
        }));
        gui.setItem(5, 9, ItemBuilder.from(Material.ARROW).name(MiniMessageUtils.miniMessage("<blue>Next Page")).asGuiItem(event -> {
            if (!gui.next()){
                player.sendMessage(ChatColor.RED + "No next page.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }));
        gui.setItem(5, 1, ItemBuilder.from(Material.ARROW).name(MiniMessageUtils.miniMessage("<blue>Previous Page")).asGuiItem(event -> {
            if (!gui.previous()){
                player.sendMessage(ChatColor.RED + "No previous page.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }));
        for (Conversation conversation : ConfigurationConstants.NPC_CONVERSATIONS) {
            String[] lore = new String[]{"&7this conversation has &b" + conversation.getTexts().size() + " &7texts,", "&7it will activate when a player is on a &b" + conversation.getRadius() + "x" + conversation.getRadius() + " &7radius,", "&7or when a player interacts with an npc.", "&7there is a &b" + conversation.getDelay() + "s &7delay to start again.", "&f&lUSES", " &bLeft-click &7to manage texts.", " &bRight-click &7to add a new text.", " &bMiddle-click &7to change the cooldown.", " &bQ &7to change the radius."};
            gui.addItem(ItemBuilder.from(Material.PAPER)
                    .name(MiniMessageUtils.miniMessage("<green>" + conversation.getName()))
                    .setLore(Arrays.stream(lore).map(s -> ChatColor.translateAlternateColorCodes('&', s)).toArray(String[]::new))
                    .asGuiItem(event -> {
                        handleClickOnConversation(conversation, event);
                    }));
        }
        gui.update();
    }

    public void handleClickOnConversation(Conversation conversation, InventoryClickEvent event) {
        if (event.isLeftClick()) {
            // Open the conversation edit GUI
            new EditConversationGUI(player, conversation).open();
        } else if (event.isRightClick()) {
            // Add a new text to the conversation
            gui.close(player);
            PromptUtils.promptForString(player, "&e&lNew Text", "&7Enter text for new Conversation").thenAccept(res -> {
                String colored = ChatColor.translateAlternateColorCodes('&', res);
                List<String> lines = Arrays.asList(colored.split("\\n"));
                conversation.getTexts().add(new ConversationKey(lines));
                player.sendMessage(ChatColor.GREEN + "Text has been added.");
                update();
                gui.open(player);
            });
        } else if (event.getClick() == ClickType.DROP) {
            // Change the radius of the conversation
            gui.close(player);
            PromptUtils.promptForInt(player, "&e&lChange Radius", "&7Enter new radius for the conversation").thenAccept(res -> {
                conversation.setRadius(res);
                player.sendMessage(ChatColor.GREEN + "Radius has been changed.");
                update();
                gui.open(player);
            });
        } else if (event.getClick() == ClickType.MIDDLE) {
            // Change the cooldown of the conversation
            gui.close(player);
            PromptUtils.promptForInt(player, "&e&lChange Cooldown", "&7Enter new cooldown for the conversation").thenAccept(res -> {
                conversation.setDelay(res);
                player.sendMessage(ChatColor.GREEN + "Cooldown has been changed.");
                update();
                gui.open(player);
            });
        }
    }

    public void open() {
        gui.open(player);
    }

    public void createConversation(Player sender, String conversationName) {
        ConversationService.createConversation(sender, conversationName);
    }
}
