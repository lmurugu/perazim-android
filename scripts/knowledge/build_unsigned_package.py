#!/usr/bin/env python3
"""Builds the UNSIGNED knowledge package (pre-signing estimate) from the approved library files.

Format: perazim-knowledge-v1 (OfflineKnowledgeStore.verify). The envelope and payload match the product's shape. Only the
signature is missing: signingKeyId is PENDING and signature is empty, so the product will refuse this file until Levi signs
it on his laptop (see ai-eval/knowledge-package-unsigned/SIGNING.md).

Inputs (approved material only):
  - src/main/assets/ai/library/doctrine/entries.json      APPROVED_LEVI entries (Perazim view and other views, verses)
  - src/main/assets/ai/library/doctrine/positions.json    UNDER_REVIEW developing views (labelled) and UNDECIDED topics
                                                          (explained without a position). DRAFT_PENDING_LEVI is skipped.
  - ai-eval/knowledge-package-unsigned/source/comparative-kb-1.5.22.json   APPROVED_LEVI entries only (PR 76, d052d7c)
  - scripts/reference/creeds/nicene-constantinopolitan-381.txt           approved by Levi 2026-11-23
  - scripts/reference/sources.json + src/main/assets/reference/reference.db.gz, entries of Hitchcock's Bible Names
    (approved factual source). Smith, Easton and Nave's are excluded: the 2 MB package limit cannot hold them.
Excluded on purpose: Henry and Wesley commentaries (optional download, not in the default package), cross-references and
places (not model-facing text), the apostles' and Westminster creeds (not approved).

Output: ai-eval/knowledge-package-unsigned/package.unsigned.json and a summary to stdout.
"""
import gzip
import hashlib
import json
import sqlite3
import tempfile
from datetime import datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT_DIR = ROOT / "ai-eval/knowledge-package-unsigned"
FORMAT = "perazim-knowledge-v1"
PUBLISHED = datetime(2026, 11, 25, tzinfo=timezone.utc)
MAX_BYTES = 2 * 1024 * 1024  # OfflineKnowledgeStore.MAX_BYTES


