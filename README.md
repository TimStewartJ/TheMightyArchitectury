![logo](https://i.imgur.com/XCm5gCI.png)

[<img src="https://img.shields.io/github/license/Creators-of-Create/Create?style=flat&color=900c3f" alt="License">](https://github.com/TimStewartJ/TheMightyArchitectury/blob/main/LICENSE)

# The Mighty Architectury

A WIP port of [simibubi's](https://github.com/simibubi) [The Mighty Architect](https://github.com/simibubi/TheMightyArchitect)
to Fabric and NeoForge. No third-party multiloader framework: each loader is built with its own
official toolkit, so the mod has no runtime dependencies beyond the loader itself (plus Fabric API
on Fabric).

This is experimental. Some things may still be broken!

## Contributing

Everything lives on `main`. One source tree builds 13 Minecraft versions - 25 jars - through
[Stonecutter](https://stonecutter.kikugie.dev/); there are no per-version branches. The versions are
listed in `settings.gradle`, and each has its own dependency and metadata values in
`versions/<mc>/gradle.properties`.

Every version ships a Fabric jar. The second loader depends on the era:

| versions | second loader | why |
| --- | --- | --- |
| 1.19.4, 1.20.1 | Forge | NeoForge did not exist yet; NeoForge 1.20.1 loads Forge mods anyway |
| 1.20.2 | *none* | ModDevGradle resolves NeoForge through Gradle module metadata, and no 20.2 or 20.3 build ever published any, so NeoForge cannot be built for it at all |
| 1.20.4 and newer | NeoForge | |

### Layout

| directory | what it is |
| --- | --- |
| `common/` | the shared source. No toolchain of its own - Stonecutter processes it per Minecraft version and both loader modules compile the result directly (source inclusion). |
| `fabric/` | Fabric entrypoints and metadata, built with [Fabric Loom](https://github.com/FabricMC/fabric-loom). |
| `neoforge/` | NeoForge entrypoints and metadata, built with [ModDevGradle](https://github.com/neoforged/ModDevGradle). |
| `forge/` | Forge entrypoints and metadata for 1.19.4 and 1.20.1, built with [ModDevGradle's legacy plugin](https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md). |
| `client-test/` | the automated in-game client test companion mod. |
| `server-test/` | the automated dedicated-server test companion mod (print-to-world). |

This is Stonecutter's recommended
[split-buildscript setup](https://stonecutter.kikugie.dev/wiki/tips/multiloader#split-buildscript):
one buildscript per loader, each using that loader's own moddev plugin.

Client tick, HUD and raw input hooks live in `common/.../mixin` as Mixins on vanilla classes, which
both loaders support - so there is one implementation rather than one per loader per version.
Everything else that genuinely differs between loaders (registration, networking, key mappings,
world render events) is behind the small interfaces in `common/.../platform` and implemented in each
loader module.

Note that the shared source is compiled against the *NeoForge-patched* Minecraft in the NeoForge
module, so a handful of vanilla signatures NeoForge widens (for example `Level#dragonParts`) have to
be written so they compile both ways.

Write code once. Where an API genuinely differs between versions, guard just the
differing lines rather than forking the file:

```java
//? if >=1.21.6 {
public void drawPassive(GuiGraphics graphics, float partialTicks) {
//?} else {
/*public void drawPassive(GuiGraphics graphics, float partialTicks) {
*///?}
    draw(graphics, partialTicks);   // shared by every version
}
```

Stonecutter rewrites these in place when you switch the active version, commenting
out the arms that do not apply. Three consequences worth knowing: a block comment
cannot nest, so each arm must close before the next opens; an arm containing only
comments loses its marker and becomes live code, so never guard commentary; and a
`//` comment placed between an arm's marker and its `/*` body loses its marker the
same way, so keep such comments inside the body.

```powershell
./gradlew stonecutterSwitchTo1.21.8   # switch which version the tree targets
./gradlew compileAll                   # compile every node and source set - the local inner loop
./gradlew buildAll                     # build all 25 jars
./gradlew ":fabric:1.21.8:build"       # build one target
./gradlew ":forge:1.20.1:build"        # the Forge branch only covers 1.19.4 and 1.20.1
```

Prefer `compileAll` locally and let CI run the matrices: it validates every version in
parallel on clean runners, where a full local matrix takes hours and collides with other
worktrees on ports and Gradle daemons.

Switching rewrites the tree in place, so it leaves a cosmetic diff on files with
empty guard arms. That churn carries no behaviour and is safe to discard with
`git checkout -- .`.

Before opening a pull request, run the matrices below. CI runs the same checks per
version, and `All versions green` must pass.

Branches from before the migration are kept as tags rather than branches. `git tag -l
'archive/*'` lists them; `archive/master-1.19.4` is the pre-migration trunk.

## Runtime testing

PowerShell 7 is required. The matrices cover every version and each version's loaders;
`-Loaders` narrows them, and a version with no NeoForge build runs Forge, or Fabric alone.

```powershell
# All versions x their loaders
pwsh -File scripts/run-server-test-matrix.ps1
pwsh -File scripts/run-client-test-matrix.ps1

# The same client harness against the packaged jars, every version and loader
pwsh -File scripts/run-client-test-matrix.ps1 -Mode prod
```

`-Mode prod` runs the identical harness, but launched the way a launcher launches the game:
packaged artifacts, remapped or reobfuscated names, mods discovered from a jar rather than a
classpath. It needs no account and no installed launcher, so it runs in CI alongside everything
else, on all 25 targets.

Two launchers sit behind it, because no single one covers every loader:

| loader | launcher | why |
| --- | --- | --- |
| Fabric | Loom's `ClientProductionRunTask` | part of the toolchain already; resolves the client jar, Fabric Loader, intermediary and the runtime libraries itself |
| NeoForge, Forge | HeadlessMc (MIT) | ModDevGradle ships no production run task, and NeoForge keeps its own production-test tasks inside an unpublished `buildSrc` plugin |

HeadlessMc is used purely as an installer and launcher — it installs a real Minecraft plus the
**pinned** loader build the node targets (`--uid`, so an upstream release cannot change what the
matrix means) and launches the jars out of a `mods` folder. Nothing it does reaches a shipped
artifact. Its Minecraft lives in `build/headlessmc` and is cached in CI per version.

> **Local caveat.** The Forge-family production lane needs a virtual framebuffer, so it runs on
> Linux/CI only. HeadlessMc forces its LWJGL stub for offline accounts unless Xvfb is present, and a
> stubbed renderer produces empty framebuffers that fail every screenshot assertion. The Fabric
> production lane has no such constraint and runs anywhere, including `-KeepOpen`.

The server matrix boots a real dedicated server per target and runs the print-to-world test in
`server-test/`: a schematic is turned into `InstantPrintPacket`s, round-tripped through the
registered stream codec, applied to a real `ServerLevel`, and the resulting blocks are compared
against the schematic. Booting alone is not a pass — the runner waits for the test's verdict.

The same run covers the serverbound authorization gate in `ServerBuildGuard`: the decode-time
bounds on both packets, the per-player placement budget and the reach test, and then the gate end
to end — a real packet handed to the real handler with a real `ServerPlayer`, asserting that an
authorised sender's build lands and that an out-of-reach or unauthorised one does not.

For manual testing, add `-KeepOpen` — in either mode. The target runs the full automated suite
first and then stays connected in-world with the composer active. `-Mode prod -KeepOpen` is the way
to play with the actual distributable jars:

```powershell
pwsh -File scripts/run-client-test-matrix.ps1 -Mode prod -Versions 1.21.8 -Port 25601 -KeepOpen
```

The command prints a session manifest and a stop command.
To stop every retained session:

```powershell
pwsh -File scripts/stop-kept-open-clients.ps1 -All
```

## Releasing

Publishing goes through [mod-publish-plugin](https://github.com/modmuss50/mod-publish-plugin),
applied to every loader node by `buildSrc/src/main/groovy/mightyarchitect.publish.gradle`. Each
node's listing metadata lives next to its toolchain values in `versions/<mc>/gradle.properties`:

| property | meaning |
| --- | --- |
| `meta_game_versions` | the **inclusive** list of Minecraft versions the listing declares. Written out explicitly because `meta_mc_max` is *exclusive*; `checkPublishMetadata` (part of `check`, so part of every CI build) fails if any listed version falls outside the jar's own `[meta_mc_range, meta_mc_max)` |
| `meta_release_channel` | `release`, `beta` or `alpha` for that node's uploads |

Release notes are the top section of `CHANGELOG.md`. The version number of each upload is the
jar's own version plus a loader suffix (`2.0.0+mc1.21.1-fabric`), one upload per jar, as before.

```powershell
./gradlew :fabric:1.21.1:publishMods   # dry run: writes build/publishMods/ for review, uploads nothing
./gradlew publishAll                   # the same for all 25 jars
./gradlew publishAll -PpublishLive=true # real upload; needs MODRINTH_TOKEN (and CURSEFORGE_TOKEN)
```

In CI, `.github/workflows/release.yml` runs the whole matrix on the commit, then a dry run of all 25
publications whose resolved metadata is tabulated in the run summary (`workflow_dispatch` on any
ref stops here). Pushing a `v<mod_version>` tag continues to the live upload from the `release`
environment, which is where the tokens belong and where a required reviewer makes the upload a
manual gate. A tag that does not match `mod_version`, or a `CHANGELOG.md` still headed
`[Unreleased]`, refuses to publish.
