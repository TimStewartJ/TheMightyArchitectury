![The Mighty Architect in action](https://i.imgur.com/4nHbxIu.gif)

# The Mighty Architectury

Design elaborate buildings within a minute. Draw a ground plan out of rooms, towers and roofs; the mod
decorates it with walls, windows, roofing and detail in the theme you picked; choose a block palette;
then print the result straight into your world or save it as a schematic.

Press **G** to get started.

A maintained port of simibubi's original
[The Mighty Architect](https://www.curseforge.com/minecraft/mc-mods/the-mighty-architect), with his themes and designs.

## Drafting from anywhere

The drafting tools aim from your camera. Spectate, or use a free-camera mod such as
[Freecam](https://modrinth.com/mod/freecam), to lay out a building from above or far away, in
survival too. Lines from the outline's corners show where it meets the ground. Don't like the blue
blueprint tint? Press **B** in the menu to turn it off.

Printing still happens from where you stand. A server running the mod only places blocks within
256 blocks of you, and the mod tells you before printing if a build reaches farther.

## Versions and loaders

| Minecraft | Fabric | NeoForge | Forge |
| --- | :-: | :-: | :-: |
| 1.19.4, 1.20 – 1.20.1 | ✅ | | ✅ |
| 1.20.2 | ✅ | | |
| 1.20.3 – 1.20.6 | ✅ | ✅ | |
| 1.21.1, 1.21.4, 1.21.6, 1.21.8, 1.21.10, 1.21.11 | ✅ | ✅ | |
| 26.1, 26.2 | ✅ | ✅ | |
| 26.3 | ✅ | ✅ | |

Each file lists exactly the Minecraft versions it was built and tested for, and loaders will refuse it
on anything else — so pick the file that matches your version.

**Requires:** Fabric API on Fabric. Nothing else — **Architectury API is no longer needed** as of 2.0.0.

## Multiplayer

The mod is client-side: you can design, preview and save schematics on any server.

- **Printing into the world** needs operator permission, the same bar as `/setblock`. On a server
  without the mod it places blocks with ordinary commands, batched and rate-limited.
- **Servers and LAN hosts can install it too.** Since 3.0.0, players without the mod can still
  join. With it on the server, printing is faster, skips the chat spam and also works for players
  in creative mode. Placements still respect the world border, spawn protection, claims and reach.

> **Server admins: do not run a 0.x build on a server.** Versions before 2.0.0 accepted build packets
> from any player without checking permissions. Update to 2.0.0 or newer, or simply remove the mod from the
> server — players lose nothing. If a server ever ran an older build, also check
> `/gamerule logAdminCommands`.

## Making themes

Press **G**, edit a theme, then **1** for the exporter toolkit (creative mode). The tools are
vanilla items: a blaze rod exports, a lodestone marks each design, and glazed terracotta marks its
layers. The mod labels them and outlines each design while you work.

Coming from 2.x? The old marker blocks disappear from your worlds. Exported designs are fine.

## Your themes and palettes

Since 2.0.0 your own themes and palettes live in `<game folder>/mightyarchitect/`. Existing ones are
copied there on first launch; the originals are left untouched. Resource packs can now ship complete
themes under `assets/mightyarchitect/themes/<name>/`.

## Links

[Changelog](https://github.com/TimStewartJ/TheMightyArchitectury/blob/main/CHANGELOG.md) ·
[Report a bug](https://github.com/TimStewartJ/TheMightyArchitectury/issues) (please include your Minecraft version and loader) ·
[Source](https://github.com/TimStewartJ/TheMightyArchitectury)
