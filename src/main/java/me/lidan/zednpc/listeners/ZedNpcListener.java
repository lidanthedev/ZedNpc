package me.lidan.zednpc.listeners;

import io.github.gonalez.znpcs.configuration.ConfigurationConstants;
import io.github.gonalez.znpcs.npc.NPC;
import io.github.gonalez.znpcs.npc.NPCModel;
import io.github.gonalez.znpcs.npc.event.NPCInteractEvent;
import lombok.extern.slf4j.Slf4j;
import me.lidan.zednpc.commands.ZedNpcCommand;
import me.lidan.zednpc.npc.NPCManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Slf4j
public class ZedNpcListener implements Listener {

    private NPCManager npcManager = NPCManager.getInstance();

    @EventHandler()
    public void onNpcInteract(NPCInteractEvent event) {
        Boolean selecting = npcManager.getIsSelecting().getOrDefault(event.getPlayer(), false);
        if (!selecting) {
            return;
        }
        log.info("Npc interact event: {} {}", event, event.getNpc().getEntityID());
        NPCModel npcModel = event.getNpc().getNpcPojo();
        if (npcModel == null) {
            return;
        }
        log.info("Found NPC id {}", npcModel.getId());
        npcManager.getSelectedNPC().put(event.getPlayer(), npcModel.getId());
        npcManager.getIsSelecting().put(event.getPlayer(), false);
        event.getPlayer().sendMessage(ZedNpcCommand.SELECTED_NPC_MESSAGE.formatted(npcModel.getId()));
    }
}
