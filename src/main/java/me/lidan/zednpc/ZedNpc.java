package me.lidan.zednpc;

import io.github.gonalez.znpcs.configuration.ConfigurationConstants;
import io.github.gonalez.znpcs.npc.NPCType;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import me.lidan.zednpc.commands.ZedNpcCommand;
import me.lidan.zednpc.listeners.ZedNpcListener;
import me.lidan.zednpc.npc.NPCManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.util.Arrays;
import java.util.List;

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

        registerEvent(new ZedNpcListener());

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
