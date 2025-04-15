package me.lidan.zednpc.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import io.github.gonalez.znpcs.configuration.Configuration;
import io.github.gonalez.znpcs.configuration.ConfigurationValue;
import io.github.gonalez.znpcs.npc.conversation.Conversation;
import io.github.gonalez.znpcs.npc.conversation.ConversationKey;
import me.lidan.zednpc.utils.MiniMessageUtils;
import me.lidan.zednpc.utils.PromptUtils;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EditConversationGUI {
    private PaginatedGui gui;
    private Player player;
    private Conversation conversation;

    public EditConversationGUI(Player player, Conversation conversation) {
        this.player = player;
        this.gui = Gui.paginated().rows(5).title(MiniMessageUtils.miniMessage("<blue>Edit Conversation")).create();
        this.conversation = conversation;
        gui.disableAllInteractions();
        update();
    }

    private void update() {
        gui.clearPageItems();
        gui.getFiller().fillBorder(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty()).asGuiItem());
        gui.setItem(5, 5, ItemBuilder.from(Material.EMERALD_BLOCK).name(MiniMessageUtils.miniMessage("<green>New</green>")).asGuiItem(event -> {
            gui.close(player);
            // Add a new text to the conversation
            PromptUtils.promptForString(player, "&e&lNew Text", "&7Enter text for new Conversation").thenAccept(res -> {
                String colored = ChatColor.translateAlternateColorCodes('&', res);
                List<String> lines = Arrays.asList(colored.split("\\n"));
                conversation.getTexts().add(new ConversationKey(lines));
                player.sendMessage(ChatColor.GREEN + "Text has been added.");
                update();
                open();
            });
        }));
        gui.setItem(5, 9, ItemBuilder.from(Material.ARROW).name(MiniMessageUtils.miniMessage("<blue>Next Page")).asGuiItem(event -> {
            if (!gui.next()) {
                player.sendMessage(ChatColor.RED + "No next page.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }));
        gui.setItem(5, 1, ItemBuilder.from(Material.ARROW).name(MiniMessageUtils.miniMessage("<blue>Previous Page")).asGuiItem(event -> {
            if (!gui.previous()) {
                new ConversationsGUI(player).open();
            }
        }));
        for (ConversationKey text : conversation.getTexts()) {
            String[] lore = {"&7this conversation text has a delay of &b" + text.getDelay() + "s &7to be executed,", "&7the sound for the text is &b" + (text.getSoundName() == null ? "NONE" : text.getSoundName()) + "&7,", "&7before sending the text there is a delay of &b" + text.getDelay() + "s", "&7and the conversation has currently &b" + text.getActions().size() + " actions&7.", "&f&lUSES", " &bLeft-click &7to edit the text.", " &bRight-click &7to manage actions.", " &bShift-Left-click &7to change the position.", " &bShift-Right-click &7to change the sound.", " &bMiddle-click &7to change the delay.", " &bQ &7to remove text."};
            GuiItem item = ItemBuilder.from(Material.PAPER)
                    .setName(org.bukkit.ChatColor.AQUA + text.getTextFormatted() + "....").setLore(Arrays.stream(lore).map(s -> ChatColor.translateAlternateColorCodes('&', s)).toArray(String[]::new))
                    .asGuiItem(event -> {
                        if (event.getClick() == ClickType.LEFT) {
                            gui.close(player);
                            PromptUtils.promptForString(player, "&e&lEdit Text", "&7Enter new text for Conversation").thenAccept(res -> {
                                String colored = ChatColor.translateAlternateColorCodes('&', res);
                                List<String> lines = Arrays.asList(colored.split("\\n"));
                                text.getLines().clear();
                                text.getLines().addAll(lines);
                                player.sendMessage(ChatColor.GREEN + "Text has been edited.");
                            }).whenComplete((unused, throwable) -> {
                                update();
                                open();
                            });
                        }
                        else if (event.getClick() == ClickType.RIGHT) {
                            new ConversationActionsGUI(player, conversation, text).open();
                        }
                        else if (event.getClick() == ClickType.SHIFT_LEFT){
                            PromptUtils.promptForInt(player, "&e&lCHANGE POSITION &a>=0&c<=" + this.conversation.getTexts().size(), "&7Type the new position...").thenAccept(position -> {
                                if (position >= 0 && position <= this.conversation.getTexts().size() - 1) {
                                    Collections.swap(this.conversation.getTexts(), this.conversation.getTexts().indexOf(text), position);
                                    Configuration.MESSAGES.sendMessage(player, ConfigurationValue.SUCCESS);
                                } else {
                                    Configuration.MESSAGES.sendMessage(player, ConfigurationValue.INVALID_SIZE);
                                }
                            }).whenComplete((unused, throwable) -> {
                                update();
                                open();
                            });
                        }
                        else if (event.getClick() == ClickType.SHIFT_RIGHT){
                            PromptUtils.promptForString(player, "&e&lEdit Sound", "&7Enter new sound for Conversation").thenAccept(res -> {
                                text.setSoundName(res);
                                player.sendMessage(ChatColor.GREEN + "Sound has been edited.");
                            }).whenComplete((unused, throwable) -> {
                                update();
                                open();
                            });
                        }
                        else if (event.getClick() == ClickType.MIDDLE){
                            PromptUtils.promptForInt(player, "&e&lEdit Delay", "&7Enter new delay for Conversation").thenAccept(res -> {
                                if (res < 0){
                                    player.sendMessage(ChatColor.RED + "ERROR! Invalid input. Aborting.");
                                    return;
                                }
                                text.setDelay(res);
                                player.sendMessage(ChatColor.GREEN + "Delay has been edited.");
                            }).whenComplete((unused, throwable) -> {
                                update();
                                open();
                            });
                        }
                        else if (event.getClick() == ClickType.DROP){
                            this.conversation.getTexts().remove(text);
                            player.sendMessage(ChatColor.GREEN + "Text has been removed.");
                            update();
                            open();
                        }
                    });
            gui.addItem(item);
        }
        gui.update();
    }

    public void open() {
        gui.open(player);
    }
}
