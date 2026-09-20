#!/usr/bin/env python3
"""Fill one directory with every asset object the project's Minecraft versions need.

    python scripts/fetch-minecraft-assets.py <target-directory>

Asset objects are named by their SHA-1 and shared between Minecraft versions: the fourteen versions
built here ask for 7.7 GB between them, and their union is 1.3 GB. CI keeps that union in one cache
entry and points every launcher's asset directory at it (.github/actions/minecraft-assets), instead
of letting Loom, NeoForge's runtime and HeadlessMc each download their own copy in every job - which
was most of what a build asked of Mojang's CDN, and most of what failed when it hiccuped.

Only `objects/` is written. Each launcher fetches its own index file and then finds its objects
already present; one that is missing is simply downloaded by that launcher, so nothing depends on
this having run. Objects no version needs any more are removed. Standard library only.
"""
import concurrent.futures
import hashlib
import json
import pathlib
import re
import sys
import time
import urllib.request

MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
OBJECTS = "https://resources.download.minecraft.net"


def fetch(url):
    for attempt in range(5):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": "TheMightyArchitectury-ci"})
            with urllib.request.urlopen(request, timeout=60) as response:
                return response.read()
        except OSError:
            if attempt == 4:
                raise
            time.sleep(2 ** attempt)


def main(target):
    repo = pathlib.Path(__file__).resolve().parent.parent
    versions = sorted({re.search(r"^minecraft_version=(\S+)", path.read_text(), re.M)[1]
                       for path in repo.glob("versions/*/gradle.properties")})
    manifest = {entry["id"]: entry["url"] for entry in json.loads(fetch(MANIFEST))["versions"]}

    wanted = {}
    for version in versions:
        index = json.loads(fetch(json.loads(fetch(manifest[version]))["assetIndex"]["url"]))
        wanted.update((entry["hash"], entry["size"]) for entry in index["objects"].values())

    objects = pathlib.Path(target, "objects")
    present = {path.name: path for path in objects.glob("*/*")}
    for name in present.keys() - wanted.keys():
        present[name].unlink()
    missing = [name for name in wanted if name not in present]

    def download(name):
        data = fetch(f"{OBJECTS}/{name[:2]}/{name}")
        if hashlib.sha1(data).hexdigest() != name:
            raise OSError(f"{name} arrived with the wrong hash")
        path = objects / name[:2] / name
        path.parent.mkdir(parents=True, exist_ok=True)
        partial = path.with_suffix(".part")
        partial.write_bytes(data)
        partial.replace(path)

    with concurrent.futures.ThreadPoolExecutor(32) as pool:
        list(pool.map(download, missing))

    print(f"{len(versions)} Minecraft versions, {len(wanted)} objects, {sum(wanted.values()) / 1e6:.0f} MB: "
          f"{len(missing)} downloaded, {len(present.keys() - wanted.keys())} removed")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