def sha256_hex(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def canonical(value):
    # Matches OfflineKnowledgeStore.canonical: sorted keys, no whitespace, JSON.stringify-style strings.
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def doc(source_id, title, version, location, text, approval, authority="APPROVED"):
    return {
        "sourceId": source_id,
        "title": title,
        "authorityClass": authority,
        "version": version,
        "location": location,
        "text": text,
        "contentHash": sha256_hex(text),
        "approvalReference": approval,
    }


def doctrine_docs():
    docs = []
    lib = ROOT / "src/main/assets/ai/library/doctrine"
    entries = json.loads((lib / "entries.json").read_text(encoding="utf-8"))["entries"]
    for e in entries:
        if e.get("status") != "APPROVED_LEVI":
            continue
        parts = [e["perazimView"]]
        if e.get("otherViews"):
            parts.append("Other Christian views: " + " ".join(e["otherViews"]))
        for v in (e.get("verses") or [])[:3]:
            parts.append(f'{v["ref"]} (King James Version): "{v["text"]}"')
        if e.get("noVerdict"):
            parts.append(e["noVerdict"])
        approval = e.get("approval") or {}
        ref = "doctrine " + e["id"] + " APPROVED_LEVI " + str(approval.get("messageId", "Levi"))
        docs.append(doc("doctrine-" + e["id"], e["topic"], "entries-" + str(approval.get("date", "2026-11-24")),
                        e["id"], " ".join(parts), ref))
    positions = json.loads((lib / "positions.json").read_text(encoding="utf-8"))["topics"]
    for p in positions:
        status = p.get("status")
        if status == "UNDER_REVIEW":
            wording = (p.get("draft") or {}).get("wording", "")
            text = ("This is a developing view, under review, and not yet a settled church position. " + wording)
            docs.append(doc("doctrine-" + p["id"], p["topic"], "positions", p["id"], text,
                            "doctrine " + p["id"] + " UNDER_REVIEW", authority="UNDER_REVIEW"))
        elif status == "UNDECIDED":
            text = ("The guide has not settled a position on " + p["topic"].lower()
                    + ", so it will not state one. Please speak with your pastor or a church leader about it.")
            docs.append(doc("doctrine-" + p["id"], p["topic"], "positions", p["id"], text,
                            "doctrine " + p["id"] + " UNDECIDED", authority="UNDECIDED"))
    return docs


def comparative_docs():
    kb = json.loads((OUT_DIR / "source/comparative-kb-1.5.22.json").read_text(encoding="utf-8"))
    docs = []
    for e in kb["entries"]:
        if e.get("status") != "APPROVED_LEVI":
            continue
        # Only model-facing fields: summary, origin, scripture (the KB's own rule).
        text = " ".join(x for x in (e.get("summary"), e.get("origin"), e.get("scripture")) if x)
        docs.append(doc("comparative-" + e["id"], e["id"].replace("-", " ").title(), kb["version"],
                        e.get("group", "comparative"), text,
                        "comparative-kb " + kb["version"] + " APPROVED_LEVI (PR 76, d052d7c)"))
    return docs


def creed_docs():
    text = (ROOT / "scripts/reference/creeds/nicene-constantinopolitan-381.txt").read_text(encoding="utf-8").strip()
    return [doc("creed-nicene-constantinopolitan-381", "Nicene-Constantinopolitan Creed of 381", "1",
                "Nicene-Constantinopolitan Creed of 381", text,
                "approvals.json creed-nicene-constantinopolitan-381 (Levi 2026-11-23)")]


def hitchcock_docs(limit_bytes=400 * 1024):
    docs = []
    used = 0
    with gzip.open(ROOT / "src/main/assets/reference/reference.db.gz", "rb") as fh:
        data = fh.read()
    with tempfile.NamedTemporaryFile(suffix=".db", delete=False) as tmp:
        tmp.write(data)
        path = tmp.name
    con = sqlite3.connect(path)
    rows = con.execute("select headword, location, body from entries where source_id = 'hitchcock-bible-names' "
                       "order by id").fetchall()
    con.close()
    for headword, location, body in rows:
        text = body.strip()
        size = len(text.encode("utf-8"))
        if used + size > limit_bytes:
            break
        used += size
        docs.append(doc("hitchcock-bible-names", "Hitchcock's Bible Names Dictionary", "2003",
                        location or headword, headword + ": " + text,
                        "approvals.json hitchcock-bible-names (coordinator 2026-11-23, factual)"))
    return docs


def build():
    documents = doctrine_docs() + comparative_docs() + creed_docs() + hitchcock_docs()
    published = int(PUBLISHED.timestamp() * 1000)
    payload = {
        "format": FORMAT,
        "audience": "PUBLIC",
        "version": "pre-signing-2026-11-25",
        "snapshotKind": "SNAPSHOT",
        "publishedAt": published,
        "expiresAt": int((PUBLISHED + timedelta(days=90)).timestamp() * 1000),
        "minimumAppVersion": "1.0.0",
        "documents": documents,
    }
    payload["contentDigest"] = sha256_hex(canonical({k: v for k, v in payload.items() if k != "contentDigest"}))
    envelope = {"payload": payload, "signingKeyId": "PENDING_LEVI_PACKAGE_KEY", "signature": ""}
    text = json.dumps(envelope, ensure_ascii=False, indent=1)
    size = len(text.encode("utf-8"))
    if size > MAX_BYTES:
        raise SystemExit(f"package is {size} bytes, over the {MAX_BYTES} limit")
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    (OUT_DIR / "package.unsigned.json").write_text(text, encoding="utf-8")
    by_source = {}
    for d in documents:
        by_source[d["sourceId"].split("-")[0]] = by_source.get(d["sourceId"].split("-")[0], 0) + 1
    print(f"documents: {len(documents)}  bytes: {size}  contentDigest: {payload['contentDigest']}")
    print("by source prefix:", by_source)


if __name__ == "__main__":
    build()
