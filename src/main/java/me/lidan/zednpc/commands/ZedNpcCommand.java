package me.lidan.zednpc.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;

@Command({"zednpc", "zenpc", "npc"})
public class ZedNpcCommand {

    @Subcommand("create")
    public void createNPC(CommandSender sender, EntityType entityType, String name) {
        sender.sendMessage("Creating NPC with type " + entityType + " and name " + name);
    }
}
