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

Next steps
- Add textures or tile atlas for nicer visuals
- Implement simple world features (trees, caves)
- Add inventory counts and block drops

What's new (recent changes)
- Endless horizontal world: terrain is generated in chunks on demand so the world appears effectively infinite horizontally.
- Camera locked to player: the view is centered on the player and follows movement.
- Visible-tile rendering: only tiles within the player's view are rendered for better performance.
- Persistent modifications overlay: blocks placed/broken by the player are tracked so changes persist as you move.

Performance & notes
- The vertical world remains finite (fixed number of tile rows) while horizontal generation is chunked and deterministic.
- Chunk generation is deterministic (seeded) so the same areas regenerate the same terrain unless you modify them.
- You may need to click the game window once to ensure it has keyboard focus.
- If you see rendering stutter while new chunks are generated, let me know — I can add background generation or chunk unloading.

Next steps (suggested)
- Add textures or a tile atlas for nicer visuals.
- Implement item drops and inventory counts when breaking blocks.
- Add chunk unloading to free memory for very long play sessions.
- Improve terrain noise and add caves/biomes.

If you'd like, I can run the game here and report behavior, or implement any of the next steps — tell me which one to do next.
