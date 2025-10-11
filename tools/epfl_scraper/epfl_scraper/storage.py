from __future__ import annotations

import json
import hashlib
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable, Optional


def sha256_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def iso_now() -> str:
    return datetime.now(timezone.utc).isoformat()


class JsonlWriter:
    def __init__(self, output_path: Path) -> None:
        self.output_path = output_path
        self._fh = output_path.open("a", encoding="utf-8")

    def write(self, obj: dict) -> None:
        self._fh.write(json.dumps(obj, ensure_ascii=False) + "\n")
        self._fh.flush()

    def close(self) -> None:
        self._fh.close()


def save_text_mirror(mirror_dir: Optional[Path], url: str, text: str) -> Optional[Path]:
    if not mirror_dir:
        return None
    # Use hash-based filename to avoid filesystem issues
    fname = sha256_text(url) + ".txt"
    path = mirror_dir / fname
    path.write_text(text, encoding="utf-8")
    return path


class VisitedSet:
    def __init__(self, path: Path) -> None:
        self.path = path
        self._set = set()
        if path.exists():
            for line in path.read_text(encoding="utf-8").splitlines():
                s = line.strip()
                if s:
                    self._set.add(s)

    def add(self, url: str) -> None:
        if url in self._set:
            return
        self._set.add(url)
        with self.path.open("a", encoding="utf-8") as fh:
            fh.write(url + "\n")

    def __contains__(self, url: str) -> bool:
        return url in self._set


class Frontier:
    def __init__(self, path: Path) -> None:
        self.path = path

    def save(self, urls: Iterable[str]) -> None:
        with self.path.open("w", encoding="utf-8") as fh:
            for u in urls:
                fh.write(u + "\n")

    def load(self) -> list[str]:
        if not self.path.exists():
            return []
        return [l.strip() for l in self.path.read_text(encoding="utf-8").splitlines() if l.strip()]
