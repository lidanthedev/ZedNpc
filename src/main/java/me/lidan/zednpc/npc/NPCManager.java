package me.lidan.zednpc.npc;

import io.github.gonalez.znpcs.npc.NPC;
import lombok.Getter;
import lombok.Singular;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Getter
public class NPCManager {
    private static NPCManager instance;

    private final Map<CommandSender, Integer> selectedNPC = new HashMap<>();
    private final Map<CommandSender, Boolean> isSelecting = new HashMap<>();

    public @Nullable NPC getNpcById(int id) {
        return NPC.find(id);
    }

    public static NPCManager getInstance() {
        if (instance == null) {
            instance = new NPCManager();
        }
        return instance;
    }
}
