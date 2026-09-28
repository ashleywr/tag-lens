# Tag Lens

Tag Lens is a client-side NeoForge 1.21.1 mod that summarizes the item tags in
the inventory currently on screen. Tags are ranked by the number of occupied
slots that use them, making it easy to discover useful routing tags from an
already well-organized chest.

## Behavior

- Container screens analyze the opened container and ignore the player's own
  inventory slots.
- The player inventory screen analyzes the player inventory.
- Each occupied slot counts once, regardless of stack size.
- Entries show `matching slots / occupied slots` and are sorted by frequency.
- A green entry is shared by every occupied slot in the analyzed inventory.
- Scroll over the panel to browse the full list.
- Hover a tag to see its full identifier and click it to copy that identifier.

The mod is client-side only and does not need to be installed on a server.

## Build

Requires Java 21.

```powershell
.\gradlew.bat build
```

The built JAR is written to `build/libs/`.
