![The Mighty Architect in action](https://i.imgur.com/4nHbxIu.gif)

# The Mighty Architectury

Design elaborate buildings within a minute. Draw a ground plan out of rooms, towers and roofs; the mod
decorates it with walls, windows, roofing and detail in the theme you picked; choose a block palette;
then print the result straight into your world or save it as a schematic.

Press **G** to get started.

A maintained port of simibubi's original
[The Mighty Architect](https://www.curseforge.com/minecraft/mc-mods/the-mighty-architect), with his themes and designs.

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
- **Servers and LAN hosts can install it too, at no cost to anyone.** Since 3.0.0 the mod adds no
  blocks or items, so players without it can still join. Printing then uses the mod's own
  packets — much faster, no command spam — and is allowed for creative-mode players or operators.
  Every placement is checked against world border, spawn protection, claims, reach and a
  per-player budget.

> **Server admins: do not run a 0.x build on a server.** Versions before 2.0.0 accepted build packets
> from any player without checking permissions. Update to 2.0.0 or newer, or simply remove the mod from the
> server — players lose nothing. If a server ever ran an older build, also check
> `/gamerule logAdminCommands`.

## Making your own themes

Press **G**, create or edit a theme, and press **1** for the exporter toolkit (creative mode). The
tools are plain vanilla items, so worlds stay compatible with players who don't have the mod: the
**Design Exporter** is a named blaze rod, the **Design Marker** is a lodestone, and **layer markers**
are glazed terracotta, one colour per trait. While you edit a theme, the mod labels them and shows
where each design goes. Worlds that still contain the pre-3.0 marker blocks lose them; exported
designs are unaffected.

## Your themes and palettes

Since 2.0.0 your own themes and palettes live in `<game folder>/mightyarchitect/`. Existing ones are
copied there on first launch; the originals are left untouched. Resource packs can now ship complete
themes under `assets/mightyarchitect/themes/<name>/`.

## Links

[Changelog](https://github.com/TimStewartJ/TheMightyArchitectury/blob/main/CHANGELOG.md) ·
[Report a bug](https://github.com/TimStewartJ/TheMightyArchitectury/issues) (please include your Minecraft version and loader) ·
[Source](https://github.com/TimStewartJ/TheMightyArchitectury)
