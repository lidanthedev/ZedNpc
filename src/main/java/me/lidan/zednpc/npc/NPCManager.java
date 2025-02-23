package me.lidan.zednpc.npc;

import io.github.gonalez.znpcs.npc.NPC;
import io.github.gonalez.znpcs.npc.NPCType;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;

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
        log.info("Actions: {}", actions);
        return actions;
    }

    public @Nullable NPC getSelectedNpcOf(CommandSender sender) {
        return getNpcById(selectedNPC.get(sender));
    }

    public List<String> getCompletionsForMethod(NPCType type, String methodName){
        Method method = type.getCustomizationLoader().getMethods().get(methodName);
        List<String> list;
        Class<?> clazz = method.getParameterTypes()[0];
        if (clazz.isEnum()){
            Object[] enumConstants = clazz.getEnumConstants();
            list = Arrays.stream(enumConstants).map(Object::toString).toList();
        }
        else if (clazz.isPrimitive() && clazz.getName().equals("boolean")) {
            list = List.of("true", "false");
        }
        else if (clazz.isPrimitive() && clazz.getName().equals("int")) {
            list = List.of("-1", "0");
        }
        else {
            list = List.of(clazz.getName());
        }
        return list;
    }

    public static NPCManager getInstance() {
        if (instance == null) {
            instance = new NPCManager();
        }
        return instance;
    }
}
