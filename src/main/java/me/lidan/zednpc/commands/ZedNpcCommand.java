package me.lidan.zednpc.commands;

import io.github.gonalez.znpcs.ServersNPC;
import io.github.gonalez.znpcs.configuration.Configuration;
import io.github.gonalez.znpcs.configuration.ConfigurationConstants;
import io.github.gonalez.znpcs.configuration.ConfigurationValue;
import io.github.gonalez.znpcs.npc.NPC;
import io.github.gonalez.znpcs.npc.NPCModel;
import io.github.gonalez.znpcs.npc.NPCSkin;
import io.github.gonalez.znpcs.npc.NPCType;
import io.github.gonalez.znpcs.skin.SkinFetcherResult;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import me.lidan.zednpc.npc.NPCManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.annotation.*;
import revxrsal.commands.annotation.Optional;

import java.util.*;

@Command({"zednpc", "zenpc", "npc", "znpc"})
public class ZedNpcCommand {
    interface SkinFunction {
        void apply(CommandSender var1, NPC var2, String var3, SkinFetcherResult var4);
    }

    private static final SkinFunction DO_APPLY_SKIN = (commandSender, npc, skinName, result) -> NPCSkin.forName(skinName, (paramArrayOfString, throwable) -> {
        if (throwable != null) {
            Configuration.MESSAGES.sendMessage(commandSender, ConfigurationValue.CANT_GET_SKIN, skinName);
        } else {
            npc.changeSkin(NPCSkin.forValues(paramArrayOfString));
            Configuration.MESSAGES.sendMessage(commandSender, ConfigurationValue.GET_SKIN, new Object[0]);
        }

        if (result != null) {
            result.onDone(paramArrayOfString, throwable);
        }
    });

    public static final String SELECTED_NPC_MESSAGE = "Selected NPC: %d";
    public static final String SELECT_NPC_MESSAGE = "Click on the NPC you want to select";
    public static final String NPC_NOT_PLAYER = "NPC is not a player";
    public static final String NPC_NOT_SELECTED = "No NPC selected select one with /npc select <id>";
    private final NPCManager npcManager = NPCManager.getInstance();

    @Subcommand("create")
    public void createNPC(Player sender, String name, @Default("PLAYER") NPCType entityType) {
//        sender.sendMessage("Creating NPC with type " + entityType + " and name " + name);
        java.util.Optional<NPCModel> max = ConfigurationConstants.NPC_LIST.stream().max(Comparator.comparingInt(NPCModel::getId));
        int id = max.map(npcModel -> npcModel.getId() + 1).orElse(0);
//        sender.sendMessage("NPC created with id " + id);
        if (name.length() < 3 || name.length() > 16) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.INVALID_NAME_LENGTH);
            return;
        }
        NPC npc = ServersNPC.createNPC(id, entityType, sender.getLocation(), name);
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.SUCCESS);
        if (entityType == NPCType.PLAYER) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.FETCHING_SKIN, name);
            DO_APPLY_SKIN.apply(sender, npc, name, null);
        }
        npcManager.getSelectedNPC().put(sender, id);
    }

    @Subcommand({"select","sel"})
    public void selectNPC(CommandSender sender, @Optional int id) {
        if (id == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("You must specify an id");
            } else {
                sender.sendMessage(SELECT_NPC_MESSAGE);
                npcManager.getIsSelecting().put(sender, true);
                return;
            }
        }
        if (ConfigurationConstants.NPC_LIST.stream().noneMatch(npcModel -> npcModel.getId() == id)) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.NPC_NOT_FOUND);
            return;
        }
        npcManager.getSelectedNPC().put(sender, id);
        sender.sendMessage(SELECTED_NPC_MESSAGE.formatted(id));
    }

    @Subcommand({"delete","del"})
    public void deleteNPC(Player sender, @Optional int id) {
        if (id == 0) {
            id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        }
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.NPC_NOT_FOUND);
            return;
        }
        ServersNPC.deleteNPC(id);
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.SUCCESS);
    }

    @Subcommand("skin")
    public void skinNPC(Player sender, String skinName) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        if (npc.getNpcPojo().getNpcType() != NPCType.PLAYER){
            sender.sendMessage(NPC_NOT_PLAYER);
            return;
        }
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.FETCHING_SKIN, skinName);
        DO_APPLY_SKIN.apply(sender, npc, skinName, null);
    }
}
