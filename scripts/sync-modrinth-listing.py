#!/usr/bin/env python3
"""Keep the Modrinth project page in step with .github/modrinth/.

    python scripts/sync-modrinth-listing.py            # show what would change; needs no token
    python scripts/sync-modrinth-listing.py --apply    # change it; needs MODRINTH_TOKEN

The page is described by .github/modrinth/project.json (title, summary, links) and
.github/modrinth/description.md (the body). Only those fields are ever sent. The client/server
environment in particular is not: Modrinth stores it per version, the release pipeline sets it on
every upload, and the 0.x versions' `client_only` is what keeps modpack tools from putting their
unauthorised build packets on servers. A project-level change there would be wrong for both.

--apply sends only the fields that differ, then reads the page back and fails unless every field
now matches. Needs a Modrinth token with the "Write projects" scope. Standard library only.
"""
import argparse
import difflib
import json
import os
import pathlib
import sys
import urllib.error
import urllib.request

PROJECT = "the-mighty-architectury"
API = "https://api.modrinth.com/v2"
AGENT = "TimStewartJ/TheMightyArchitectury listing-sync (github.com/TimStewartJ/TheMightyArchitectury)"
# Repository name -> Modrinth field. Nothing outside this map can reach the API.
FIELDS = {"title": "title", "summary": "description", "issues_url": "issues_url",
          "source_url": "source_url", "wiki_url": "wiki_url", "discord_url": "discord_url",
          "body": "body"}


def request(method, path, token=None, payload=None):
    headers = {"User-Agent": AGENT}
    data = None
    if token:
        headers["Authorization"] = token
    if payload is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(payload).encode()
    req = urllib.request.Request(API + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            text = response.read().decode()
            return json.loads(text) if text else None
    except urllib.error.HTTPError as error:
        sys.exit(f"{method} {path}: HTTP {error.code} {error.read().decode(errors='replace')[:500]}")


def normalise(value):
    return (value or "").replace("\r\n", "\n").rstrip()


def desired_state(root):
    config = json.loads((root / "project.json").read_text(encoding="utf-8"))
    unknown = set(config) - set(FIELDS) - {"//"}
    if unknown:
        sys.exit(f"project.json has fields this script does not sync: {sorted(unknown)}")
    wanted = {FIELDS[key]: normalise(value) for key, value in config.items() if key in FIELDS}
    wanted["body"] = normalise((root / "description.md").read_text(encoding="utf-8"))
    return wanted


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--apply", action="store_true", help="change the live page")
    args = parser.parse_args()

    root = pathlib.Path(__file__).resolve().parent.parent / ".github" / "modrinth"
    wanted = desired_state(root)
    live = request("GET", f"/project/{PROJECT}")
    changed = {field: value for field, value in wanted.items() if normalise(live.get(field)) != value}

    lines = [f"Modrinth listing for {PROJECT}: {len(changed)} field(s) differ from the live page."]
    for field, value in changed.items():
        diff = difflib.unified_diff(normalise(live.get(field)).splitlines(), value.splitlines(),
                                    f"live/{field}", f"repo/{field}", lineterm="")
        lines += ["", f"### {field}", "```diff", *diff, "```"]
    report = "\n".join(lines)
    print(report)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as summary:
            summary.write(report + "\n")

    if not args.apply or not changed:
        return
    token = os.environ.get("MODRINTH_TOKEN")
    if not token:
        sys.exit("--apply needs MODRINTH_TOKEN")
    request("PATCH", f"/project/{live['id']}", token, changed)

    after = request("GET", f"/project/{live['id']}")
    still = [field for field, value in changed.items() if normalise(after.get(field)) != value]
    if still:
        sys.exit(f"Modrinth accepted the change but these fields do not match afterwards: {still}")
    print(f"\nApplied and verified: {', '.join(changed)}")


if __name__ == "__main__":
    main()
