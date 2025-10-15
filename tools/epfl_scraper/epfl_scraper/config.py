from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional


@dataclass
class ScraperConfig:
    start_urls: List[str]
    allow_paths: List[str]
    output_jsonl: Path
    mirror_dir: Optional[Path] = None
    state_dir: Path = Path(".crawler_state")
    section: str = "education"

    # Networking / politeness
    user_agent: str = "EPFL-RAG-Crawler/0.1 (+contact@example.com)"
    rate_per_sec: float = 1.0  # requests per second
    max_pages: int = 5000
    request_timeout_s: float = 20.0
    max_retries: int = 3
    backoff_base_s: float = 1.0
    jitter_s: float = 0.2
    obey_robots: bool = True

    # Content limits
    max_content_bytes: int = 5_000_000  # 5 MB safety cap

    # Advanced
    save_frontier: bool = True
    checkpoint_every: int = 100  # pages

    def ensure_dirs(self) -> None:
        if self.mirror_dir:
            self.mirror_dir.mkdir(parents=True, exist_ok=True)
        self.output_jsonl.parent.mkdir(parents=True, exist_ok=True)
        self.state_dir.mkdir(parents=True, exist_ok=True)

    @property
    def visited_file(self) -> Path:
        return self.state_dir / "visited_urls.txt"

    @property
    def frontier_file(self) -> Path:
        return self.state_dir / "frontier.jsonl"
