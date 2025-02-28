package me.lidan.zednpc;

import io.github.gonalez.znpcs.configuration.ConfigurationConstants;
import io.github.gonalez.znpcs.npc.NPC;
import io.github.gonalez.znpcs.npc.NPCPath;
import io.github.gonalez.znpcs.npc.NPCType;
import io.github.gonalez.znpcs.npc.conversation.Conversation;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import me.lidan.zednpc.commands.ZedNpcCommand;
import me.lidan.zednpc.npc.NPCManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public final class ZedNpc extends JavaPlugin {

    @Getter
    private static ZedNpc instance;
    private NPCManager npcManager;
    private BukkitCommandHandler commandHandler;
    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        adventure = BukkitAudiences.create(this);

        npcManager = NPCManager.getInstance();
        registerCommands();

        getLogger().info("Loaded!");
    }

    private void registerCommands() {
        commandHandler = BukkitCommandHandler.create(this);
        commandHandler.getAutoCompleter().registerSuggestion("npc-id", (args, sender, command) -> ConfigurationConstants.NPC_LIST.stream().map(npcModel -> String.valueOf(npcModel.getId())).toList());
        commandHandler.getAutoCompleter().registerSuggestion("toggle-settings",(args, sender, command) -> List.of("glow", "holo", "mirror", "look"));
        commandHandler.getAutoCompleter().registerSuggestion("actions",(args, sender, command) -> List.of("cmd", "console", "chat", "message", "server"));
        commandHandler.getAutoCompleter().registerParameterSuggestions(NPCType.class, (list, commandActor, executableCommand) -> Arrays.stream(NPCType.values()).map(Enum::name).toList());
        commandHandler.getAutoCompleter().registerSuggestion("action-id", (args, sender, command) -> {
            Player player = Bukkit.getPlayer(sender.getName());
            return npcManager.getActionIndexes(npcManager.getSelectedNpcOf(player));
        });
        commandHandler.getAutoCompleter().registerSuggestion("customize-method", (args, sender, command) -> {
            Player player = Bukkit.getPlayer(sender.getName());
            NPC npc = npcManager.getSelectedNpcOf(player);
            if (npc == null) {
                return List.of();
            }
            return npc.getNpcPojo().getNpcType().getCustomizationLoader().getMethods().keySet();
        });
        commandHandler.getAutoCompleter().registerSuggestion("customize-method-args", (args, sender, command) -> {
            Player player = Bukkit.getPlayer(sender.getName());
            NPC npc = npcManager.getSelectedNpcOf(player);
            if (npc == null) {
                return List.of();
            }
            String methodName = args.get(0);
            return npcManager.getCompletionsForMethod(npc.getNpcPojo().getNpcType(), methodName);
        });
        commandHandler.getAutoCompleter().registerSuggestion("path-name", (args, sender, command) ->
            Stream.concat(NPCPath.AbstractTypeWriter.getPaths().stream().map(NPCPath.AbstractTypeWriter::getName), Stream.of("clear")).toList()
        );
        commandHandler.getAutoCompleter().registerSuggestion("conversation-name", (args, sender, command) -> Stream.concat(ConfigurationConstants.NPC_CONVERSATIONS.stream().map(Conversation::getName), Stream.of("clear")).toList());
        commandHandler.register(new ZedNpcCommand());
        commandHandler.registerBrigadier();
    }

    public @NonNull BukkitAudiences adventure() {
        if(this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    public void registerEvent(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
