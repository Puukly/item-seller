# Slime Seller Mod for Minecraft 1.21.10

A client-side Fabric mod that automates selling slime balls by pressing a customizable hotkey (default: F6).

## Features

- Press F6 (configurable) to automatically execute `/sell` command
- Automatically moves all slime balls from your inventory into the opened container
- Client-side only - works on servers that have the `/sell` command
- Customizable keybinding through Minecraft's controls menu

## Requirements

- Minecraft 1.21.10
- Fabric Loader 0.16.9 or higher
- Fabric API 0.110.5+1.21.10 or higher
- Java 21

## Installation

1. Install Fabric Loader for Minecraft 1.21.10 from https://fabricmc.net/use/
2. Download and install Fabric API from https://modrinth.com/mod/fabric-api
3. Build this mod using `./gradlew build` (or `gradlew.bat build` on Windows)
4. Place the generated JAR file from `build/libs/` into your `.minecraft/mods` folder
5. Launch Minecraft with the Fabric profile

## Building from Source

### Prerequisites
- Java Development Kit (JDK) 21 or higher
- Git (optional)

### Build Steps

1. Open a terminal/command prompt in the mod directory
2. Run the build command:
   ```bash
   ./gradlew build
   ```
   On Windows:
   ```cmd
   gradlew.bat build
   ```
3. The compiled mod JAR will be in `build/libs/slimeseller-1.0.0.jar`

## Usage

1. Join a server that has the `/sell` command enabled
2. Collect slime balls in your inventory
3. Press F6 (or your custom keybinding)
4. The mod will:
   - Execute `/sell` command
   - Wait for the container to open
   - Automatically move all slime balls from your inventory to the container

## Configuration

To change the keybinding:
1. Open Minecraft
2. Go to Options → Controls → Key Binds
3. Scroll to the "Slime Seller" category
4. Click on "Sell Slime Balls" and press your desired key
5. Click "Done"

## How It Works

1. When you press the hotkey, the mod sends the `/sell` command to the server
2. The mod waits up to 2 seconds for a container GUI to open
3. Once the container opens, it scans your entire inventory for slime balls
4. All found slime balls are shift-clicked into the container automatically

## Compatibility

- Client-side only (doesn't need to be installed on the server)
- Works with any server that has a `/sell` command that opens a container
- Compatible with most other Fabric mods

## Troubleshooting

**The mod doesn't move items:**
- Make sure the `/sell` command actually opens a container/GUI
- Check that you have slime balls in your inventory
- Verify the server allows the `/sell` command

**Keybinding doesn't work:**
- Check for keybinding conflicts in Controls settings
- Make sure the key isn't bound to another action

**Build errors:**
- Ensure you have JDK 21 or higher installed
- Try running `./gradlew clean` before building
- Check that you have internet connection (Gradle needs to download dependencies)

## License

MIT License - Feel free to modify and distribute

## Support

This mod was created for Minecraft 1.21.10 specifically. If you encounter issues, make sure:
- You're running the correct Minecraft version
- Fabric Loader and Fabric API are properly installed
- The server supports the `/sell` command
