# ZedNPC Plugin

ZedNPC is a Minecraft plugin designed to improve the user experience (UX) of the ZNPCs command system and GUI system by providing a more intuitive and user-friendly interface, similar to the Citizens plugin. This plugin acts as a command wrapper/addon for the ZNPCs system, simplifying interactions without extending its core functionality.

## Features

- **Simplified Command System**: Provides a cleaner and more intuitive command structure for managing NPCs.
- **Path Management**: Create, list, and assign paths to NPCs with ease.
- **Conversation System**: Manage NPC conversations with commands or a GUI.
- **NPC Customization**: Change NPC types, skins, holograms, and more.
- **Action Management**: Add, remove, and list NPC actions with cooldown support.
- **Auto-Completion**: Command suggestions for faster and error-free usage.
- **Toggleable Settings**: Easily toggle NPC features like glow, holograms, and more.

## Commands

### NPC Management
- `/zednpc create <name> <type>`: Create a new NPC.
- `/zednpc select <id>`: Select an NPC by ID.
- `/zednpc delete <id>`: Delete an NPC.
- `/zednpc list`: List all NPCs.
- `/zednpc teleport <id>`: Teleport to an NPC.
- `/zednpc move`: Move the selected NPC to your location.
- `/zednpc type <type>`: Change the type of the selected NPC.

### Customization
- `/zednpc skin <skinName>`: Change the skin of the selected NPC.
- `/zednpc lines <lines>`: Change the hologram lines of the selected NPC.
- `/zednpc height <height>`: Change the hologram height of the selected NPC.
- `/zednpc toggle <toggle> [color]`: Toggle a setting (e.g., glow, hologram) for the selected NPC.

### Actions
- `/zednpc action add <action> <value>`: Add an action to the selected NPC.
- `/zednpc action remove <index>`: Remove an action from the selected NPC.
- `/zednpc action list`: List all actions of the selected NPC.
- `/zednpc action cooldown <index> <cooldown>`: Set the cooldown for an action.

### Path Management
- `/zednpc path create <name>`: Create a new path.
- `/zednpc path exit`: Exit the path creator.
- `/zednpc path list`: List all paths.
- `/zednpc path set <name>`: Assign a path to the selected NPC.

### Conversations
- `/zednpc conversation create <name>`: Create a new conversation.
- `/zednpc conversation gui`: Open the conversation GUI.
- `/zednpc conversation list`: List all conversations.
- `/zednpc conversation remove <name>`: Remove a conversation.
- `/zednpc conversation set <name> [type]`: Assign a conversation to the selected NPC.

### Help
- `/zednpc help`: Display the help menu with all available commands.

### Permissions
- `zednpc.admin`: Access to all commands.

## Installation

1. Download the plugin `.jar` file.
2. Place the `.jar` file in your server's `plugins` folder.
3. Restart or reload your server.
4. The plugin will be ready to use!

## Requirements

- **Minecraft Version**: 1.16 or higher
- **ZNPCs Plugin**: Required as this plugin wraps its functionality
- **Java Version**: 11 or higher

## Development

### Build

Put the original ZNPCs plugin in the `libs` folder.
Can be found [here](https://www.spigotmc.org/resources/znpcs.80940/download?version=569796) or [here](https://github.com/lidanthedev/ZedNpc/releases/download/v0.2.0-alpha/znpcs-5.0.jar)

This project uses Gradle for building. To build the plugin:

```bash
./gradlew build
```

## Contributing
Contributions are welcome! Feel free to open issues or submit pull requests to improve the plugin.

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
