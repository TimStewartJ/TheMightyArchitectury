![logo](https://i.imgur.com/XCm5gCI.png)

[<img src="https://img.shields.io/github/license/Creators-of-Create/Create?style=flat&color=900c3f" alt="License">](https://github.com/TimStewartJ/TheMightyArchitectury/blob/master/LICENSE)

# The Mighty Architectury

A WIP port of [simibubi's](https://github.com/simibubi) [The Mighty Architect](https://github.com/simibubi/TheMightyArchitect) to an [Architectury](https://github.com/architectury/architectury-api) project for Forge + Fabric compatibility. Requires [Architectury API](https://www.curseforge.com/minecraft/mc-mods/architectury-api).

This is experimental. Some things may still be broken!

## Runtime testing

PowerShell 7 is required.

```powershell
# All versions x both loaders
pwsh -File scripts/run-server-test-matrix.ps1
pwsh -File scripts/run-client-test-matrix.ps1

# Exact production jars in disposable Prism instances (Windows)
pwsh -File scripts/run-packaged-client-test-matrix.ps1
```

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
