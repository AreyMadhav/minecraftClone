# MinecraftClone (minimal 2D)

A small 2D Minecraft-like prototype written in Java Swing.

Features
- Tile-based world (grass, dirt, stone)
- Player with basic physics (move, jump, collision)
- Break and place blocks with the mouse
- Hotbar (3 slots) selectable with keys 1–3

Build & run

1. Compile with Maven (requires JDK 17):

```powershell
mvn -DskipTests=true clean compile
```

2. Run the game:

```powershell
java -cp target/classes com.minecraftclone.minecraftclone.Main
```

Controls
- Move: `A`/`D` or `←`/`→`
- Jump: `Space`, `W`, or `↑`
- Break block: Left mouse button
- Place block: Right mouse button
- Select block: `1` = Grass, `2` = Dirt, `3` = Stone

Notes
- The project targets Java 17. If you see UnsupportedClassVersionError, install or use a Java 17 runtime.
- Quick code pointers:
  - Main window: `src/main/java/com/minecraftclone/minecraftclone/Main.java`
  - Game loop & rendering: `src/main/java/com/minecraftclone/minecraftclone/GamePanel.java`
  - Player physics: `src/main/java/com/minecraftclone/minecraftclone/Player.java`
  - Block model: `src/main/java/com/minecraftclone/minecraftclone/Block.java`

What's new (recent changes)
- Endless horizontal world: terrain is generated in chunks on demand so the world appears effectively infinite horizontally.
- Camera locked to player: the view is centered on the player and follows movement.
- Visible-tile rendering: only tiles within the player's view are rendered for better performance.
- Persistent modifications overlay: blocks placed/broken by the player are tracked so changes persist as you move.

### Screenshots
<img width="780" height="585" alt="image" src="https://github.com/user-attachments/assets/d43ec1e0-9936-4c9b-889b-2ebcca8904a3" />


