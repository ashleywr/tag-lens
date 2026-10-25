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

<!-- ashley-release-information -->

## Ashley's builds

This is an original project by Ashley Wijnen-Riems. Original project code is offered under the [MIT License](LICENSE). Third-party material retains its own license and attribution.

Provided **AS IS**, without warranty of any kind. Maintenance, updates and support are best effort; compatibility with every modpack is not guaranteed.

### Downloads and installation

Download a runtime `.jar` from [GitHub Releases](https://github.com/ashleywr/tag-lens/releases). Use the Minecraft version and loader named in that release and install its required dependencies. Do not install `-sources`, `-dev`, `-slim`, `-common` or test JARs as the mod.

Close Minecraft, back up your world, and place the matching runtime JAR in the instance's `mods/` directory. Replace older versions of this same mod rather than loading duplicates. For content mods, install matching versions on the server and its clients unless the release explicitly says client-only or server-only.

### Building and reporting problems

Use Java 21 and the checked-in Gradle wrapper:

```powershell
.\gradlew.bat build
```

Build outputs are under `build/libs/` or each loader subproject's `build/libs/`. Some projects require the local development dependencies described above; those dependencies are not included in the download.

For a problem report, include the exact release filename, Minecraft and loader versions, relevant mod versions, `logs/latest.log`, and a crash report if one was written. Remove private information before sharing logs. Remove the JAR to uninstall; worlds containing this mod's blocks, entities or world generation may require restoring a backup.
