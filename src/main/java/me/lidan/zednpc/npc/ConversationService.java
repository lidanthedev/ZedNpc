package me.lidan.zednpc.npc;

import io.github.gonalez.znpcs.configuration.Configuration;
import io.github.gonalez.znpcs.configuration.ConfigurationConstants;
import io.github.gonalez.znpcs.configuration.ConfigurationValue;
import io.github.gonalez.znpcs.npc.conversation.Conversation;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ConversationService {
    public static void createConversation(Player sender, @NotNull String conversationName) {
        if (Conversation.exists(conversationName)) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.CONVERSATION_FOUND);
            return;
        }

        if (conversationName.length() < 3 || conversationName.length() > 16) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.INVALID_NAME_LENGTH);
            return;
        }

        ConfigurationConstants.NPC_CONVERSATIONS.add(new Conversation(conversationName));
        sender.sendMessage(ChatColor.GREEN + "Conversation has been created. Use /zednpc conversation gui to edit messages in the conversation.");
    }
}
