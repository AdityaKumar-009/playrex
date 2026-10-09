#!/usr/bin/env python3
"""Fail CI if known RzFlix ad SDK dependencies enter the Android sources."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
PATTERNS = [re.compile(x, re.I) for x in (
    r'\btradplus[_\-\.]?sdk\b',
    r'\bcom\.unity3d\.ads\b',
    r'\bcom\.vungle\.ads\b',
    r'\bcom\.mbridge\.msdk\b',
    r'\bcom\.google\.android\.gms\.ads\b',
)]
EXTENSIONS = {".gradle", ".kts", ".kt", ".java", ".xml", ".toml"}
issues = []
for dirname in ("androidApp", "composeApp"):
    for path in (ROOT / dirname).rglob("*"):
        if path.suffix not in EXTENSIONS or not path.is_file():
            continue
        try:
            content = path.read_text(encoding="utf-8")
        except UnicodeError:
            continue
        for pattern in PATTERNS:
            if pattern.search(content):
                issues.append(f"{path.relative_to(ROOT)}: {pattern.pattern}")
if issues:
    raise SystemExit("Ad SDK identifiers found:\n" + "\n".join(issues))
print("PASS: no known RzFlix ad SDK dependencies found in Android source files")
