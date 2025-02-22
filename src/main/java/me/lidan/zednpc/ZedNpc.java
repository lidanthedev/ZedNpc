package me.lidan.zednpc;

import io.github.gonalez.znpcs.npc.NPCType;
import io.netty.util.internal.logging.Log4JLoggerFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.lidan.zednpc.commands.ZedNpcCommand;
import me.lidan.zednpc.listeners.ZedNpcListener;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.util.Arrays;
import java.util.List;

public final class ZedNpc extends JavaPlugin {

    @Getter
    private static ZedNpc instance;
    private BukkitCommandHandler commandHandler;

    @Override
    public void onEnable() {
        // Plugin startup logic
        commandHandler = BukkitCommandHandler.create(this);
        commandHandler.getAutoCompleter().registerParameterSuggestions(OfflinePlayer.class, (args, sender, command) -> {
            List<String> lll = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            System.out.printf("SCAM YOU SPIGOT: %s%n", lll);
            getLogger().info("llll: %s".formatted(lll));
            return lll;
        });
        commandHandler.getAutoCompleter().registerParameterSuggestions(NPCType.class, (list, commandActor, executableCommand) -> Arrays.stream(NPCType.values()).map(Enum::name).toList());
        commandHandler.register(new ZedNpcCommand());
        commandHandler.registerBrigadier();

        registerEvent(new ZedNpcListener());

        getLogger().info("Loaded!");
    }

    public void registerEvent(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
