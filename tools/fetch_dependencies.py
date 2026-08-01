#!/usr/bin/env python3
"""
Fetches the dependency jars this project builds against into libs/.

The jars are NOT committed: Sable is PolyForm Shield licensed and redistributing it in this
repository would be inappropriate, so they are downloaded from the official Modrinth releases
instead. Sable Companion, Veil and the Rapier physics backend are JarJar'd inside the Sable jar
and are extracted from it.

Run:  python3 tools/fetch_dependencies.py
"""

import json
import os
import sys
import urllib.request
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LIBS = os.path.join(ROOT, "libs")

# project slug -> exact version this addon is built and tested against
WANTED = {
    "sable": "2.0.3+mc1.21.1",
    "create-aeronautics": "1.3.0+mc1.21.1",
}

# nested jars to pull out of the Sable jar, mapped to the names build.gradle expects
NESTED = {
    "sable-companion-common-1.21.1-1.6.0.jar": "sable-companion-common-1.21.1-1.6.0.jar",
    "veil-neoforge-1.21.1-4.1.4.jar": "veil-neoforge-1.21.1-4.1.4.jar",
    "dev.ryanhcode.sable.sable-sable_rapier-1.21.1-2.0.3.jar": "sable-rapier-1.21.1-2.0.3.jar",
}


def get_json(url):
    with urllib.request.urlopen(url, timeout=90) as r:
        return json.load(r)


def download(url, dest):
    with urllib.request.urlopen(url, timeout=600) as r, open(dest, "wb") as fh:
        fh.write(r.read())
    return os.path.getsize(dest)


def main():
    os.makedirs(LIBS, exist_ok=True)
    sable_jar = None

    for slug, version in WANTED.items():
        versions = get_json(f"https://api.modrinth.com/v2/project/{slug}/version")
        match = next((v for v in versions
                      if v["version_number"] == version and "neoforge" in v["loaders"]), None)
        if match is None:
            print(f"!! {slug} {version} (neoforge) not found on Modrinth", file=sys.stderr)
            return 1
        f = match["files"][0]
        dest = os.path.join(LIBS, f["filename"])
        if os.path.exists(dest):
            print(f"have  {f['filename']}")
        else:
            size = download(f["url"], dest)
            print(f"got   {f['filename']}  ({size/1024/1024:.1f} MiB)")
        if slug == "sable":
            sable_jar = dest

    if sable_jar:
        with zipfile.ZipFile(sable_jar) as z:
            names = {n.split("/")[-1]: n for n in z.namelist() if n.startswith("META-INF/jarjar/")}
            for src, out in NESTED.items():
                if src not in names:
                    print(f"!! {src} not present inside the Sable jar", file=sys.stderr)
                    return 1
                dest = os.path.join(LIBS, out)
                with open(dest, "wb") as fh:
                    fh.write(z.read(names[src]))
                print(f"unpacked {out}")

    print("\nlibs/ ready — run ./gradlew build")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
