package me.lidan.zednpc.npc;

import io.github.gonalez.znpcs.configuration.ConfigurationConstants;
import io.github.gonalez.znpcs.npc.NPC;
import io.github.gonalez.znpcs.npc.NPCModel;
import io.github.gonalez.znpcs.npc.NPCType;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Getter
public class NPCManager {
    public static final int MAX_RANGE = 5;
    private static NPCManager instance;

    private final Map<CommandSender, Integer> selectedNPC = new HashMap<>();
    private final Map<CommandSender, Boolean> isSelecting = new HashMap<>();

    public @Nullable NPC getNpcById(int id) {
        return NPC.find(id);
    }

    public List<String> getActionIndexes(NPC npc) {
        if (npc == null) {
            log.warn("NPC is null");
            return new ArrayList<>();
        }
        List<String> actions = new ArrayList<>();
        for (int i = 0; i < npc.getNpcPojo().getClickActions().size(); i++) {
            actions.add(String.valueOf(i));
        }
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
            list = Arrays.stream(enumConstants).map(Object::toString).collect(Collectors.toList());
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

    public void updateNpc(NPC npc) {
        NPCType npcType = npc.getNpcPojo().getNpcType();
        npc.changeType(npcType);
        for (Map.Entry<String, String[]> entry : npc.getNpcPojo().getCustomizationMap().entrySet()) {
            npcType.updateCustomization(npc, entry.getKey(), entry.getValue());
        }
    }

    public NPCModel getTargetNpc(Player sender) {
        Location location = sender.getEyeLocation();
        org.bukkit.util.Vector vector = location.getDirection();
        World world = location.getWorld();
        if (world == null) return null;
        for (int i = 0; i < MAX_RANGE; i++) {
            Vector newVector = vector.clone().multiply(i);
            Location newLocation = location.clone().add(newVector);
            for (NPCModel npc: ConfigurationConstants.NPC_LIST){
                Location npcLoc = npc.getLocation().bukkitLocation();
                if (!world.equals(npcLoc.getWorld())) continue;
                if (npcLoc.distance(newLocation) < 2) {
                    return npc;
                }
            }
        }
        return null;
    }

    public static NPCManager getInstance() {
        if (instance == null) {
            instance = new NPCManager();
        }
        return instance;
    }
}
