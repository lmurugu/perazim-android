#!/usr/bin/env python3
"""Download the raw files named in scripts/reference/sources.json into a raw directory.

Each source's sourceUrl is fetched to <raw>/<file>. When a sha256 is recorded in sources.json, the download is
checked against it and a mismatch is an error. Existing files with a matching digest are kept.

Usage:
    python3 scripts/reference/fetch_reference_sources.py --raw <raw dir> [--only id ...]
"""

import argparse
import hashlib
import json
import os
import sys
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
USER_AGENT = "perazim-reference-build/1.0"


def sha256_bytes(data):
    return hashlib.sha256(data).hexdigest()


def fetch(url, timeout=120):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return response.read()


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--raw", required=True)
    parser.add_argument("--only", nargs="*")
    parser.add_argument("--record", action="store_true",
                        help="write the sha256 and byte size of each fetched file back into sources.json")
    args = parser.parse_args(argv)
    raw = Path(args.raw)
    sources_path = HERE / "sources.json"
    data = json.loads(sources_path.read_text(encoding="utf-8"))
    changed = False
    for source in data["sources"]:
        if args.only and source["id"] not in args.only:
            continue
        target = raw / source["file"]
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists() and source.get("sha256") and sha256_bytes(target.read_bytes()) == source["sha256"]:
            print("kept   %s" % source["id"])
            continue
        body = fetch(source["sourceUrl"])
        digest = sha256_bytes(body)
        if source.get("sha256") and digest != source["sha256"]:
            sys.exit("sha256 mismatch for %s: expected %s, got %s" % (source["id"], source["sha256"], digest))
        target.write_bytes(body)
        print("fetched %-26s %9d bytes  sha256 %s" % (source["id"], len(body), digest[:16]))
        if args.record and source.get("sha256") != digest:
            source["sha256"] = digest
            source["sizeBytes"] = len(body)
            changed = True
    if changed:
        sources_path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
