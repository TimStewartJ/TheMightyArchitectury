![logo](https://i.imgur.com/XCm5gCI.png)

[<img src="https://img.shields.io/github/license/Creators-of-Create/Create?style=flat&color=900c3f" alt="License">](https://github.com/TimStewartJ/TheMightyArchitectury/blob/main/LICENSE)

# The Mighty Architectury

A WIP port of [simibubi's](https://github.com/simibubi) [The Mighty Architect](https://github.com/simibubi/TheMightyArchitect)
to Fabric and NeoForge. No third-party multiloader framework: each loader is built with its own
official toolkit, so the mod has no runtime dependencies beyond the loader itself (plus Fabric API
on Fabric).

This is experimental. Some things may still be broken!

## Contributing

Everything lives on `main`. One source tree builds 7 Minecraft versions x 2 loaders
(14 targets) through [Stonecutter](https://stonecutter.kikugie.dev/); there are no
per-version branches. The versions are listed in `settings.gradle`, and each has its
own dependency and metadata values in `versions/<mc>/gradle.properties`.

### Layout

| directory | what it is |
| --- | --- |
| `common/` | the shared source. No toolchain of its own - Stonecutter processes it per Minecraft version and both loader modules compile the result directly (source inclusion). |
| `fabric/` | Fabric entrypoints and metadata, built with [Fabric Loom](https://github.com/FabricMC/fabric-loom). |
| `neoforge/` | NeoForge entrypoints and metadata, built with [ModDevGradle](https://github.com/neoforged/ModDevGradle). |
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
out the arms that do not apply. Two consequences worth knowing: a block comment
cannot nest, so each arm must close before the next opens; and an arm containing
only comments loses its marker and becomes live code, so never guard commentary.

```powershell
./gradlew stonecutterSwitchTo1.21.8   # switch which version the tree targets
./gradlew buildAll                     # build all 14 targets
./gradlew ":fabric:1.21.8:build"       # build one target
```

Switching rewrites the tree in place, so it leaves a cosmetic diff on files with
empty guard arms. That churn carries no behaviour and is safe to discard with
`git checkout -- .`.

Before opening a pull request, run the matrices below. CI runs the same checks per
version, and `All versions green` must pass.

Branches from before the migration are kept as tags rather than branches. `git tag -l
'archive/*'` lists them; `archive/master-1.19.4` is the pre-migration trunk.

## Runtime testing

PowerShell 7 is required.

```powershell
# All versions x both loaders
pwsh -File scripts/run-server-test-matrix.ps1
pwsh -File scripts/run-client-test-matrix.ps1

# Exact production jars in disposable Prism instances (Windows)
pwsh -File scripts/run-packaged-client-test-matrix.ps1
```

The server matrix boots a real dedicated server per target and runs the print-to-world test in
`server-test/`: a schematic is turned into `InstantPrintPacket`s, round-tripped through the
registered stream codec, applied to a real `ServerLevel`, and the resulting blocks are compared
against the schematic. Booting alone is not a pass — the runner waits for the test's verdict.

The packaged runner is artifact-first: Gradle only ever runs in
`scripts/prepare-runtime-artifacts.ps1`, which builds every requested target once
and records the resulting jars in `build/runtime-artifacts/manifest.json`. Client
launches only copy those prebuilt jars, so many clients can start without racing
concurrent builds. Use `-Build Always|Auto|Never` to control preparation;
`Auto` (the default) rebuilds only when the manifest is missing or stale.

```powershell
pwsh -File scripts/prepare-runtime-artifacts.ps1
pwsh -File scripts/run-packaged-client-test-matrix.ps1 -Build Never
```

For manual testing, add `-KeepOpen`. Targets are launched one at a time, each
running the full automated suite first and then remaining connected in-world with
the composer active, so every requested target ends up open simultaneously. Ports
are assigned sequentially from `-Port`:

```powershell
pwsh -File scripts/run-packaged-client-test-matrix.ps1 `
  -Versions 1.21.1,1.21.8,26.1 -Loaders fabric,neoforge -Port 25601 -KeepOpen
```

The command prints a session manifest per client and a stop command.
To stop every retained session:

```powershell
pwsh -File scripts/stop-kept-open-clients.ps1 -All
```
