package me.lidan.zednpc.commands;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import io.github.gonalez.znpcs.ServersNPC;
import io.github.gonalez.znpcs.configuration.Configuration;
import io.github.gonalez.znpcs.configuration.ConfigurationConstants;
import io.github.gonalez.znpcs.configuration.ConfigurationValue;
import io.github.gonalez.znpcs.npc.*;
import io.github.gonalez.znpcs.npc.conversation.Conversation;
import io.github.gonalez.znpcs.npc.conversation.ConversationModel;
import io.github.gonalez.znpcs.skin.*;
import io.github.gonalez.znpcs.user.ZUser;
import io.github.gonalez.znpcs.utility.PlaceholderUtils;
import io.github.gonalez.znpcs.utility.location.ZLocation;
import lombok.extern.log4j.Log4j2;
import me.lidan.zednpc.ZedNpc;
import me.lidan.zednpc.gui.ConversationsGUI;
import me.lidan.zednpc.npc.ActionType;
import me.lidan.zednpc.npc.ConversationService;
import me.lidan.zednpc.npc.NPCManager;
import me.lidan.zednpc.utils.MiniMessageUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.annotation.*;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;

@Log4j2
@CommandPermission("zednpc.admin")
@Command({"zednpc", "zenpc", "npc", "znpc"})
public class ZedNpcCommand {

    public static final String NO_NPC_FOUND = "No NPC found";
    private final SkinFetcher skinFetcher = new SkinFetcher(Executors.newCachedThreadPool(), ImmutableList.of(new MineSkinFetch(), new MojangNameSkinFetch()));

    private void fetchSkin(final CommandSender commandSender, final NPC npc, final String name, @Nullable final SkinFetcher.SkinFetchListener listener) {
        Configuration.MESSAGES.sendMessage(commandSender, ConfigurationValue.FETCHING_SKIN, name);
        this.skinFetcher.fetchSkin(name, new SkinFetcher.SkinFetchListener() {
            public void onSuccess(SkinFetcherServer server, SkinProperties skinProperties) {
                npc.changeSkin(NPCSkin.forValues(skinProperties.getValue(), skinProperties.getSignature()));
                commandSender.sendMessage(ChatColor.GREEN + "Skin data received from: " + ChatColor.WHITE + server.getName());
                Configuration.MESSAGES.sendMessage(commandSender, ConfigurationValue.GET_SKIN);
                if (listener != null) {
                    listener.onSuccess(server, skinProperties);
                }
            }

            public void onError(Throwable throwable) {
                Configuration.MESSAGES.sendMessage(commandSender, ConfigurationValue.CANT_GET_SKIN, name);
            }
        });
    }

