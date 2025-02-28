package me.lidan.zednpc.commands;

import com.google.common.collect.Iterables;
import io.github.gonalez.znpcs.ServersNPC;
import io.github.gonalez.znpcs.commands.list.inventory.ConversationGUI;
import io.github.gonalez.znpcs.configuration.Configuration;
import io.github.gonalez.znpcs.configuration.ConfigurationConstants;
import io.github.gonalez.znpcs.configuration.ConfigurationValue;
import io.github.gonalez.znpcs.npc.*;
import io.github.gonalez.znpcs.npc.conversation.Conversation;
import io.github.gonalez.znpcs.npc.conversation.ConversationModel;
import io.github.gonalez.znpcs.skin.SkinFetcherResult;
import io.github.gonalez.znpcs.user.ZUser;
import io.github.gonalez.znpcs.utility.location.ZLocation;
import lombok.extern.slf4j.Slf4j;
import me.lidan.zednpc.ZedNpc;
import me.lidan.zednpc.npc.ActionType;
import me.lidan.zednpc.npc.NPCManager;
import me.lidan.zednpc.utils.MiniMessageUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import revxrsal.commands.annotation.*;
import revxrsal.commands.annotation.Optional;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Command({"zednpc", "zenpc", "npc", "znpc"})
public class ZedNpcCommand {
    interface SkinFunction {
        void apply(CommandSender var1, NPC var2, String var3, SkinFetcherResult var4);
    }

    private static final SkinFunction DO_APPLY_SKIN = (commandSender, npc, skinName, result) -> NPCSkin.forName(skinName, (paramArrayOfString, throwable) -> {
        if (throwable != null) {
            Configuration.MESSAGES.sendMessage(commandSender, ConfigurationValue.CANT_GET_SKIN, skinName);
        } else {
            npc.changeSkin(NPCSkin.forValues(paramArrayOfString));
            Configuration.MESSAGES.sendMessage(commandSender, ConfigurationValue.GET_SKIN, new Object[0]);
        }

        if (result != null) {
            result.onDone(paramArrayOfString, throwable);
        }
    });

    public static final String SELECTED_NPC_MESSAGE = "Selected NPC: %d";
    public static final String SELECT_NPC_MESSAGE = "Click on the NPC you want to select";
    public static final String NPC_NOT_PLAYER = "NPC is not a player";
    public static final String NPC_NOT_SELECTED = "No NPC selected select one with /npc select <id>";
    public static final String NPC_INFO = "<green>- <id> <hologram-lines> (<world> <x> <y> <z>) <hover:show_text:'Click to select this npc'><click:run_command:'/zednpc select <id>'><blue><b>[SELECT]</click> </hover><hover:show_text:'<green>Click to teleport to this npc'><click:run_command:'/zednpc teleport <id>'><dark_green><b>[TELEPORT]</click></hover> <hover:show_text:'<red>Click to delete this npc'><click:suggest_command:'/zednpc delete <id>'><dark_red><b>[DELETE]</click></hover>";
    private final NPCManager npcManager = NPCManager.getInstance();
    private final BukkitAudiences adventure = ZedNpc.getInstance().adventure();

    private final Path znpcsPluginFolder = ServersNPC.PLUGIN_FOLDER.toPath();
    private final Path znpcsPathFolder = znpcsPluginFolder.resolve("paths");

