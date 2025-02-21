package me.lidan.zednpc;

import lombok.Getter;
import me.lidan.zednpc.commands.ZedNpcCommand;
import org.bukkit.entity.EntityType;
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
        commandHandler.registerBrigadier();
        commandHandler.getAutoCompleter().registerParameterSuggestions(EntityType.class, (list, commandActor, executableCommand) -> {
            List<String> types = Arrays.stream(EntityType.values()).map(Enum::name).toList();
            getLogger().info("types: %s".formatted(types));
            return types;
        });
        commandHandler.register(new ZedNpcCommand());

        getLogger().info("Loaded!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
