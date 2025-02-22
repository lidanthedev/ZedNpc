package me.lidan.zednpc.npc;

import io.github.gonalez.znpcs.npc.NPC;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
public class NPCManager {
    private static NPCManager instance;

    private final Map<CommandSender, Integer> selectedNPC = new HashMap<>();
    private final Map<CommandSender, Boolean> isSelecting = new HashMap<>();

    public @Nullable NPC getNpcById(int id) {
        return NPC.find(id);
    }

    public List<String> getActionIndexes(NPC npc) {
        log.info("NPC: {}", npc);
        if (npc == null) {
            log.info("NPC is null");
            return new ArrayList<>();
        }
        List<String> actions = new ArrayList<>();
        for (int i = 0; i < npc.getNpcPojo().getClickActions().size(); i++) {
            actions.add(String.valueOf(i));
        }
        return actions;
    }

    public NPC getSelectedNpcOf(CommandSender sender) {
        return getNpcById(selectedNPC.get(sender));
    }

    public static NPCManager getInstance() {
        if (instance == null) {
            instance = new NPCManager();
        }
        return instance;
    }
}