    @Subcommand("create")
    public void createNPC(Player sender, String name, @Default("PLAYER") NPCType entityType) {
//        sender.sendMessage("Creating NPC with type " + entityType + " and name " + name);
        java.util.Optional<NPCModel> max = ConfigurationConstants.NPC_LIST.stream().max(Comparator.comparingInt(NPCModel::getId));
        int id = max.map(npcModel -> npcModel.getId() + 1).orElse(0);
//        sender.sendMessage("NPC created with id " + id);
        if (name.length() < 3 || name.length() > 16) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.INVALID_NAME_LENGTH);
            return;
        }
        NPC npc = ServersNPC.createNPC(id, entityType, sender.getLocation(), name);
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.SUCCESS);
        if (entityType == NPCType.PLAYER) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.FETCHING_SKIN, name);
            DO_APPLY_SKIN.apply(sender, npc, name, null);
        }
        npcManager.getSelectedNPC().put(sender, id);
    }

    @Subcommand({"select","sel"})
    @AutoComplete("@npc-id *")
    public void selectNPC(CommandSender sender, @Optional int id) {
        if (id == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("You must specify an id");
            } else {
                sender.sendMessage(SELECT_NPC_MESSAGE);
                npcManager.getIsSelecting().put(sender, true);
                return;
            }
        }
        if (ConfigurationConstants.NPC_LIST.stream().noneMatch(npcModel -> npcModel.getId() == id)) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.NPC_NOT_FOUND);
            return;
        }
        npcManager.getSelectedNPC().put(sender, id);
        sender.sendMessage(SELECTED_NPC_MESSAGE.formatted(id));
    }

    @Subcommand({"delete","del"})
    @AutoComplete("@npc-id *")
    public void deleteNPC(Player sender, @Optional int id) {
        if (id == 0) {
            id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        }
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.NPC_NOT_FOUND);
            return;
        }
        ServersNPC.deleteNPC(id);
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.SUCCESS);
    }

    @Subcommand("skin")
    public void skinNPC(Player sender, String skinName) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        if (npc.getNpcPojo().getNpcType() != NPCType.PLAYER){
            sender.sendMessage(NPC_NOT_PLAYER);
            return;
        }
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.FETCHING_SKIN, skinName);
        DO_APPLY_SKIN.apply(sender, npc, skinName, null);
    }

    @Subcommand("list")
    public void listNPCs(CommandSender sender) {
        Audience audience = adventure.sender(sender);
        audience.sendMessage(MiniMessageUtils.miniMessage("<dark_green>NPC list:"));

        for(NPCModel npcModel : ConfigurationConstants.NPC_LIST) {
            audience.sendMessage(MiniMessageUtils.miniMessage(NPC_INFO, Map.of("id", npcModel.getId(), "hologram-lines", npcModel.getHologramLines(), "world", npcModel.getLocation().getWorldName(), "x", (int) npcModel.getLocation().getX(), "y", (int) npcModel.getLocation().getY(), "z", (int) npcModel.getLocation().getZ())));
        }
    }

    @Subcommand({"teleport", "tp"})
    @AutoComplete("@npc-id *")
    public void teleportToNPC(Player sender, @Optional int id) {
        if (id == 0) {
            id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        }
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        sender.teleport(npc.getNpcPojo().getLocation().bukkitLocation());
    }

    @Subcommand({"move", "tome", "tptome"})
    @AutoComplete("@npc-id *")
    public void teleportNPC(Player sender, @Optional int id) {
        if (id == 0) {
            id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        }
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        npc.getNpcPojo().setLocation(new ZLocation(sender.getLocation()));
        npcManager.updateNpc(npc);
    }

    @Subcommand("type")
    @AutoComplete("* @npc-id *")
    public void changeType(Player sender, NPCType type, @Optional int id) {
        if (id == 0) {
            id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        }
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        npc.changeType(type);
    }

    @Subcommand("lines")
    public void changeLines(Player sender, String lines) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        String[] split = lines.split("\\|"); // Split by pipe
        List<String> hologramLines = Arrays.asList(split);
        Collections.reverse(hologramLines);
        npc.getNpcPojo().setHologramLines(hologramLines);
        npcManager.updateNpc(npc);
    }

    @Subcommand("height")
    public void changeHeight(Player sender, double height) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        npc.getNpcPojo().setHologramHeight(height);
        npcManager.updateNpc(npc);
    }

    @Subcommand("toggle")
    @AutoComplete("* @toggle-settings *")
    public void toggle(Player sender, String toggle, @Optional String color) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        NPCFunction npcFunction = FunctionFactory.findFunctionForName(toggle);
        if (npcFunction.getName().equalsIgnoreCase("glow")) {
            npcFunction.doRunFunction(npc, new FunctionContext.ContextWithValue(npc, color));
        } else {
            npcFunction.doRunFunction(npc, new FunctionContext.DefaultContext(npc));
        }
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.SUCCESS);
    }

    @Subcommand("holo")
    public void holo(Player sender) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        toggle(sender, "holo", "");
    }

    @Subcommand("glow")
    public void glow(Player sender, String color) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        toggle(sender, "glow", color);
    }

    @Subcommand("mirror")
    public void mirror(Player sender) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        toggle(sender, "mirror", "");
    }

    @Subcommand("look")
    public void look(Player sender) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        toggle(sender, "look", "");
    }

    @Subcommand("action add")
    public void actionAdd(Player sender, ActionType action, String value) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        npc.getNpcPojo().getClickActions().add(new NPCAction(action.name(), value));
        npcManager.updateNpc(npc);
    }

    @Subcommand("action remove")
    @AutoComplete("@action-id *")
    public void actionRemove(Player sender, int index) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        if (index < 0 || index >= npc.getNpcPojo().getClickActions().size()) {
            sender.sendMessage("Invalid index");
            return;
        }
        npc.getNpcPojo().getClickActions().remove(index);
        npcManager.updateNpc(npc);
    }

    @Subcommand("action list")
    public void actionList(Player sender) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        List<NPCAction> actions = npc.getNpcPojo().getClickActions();
        for (int i = 0; i < actions.size(); i++) {
            NPCAction action = actions.get(i);
            sender.sendMessage(i + ": " + action.getActionType() + " " + action.getAction());
        }
    }

    @Subcommand("action cooldown")
    @AutoComplete("@action-id *")
    public void actionCooldown(Player sender, int index, int cooldown) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        if (index < 0 || index >= npc.getNpcPojo().getClickActions().size()) {
            sender.sendMessage("Invalid index");
            return;
        }
        npc.getNpcPojo().getClickActions().get(index).setDelay(cooldown);
        npcManager.updateNpc(npc);
    }

    @Subcommand("equip")
    public void equip(Player sender, ItemSlot item) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        ItemStack hand = sender.getInventory().getItemInMainHand();
        npc.getNpcPojo().getNpcEquip().put(item, hand);
        npc.getPackets().flushCache("equipPackets");
        Set<ZUser> viewers = npc.getViewers();
        viewers.forEach(npc::sendEquipPackets);
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.SUCCESS);
    }

    @Subcommand("customize")
    @AutoComplete("@customize-method @customize-method-args *")
    public void customize(Player sender, String methodName, @Optional String value) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        NPCType npcType = npc.getNpcPojo().getNpcType();
        if (npcType.getCustomizationLoader().contains(methodName)) {
            Method method = npcType.getCustomizationLoader().getMethods().get(methodName);
            if (value == null) {
                List<String> completionsForMethod = npcManager.getCompletionsForMethod(npcType, methodName);
                sender.sendMessage("Possible values: " + String.join(", ", completionsForMethod));
                return;
            }
            List<String> split = Arrays.asList(value.split(" "));
            if (Iterables.size(split) < method.getParameterTypes().length) {
                Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.TOO_FEW_ARGUMENTS);
                return;
            }
            String[] values = Iterables.toArray(split, String.class);
            npc.getNpcPojo().getCustomizationMap().put(methodName, values);
            npcManager.updateNpc(npc);
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.SUCCESS);
        }
    }

    @Subcommand("path create")
    public void createPath(Player sender, String pathName) {
        ZUser znpcUser = ZUser.find(Objects.requireNonNull(sender.getPlayer()));
        if (pathName.length() < 3 || pathName.length() > 16) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.INVALID_NAME_LENGTH);
            return;
        }

        if (NPCPath.AbstractTypeWriter.find(pathName) != null) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.PATH_FOUND);
            return;
        }

        if (znpcUser.isHasPath()) {
            sender.sendMessage(ChatColor.RED + "You already have a path creator active, to remove it use /zednpc path exit.");
            return;
        }

        File file = znpcsPathFolder.resolve(pathName + ".path").toFile();
        NPCPath.AbstractTypeWriter.forCreation(file, znpcUser, NPCPath.AbstractTypeWriter.TypeWriter.MOVEMENT);
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.PATH_START);
    }

    @Subcommand("path exit")
    public void exitPath(Player sender) {
        ZUser znpcUser = ZUser.find(Objects.requireNonNull(sender.getPlayer()));
        if (!znpcUser.isHasPath()) {
            sender.sendMessage(ChatColor.RED + "You do not have a path creator active.");
            return;
        }
        znpcUser.setHasPath(false);
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.EXIT_PATH);
    }

    @Subcommand("path list")
    public void listPaths(CommandSender sender) {
        Audience audience = adventure.sender(sender);
        audience.sendMessage(MiniMessageUtils.miniMessage("<dark_green>Path list:"));

        if (NPCPath.AbstractTypeWriter.getPaths().isEmpty()) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.NO_PATH_FOUND);
        } else {
            for (NPCPath.AbstractTypeWriter file : NPCPath.AbstractTypeWriter.getPaths()) {
                audience.sendMessage(MiniMessageUtils.miniMessage("<green>- <path-name> <hover:show_text:'Click to apply this path to the selected NPC'><click:run_command:'/zednpc path set <path-name>'><blue><b>[SET]</click></hover>", Map.of("path-name", file.getName())));
            }
        }
    }

    @Subcommand("path set")
    @AutoComplete("@path-name *")
    public void setPath(Player sender, String pathName) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }
        NPCPath.AbstractTypeWriter npcPath = NPCPath.AbstractTypeWriter.find(pathName);
        if (pathName.equalsIgnoreCase("clear")){
            npcPath = null;
            npcManager.updateNpc(npc);
        }
        npc.setPath(npcPath);
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.SUCCESS);
    }

    @Subcommand("conversation create")
    public void createConversation(Player sender, String conversationName) {
        if (Conversation.exists(conversationName)) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.CONVERSATION_FOUND);
            return;
        }

        if (conversationName.length() < 3 || conversationName.length() > 16) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.INVALID_NAME_LENGTH);
            return;
        }

        ConfigurationConstants.NPC_CONVERSATIONS.add(new Conversation(conversationName));
        sender.sendMessage(ChatColor.GREEN + "Conversation has been created. Use /zednpc conversation gui to edit messages in the conversation.");
    }

    @Subcommand("conversation gui")
    public void conversationGui(Player sender) {
        sender.openInventory((new ConversationGUI(sender)).build());
    }

    @Subcommand("conversation list")
    public void listConversations(CommandSender sender) {
        Audience audience = adventure.sender(sender);
        audience.sendMessage(MiniMessageUtils.miniMessage("<dark_green>Conversation list:"));

        if (ConfigurationConstants.NPC_CONVERSATIONS.isEmpty()) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.NO_CONVERSATION_FOUND);
        } else {
            for (Conversation conversation : ConfigurationConstants.NPC_CONVERSATIONS) {
                audience.sendMessage(MiniMessageUtils.miniMessage("<green>- <conversation-name> <hover:show_text:'Click to edit this conversation'><click:run_command:'/zednpc conversation gui'><blue><b>[EDIT]</click></hover>", Map.of("conversation-name", conversation.getName())));
            }
        }
    }

    @Subcommand("conversation remove")
    public void removeConversation(Player sender, String conversationName) {
        if (!Conversation.exists(conversationName)) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.NO_CONVERSATION_FOUND);
            return;
        }

        ConfigurationConstants.NPC_CONVERSATIONS.remove(Conversation.forName(conversationName));
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.SUCCESS);
    }

    @Subcommand("conversation set")
    @AutoComplete("@conversation-name @conversation-type *")
    public void setConversation(Player sender, String conversationName, @Optional String type) {
        int id = npcManager.getSelectedNPC().getOrDefault(sender, 0);
        NPC npc = npcManager.getNpcById(id);
        if (npc == null) {
            sender.sendMessage(NPC_NOT_SELECTED);
            return;
        }

        if (conversationName.equalsIgnoreCase("clear")){
            npc.getNpcPojo().setConversation(null);
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.SUCCESS);
            return;
        }

        if (!Conversation.exists(conversationName)) {
            npc.getNpcPojo().setConversation(null);
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.NO_CONVERSATION_FOUND);
            return;
        }
        npc.getNpcPojo().setConversation(new ConversationModel(conversationName, type != null ? type : "CLICK"));
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.SUCCESS);
    }
}
