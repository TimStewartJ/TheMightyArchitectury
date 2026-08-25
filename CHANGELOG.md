# Changelog

All notable changes to The Mighty Architectury. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). One entry covers every Minecraft version
the release ships for; version-specific notes say which versions they apply to.

## [Unreleased]

<!-- DECISIONS STILL OPEN — resolve before this file is fed to Modrinth/CurseForge:
     1. Version number and channel: the heading above becomes the chosen version, e.g. [2.0.0].
     2. 1.19.2 / 1.19.3: the "Removed" entry below assumes they are end-of-life. If a 1.19.2 build
        is added instead, delete that entry and list 1.19.2 under "Added". -->

This release supersedes every previously published jar from 0.8.0 (1.19.4) through 0.11.0 (1.21.11)
and is the first built from the single-branch, multi-version codebase. It ships for 13 Minecraft
versions — 1.19.4, 1.20.1, 1.20.2, 1.20.4, 1.20.6, 1.21.1, 1.21.4, 1.21.6, 1.21.8, 1.21.10, 1.21.11,
26.1 and 26.2 — on Fabric everywhere, NeoForge from 1.20.4, and Forge on 1.19.4 and 1.20.1. 1.20.2
is Fabric-only: no NeoForge 20.2 build was ever published with the metadata the toolchain needs.
Architectury API is no longer required; Fabric builds still need Fabric API.

### Security — update now if you run a server with the mod installed