    public static final String SELECTED_NPC_MESSAGE = "Selected NPC: %d";
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
            this.fetchSkin(sender, npc, name, null);
        }
        npcManager.getSelectedNPC().put(sender, id);
    }

    @Subcommand({"select","sel"})
    @AutoComplete("@npc-id *")
    public void selectNPC(CommandSender sender, @Default("-1") int id) {
        if (id == -1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("You must specify an id");
                return;
            }
            Player player = (Player) sender;
            NPCModel targetNpc = npcManager.getTargetNpc(player);
            if (targetNpc == null) {
                sender.sendMessage(NO_NPC_FOUND);
                return;
            }
            id = targetNpc.getId();
        }
        int finalId = id;
        if (ConfigurationConstants.NPC_LIST.stream().noneMatch(npcModel -> npcModel.getId() == finalId)) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.NPC_NOT_FOUND);
            return;
        }
        npcManager.getSelectedNPC().put(sender, id);
        sender.sendMessage(String.format(SELECTED_NPC_MESSAGE, id));
    }

    @Subcommand({"delete","del","remove"})
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
    public void skinNPC(Player sender, String skinName, @Optional Integer refreshSkinDuration) {
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
        npc.getNpcPojo().setSkinName(skinName);
        this.fetchSkin(sender, npc, PlaceholderUtils.formatPlaceholders(skinName), new SkinFetcher.SkinFetchListener(){
            @Override
            public void onSuccess(SkinFetcherServer server, SkinProperties skinProperties) {
                if (refreshSkinDuration != null) {
                    npc.getNpcPojo().setRefreshSkinDuration(refreshSkinDuration);
                    sender.sendMessage(ChatColor.GREEN + "The skin will refresh every: " + ChatColor.YELLOW + refreshSkinDuration + ChatColor.GREEN + " seconds.");
                } else {
                    npc.getNpcPojo().setRefreshSkinDuration(0);
                }
            }
        });
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

    @Subcommand("action help")
    @DefaultFor(value = {"zednpc action", "zenpc action", "npc action", "znpc action"})
    public void actionHelp(CommandSender sender) {
        Audience audience = adventure.sender(sender);
        audience.sendMessage(getHelpMessage(HelpCommandType.TITLE, "ZedNPC Actions", "ZedNPC actions"));
        ActionType.helpActions(audience);
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
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.SUCCESS);
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
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.SUCCESS);
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
        if (actions.isEmpty()) {
            sender.sendMessage("No actions found");
            return;
        }
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
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.SUCCESS);
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
        ConversationService.createConversation(sender, conversationName);
    }

    @Subcommand("conversation gui")
    public void conversationGui(Player sender) {
        new ConversationsGUI(sender).open();
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
    @AutoComplete("@conversation-name *")
    public void removeConversation(Player sender, String conversationName) {
        if (!Conversation.exists(conversationName)) {
            Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.NO_CONVERSATION_FOUND);
            return;
        }

        ConfigurationConstants.NPC_CONVERSATIONS.remove(Conversation.forName(conversationName));
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.SUCCESS);
    }

    @Subcommand("conversation set")
    @AutoComplete("@conversation-name-with-clear *")
    public void setConversation(Player sender, String conversationName, @Optional ConversationModel.ConversationType type) {
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
        npc.getNpcPojo().setConversation(new ConversationModel(conversationName, type != null ? type.name() : ConversationModel.ConversationType.CLICK.name()));
        Configuration.MESSAGES.sendMessage(sender, ConfigurationValue.SUCCESS);
    }

    @Subcommand("help")
    @DefaultFor(value = {"zednpc", "zenpc", "npc", "znpc"})
    public void help(CommandSender sender) {
        Audience audience = adventure.sender(sender);
        audience.sendMessage(getHelpMessage(HelpCommandType.LINE, "", ""));
        audience.sendMessage(getHelpMessage(HelpCommandType.TITLE, "ZedNPC", "ZedNPC help"));
        audience.sendMessage(getHelpMessage(HelpCommandType.LINE, "", ""));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc create <name> <type>", "Create a new NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc select <id>", "Select an NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc delete <id>", "Delete an NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc skin <skinName>", "Change the skin of the selected NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc list", "List all NPCs"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc teleport <id>", "Teleport to an NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc move", "Move the selected NPC to your location"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc type <type>", "Change the type of the selected NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc lines <lines>", "Change the hologram lines of the selected NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc height <height>", "Change the hologram height of the selected NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc toggle <toggle> [color]", "Toggle a setting on/off for the selected NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc holo", "Toggle hologram for the selected NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc glow <color>", "Toggle glow for the selected NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc mirror", "Toggle mirror for the selected NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc look", "Toggle look for the selected NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc action add <action> <value>", "Add an action to the selected NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc action remove <index>", "Remove an action from the selected NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc action list", "List all actions of the selected NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc action cooldown <index> <cooldown>", "Set the cooldown of an action"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc equip <slot>", "Equip the selected NPC with the item in your hand"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc customize <method> [value]", "Customize the selected NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc path create <name>", "Create a new path"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc path exit", "Exit the path creator"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc path list", "List all paths"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc path set <name>", "Set the path for the selected NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc conversation create <name>", "Create a new conversation"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc conversation gui", "Open the conversation GUI"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc conversation list", "List all conversations"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc conversation remove <name>", "Remove a conversation"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc conversation set <name> [type]", "Set the conversation for the selected NPC"));
        audience.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/zednpc help", "Show this help message"));
    }

    private Component getHelpMessage(HelpCommandType type, String command, String description) {
        String onlyCommand = command.split("<")[0];
        if(onlyCommand.contains("["))
            onlyCommand = onlyCommand.split("\\[")[0];
        if(type == HelpCommandType.TITLE)
            return MiniMessageUtils.miniMessageString("<color:#D3495B><b><title></b></color>", Map.of("title", command));
        if (type == HelpCommandType.COMMAND)
            return MiniMessageUtils.miniMessageString("<click:suggest_command:'<only_command>'><color:#E9724C><u><all_command></u></color></click> <color:#E0AF79><i>- <description></i></color>\n", Map.of("only_command", onlyCommand,"all_command",command,"description", description));
        if(type == HelpCommandType.LINE)
            return MiniMessageUtils.miniMessageString("<color:#c04253>-------------------------------------</color>");
        return MiniMessageUtils.miniMessageString("<red>ERROR</red>");
    }

    public enum HelpCommandType {
        LINE,
        COMMAND,
        TITLE
    }
}
