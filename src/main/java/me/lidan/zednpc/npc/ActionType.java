package me.lidan.zednpc.npc;

import io.github.gonalez.znpcs.utility.Utils;
import me.clip.placeholderapi.PlaceholderAPI;
import me.lidan.zednpc.utils.MiniMessageUtils;
import net.kyori.adventure.audience.Audience;

public enum ActionType {
    CMD,CONSOLE,CHAT,MESSAGE,SERVER;

    public String toString() {
        return this.name().toLowerCase();
    }

    public static void helpActions(Audience audience) {
        audience.sendMessage(MiniMessageUtils.miniMessage("<green>Available Actions: <yellow><bold><hover:show_text:'<gold>Make player run command <yellow>CMD warp pvp'>CMD</hover> <hover:show_text:'<gold>Make console run command <yellow>CONSOLE give * diamond'>CONSOLE</hover> <hover:show_text:'<gold>Make player send chat message <yellow>CHAT come spawn'>CHAT</hover> <hover:show_text:'<gold>Send message in chat <yellow>MESSAGE this is the server'>MESSAGE</hover> <hover:show_text:'<gold>Send player to other server (proxy only) <yellow>SERVER lobby'>SERVER</hover> <hover:show_text:'<gold>Hover over actions to learn more'><gold><bold>HOVER FOR HELP</hover>"));
        if (Utils.PLACEHOLDER_SUPPORT && PlaceholderAPI.isRegistered("Player")){
            audience.sendMessage(MiniMessageUtils.miniMessage("<green>Available Placeholders: <yellow><bold><hover:show_text:'<gold>Player name'>%player_name%</hover> <hover:show_text:'<gold>Player uuid'>%player_uuid%</hover> <hover:show_text:'<gold>Player display name'>%player_displayname%</hover> <hover:show_text:'<gold>Player world'>%player_world%</hover> <hover:show_text:'<gold>Click to learn more'><click:open_url:'https://wiki.placeholderapi.com/'><gold>PlaceholderAPI Supported!</gold></click></hover>"));
        }
    }
}