- **Serverbound packets are now authorized.** Every earlier version let any client carrying the
  mod rewrite loaded chunks and place signs anywhere on any server that also had it — no gamemode,
  permission, distance or size check. Building now requires creative mode or permission level 2,
  and every placement is checked against world bounds, loaded chunks, a 256-block reach, the world
  border, spawn protection and claim mods (`Level#mayInteract`), and a per-player placement budget.
  Packet sizes are validated before anything is allocated.
  ([#35](https://github.com/TimStewartJ/TheMightyArchitectury/pull/35))
- Multiplayer printing no longer changes `sendCommandFeedback` or `logAdminCommands`. Earlier
  versions turned both off while printing and only restored them if the print finished, so a
  crash or disconnect mid-print left the server without admin-command logging. **The update does
  not repair that automatically**: if a server ever ran an older version, check
  `/gamerule logAdminCommands`.
- Saving themes, palettes, designs and schematics is atomic. A crash during a save no longer
  destroys the file being written.

### Fixed

- Exported designs named from a sign are no longer saved as `SignText@…` on 1.20 and newer.
  Sign naming works again and re-exporting a design overwrites it instead of leaving an orphan.
  ([#38](https://github.com/TimStewartJ/TheMightyArchitectury/pull/38))
- Extending a built-in theme with your own designs no longer deletes the built-in designs in the
  same layer.
- Editing one theme's palette no longer changes the default palette for every other theme.
- Palettes and themes load outside a world; the title screen no longer crashes on them.
- One malformed palette file, or one unknown theme enum value, no longer prevents every other
  palette or theme from loading. Failures are logged and skipped.
- Exporting a theme no longer fails silently when its export folder is missing.
- Theme and design names are sanitized before they become file names (path separators, `..`,
  reserved Windows device names).
- The composer's layout HUD is visible again on 1.21.1 and 1.21.4
  ([#26](https://github.com/TimStewartJ/TheMightyArchitectury/issues/26)).
- Opening the palette picker on NeoForge no longer crashes with `IllegalAccessError`
  ([#27](https://github.com/TimStewartJ/TheMightyArchitectury/issues/27)).
- The composer HUD renders on 1.21.6, 1.21.8, 1.21.10 and 1.21.11.
- Item model definitions are included on 1.21.4 and 1.21.6, where items need them to render.
- Palette previews render from 1.21.6 onward.
- World-space measurement labels are drawn on a backdrop their text contrasts with, on every
  version ([#34](https://github.com/TimStewartJ/TheMightyArchitectury/pull/34)).
- The theme-settings screen's name and author fields can be typed into, and resizing the window
  no longer duplicates its widgets ([#36](https://github.com/TimStewartJ/TheMightyArchitectury/pull/36)).
- The tool bar no longer leaks its tint into whatever vanilla draws next.
- Resource-pack formats are correct on 1.21.4, 1.21.10 and 1.21.11 (the jars claimed the wrong
  `pack_format`).
- Each jar now declares a bounded Minecraft range instead of an open-ended one, so installing a
  jar on a version it was not built for is refused by the loader instead of crashing in game. The
  1.21.1, 1.21.4, 1.21.6 and 1.21.8 jars no longer claim 1.21.2–1.21.3, 1.21.5, 1.21.7 or 1.21.9,
  where they would fail (renamed or removed render, registry and input APIs, and a NeoForge
  packet-sender move during its 21.7 line).
- Large builds no longer stall the client when you change palettes or re-roll. The work is spread
  across ticks and the previous preview stays visible until the replacement is ready
  ([#42](https://github.com/TimStewartJ/TheMightyArchitectury/pull/42)).
- Preview textures no longer go stale after a resource reload (F3+T).
- The same sketch now materializes identically every time; block randomization is seeded.

### Changed

- **Your themes and palettes now live in `<game directory>/mightyarchitect/`** instead of three
  generic folders in the instance root ([#41](https://github.com/TimStewartJ/TheMightyArchitectury/pull/41)).
  On first launch the mod *copies* `themes/` and `palettes/` there; the originals are left exactly
  where they were and are still read, so nothing is lost if something goes wrong. `schematics/` is
  deliberately not moved — it is shared with Create. Note that files you create *after* updating
  are written only to the new location, so going back to an older version will not see them.
- Theme, palette and design files are read and written with proper codecs instead of
  string round-tripping. Every existing file still loads; new files carry a `DataVersion`.
- Multiplayer printing on a server that has the mod now uses the mod's own packets whenever the
  connection advertises them, so it no longer spams command feedback or loses blocks to command
  limits ([#43](https://github.com/TimStewartJ/TheMightyArchitectury/pull/43)).
- On a vanilla or mod-less server, printing still falls back to commands, but more safely: block
  states are serialized the way `/setblock` expects, short runs are batched into bounded `/fill`
  commands, at most one command is sent per tick, and each batch is re-planned against the current
  world right before it is sent. Blocks that already match are left untouched.
- Palette names sort naturally in the picker (`p2` before `p10`).
- The mod list shows the mod's icon and links to Modrinth and the issue tracker on every loader.

### Added

- Support for Minecraft 1.20.2, 1.20.4, 1.20.6, 1.21.8, 26.1 and 26.2.
- **Resource packs can ship complete themes.** Put a theme under
  `assets/mightyarchitect/themes/<name>/theme.json` and it appears in the theme list; packs can also
  override the built-in themes and palettes, and F3+T reloads them. `theme.json` gained an optional
  `HeightSequence` so a pack-provided theme can define its own floor heights.

### Removed

- Support for Minecraft 1.19.2 and 1.19.3. The last builds for those versions (0.6.2 and 0.7.0)
  predate the server-side authorization fix above and will not be updated.
- The Architectury API dependency.

### Internal

No player-visible change relative to the last published jars; listed because they change how the
jars are built and verified.

- Every jar is now built from one source tree with [Stonecutter](https://stonecutter.kikugie.dev/)
  and each loader's own toolchain (Fabric Loom, ModDevGradle); there are no per-version branches.
- Every release build runs a JUnit suite, a dedicated-server print test and a client test against
  both the development classpath and the packaged jar, on all 25 targets.
- The blueprint post-effect is reached through an accessor mixin rather than reflection that
  matched a vanilla method by shape, and the legacy-Forge mixin configs name their refmap
  ([#36](https://github.com/TimStewartJ/TheMightyArchitectury/pull/36),
  [#37](https://github.com/TimStewartJ/TheMightyArchitectury/pull/37)).
