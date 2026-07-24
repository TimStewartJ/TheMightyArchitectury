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

For manual testing, launch exactly one target with `-KeepOpen`. The full automated
suite runs first, then Minecraft remains connected in-world with the composer active:

```powershell
pwsh -File scripts/run-packaged-client-test-matrix.ps1 `
  -Versions 1.21.8 -Loaders neoforge -Port 25575 -KeepOpen
```

The command prints a session manifest and its matching stop command. Multiple manual
clients can run simultaneously by launching separate invocations with distinct ports.
To stop every retained session:

```powershell
pwsh -File scripts/stop-kept-open-clients.ps1 -All
```
