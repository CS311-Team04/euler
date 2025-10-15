from __future__ import annotations

import asyncio
from collections import deque
from typing import Deque, List, Optional, Set

import logging
import signal
import time

from .config import ScraperConfig
from .fetch import PoliteHttpClient
from .extract import extract_text
from .filters import (
    extract_links,
    has_disallowed_extension,
    is_allowed_path,
    is_epfl_domain,
    is_html_like_content_type,
    normalize_url,
)
from .storage import Frontier, JsonlWriter, VisitedSet, iso_now, save_text_mirror, sha256_text


class Crawler:
    def __init__(self, cfg: ScraperConfig) -> None:
        self.cfg = cfg
        self.visited = VisitedSet(cfg.visited_file)
        self.frontier = Frontier(cfg.frontier_file)

    def _seed_frontier(self) -> Deque[str]:
        seed: List[str] = self.frontier.load() if self.cfg.save_frontier else []
        if not seed:
            seed = [normalize_url(u) for u in self.cfg.start_urls]
        return deque(u for u in seed if u not in self.visited)

    async def crawl(self) -> None:
        self.cfg.ensure_dirs()
        queue: Deque[str] = self._seed_frontier()
        client = PoliteHttpClient(self.cfg)
        writer = JsonlWriter(self.cfg.output_jsonl)
        seen: Set[str] = set(queue)
        logger = logging.getLogger("epfl_scraper.crawler")

        start_ts = time.monotonic()
        stop_requested = False

        def _on_sigint(signum, frame):  # type: ignore[override]
            nonlocal stop_requested
            stop_requested = True
            logger.info("SIGINT received; will checkpoint and stop soon…")

        try:
            signal.signal(signal.SIGINT, _on_sigint)
        except Exception:
            # Some platforms may not support signal in this context
            pass

        try:
            pages_processed = 0
            skipped_pages = 0
            while queue and pages_processed < self.cfg.max_pages:
                if stop_requested:
                    break
                url = queue.popleft()
                if url in self.visited:
                    continue
                if not is_epfl_domain(url) or has_disallowed_extension(url) or not is_allowed_path(url, self.cfg.allow_paths):
                    self.visited.add(url)
                    skipped_pages += 1
                    # Periodic metrics
                    if pages_processed and pages_processed % self.cfg.checkpoint_every == 0:
                        elapsed = max(1e-6, time.monotonic() - start_ts)
                        rate = pages_processed / elapsed
                        logger.info(
                            "processed=%d rate=%.2f/s skipped=%d frontier=%d",
                            pages_processed,
                            rate,
                            skipped_pages,
                            len(queue),
                        )
                    continue

                result = await client.fetch(url)
                if result is None:
                    self.visited.add(url)
                    skipped_pages += 1
                    if pages_processed and pages_processed % self.cfg.checkpoint_every == 0:
                        elapsed = max(1e-6, time.monotonic() - start_ts)
                        rate = pages_processed / elapsed
                        logger.info(
                            "processed=%d rate=%.2f/s skipped=%d frontier=%d",
                            pages_processed,
                            rate,
                            skipped_pages,
                            len(queue),
                        )
                    continue

                # Filter content type
                if not is_html_like_content_type(result.content_type):
                    self.visited.add(url)
                    skipped_pages += 1
                    if pages_processed and pages_processed % self.cfg.checkpoint_every == 0:
                        elapsed = max(1e-6, time.monotonic() - start_ts)
                        rate = pages_processed / elapsed
                        logger.info(
                            "processed=%d rate=%.2f/s skipped=%d frontier=%d",
                            pages_processed,
                            rate,
                            skipped_pages,
                            len(queue),
                        )
                    continue

                text, title, lang = (None, None, None)
                if result.text:
                    text, title, lang = extract_text(result.text, result.final_url)

                if text:
                    checksum = sha256_text(text)
                    save_text_mirror(self.cfg.mirror_dir, result.final_url, text)
                    writer.write({
                        "url": url,
                        "canonical_url": result.final_url if result.final_url != url else None,
                        "fetched_at": iso_now(),
                        "status_code": result.status_code,
                        "content_type": result.content_type,
                        "title": title,
                        "lang": lang,
                        "text": text,
                        "checksum": checksum,
                        "section": self.cfg.section,
                    })

                    # Discover links
                    for link in extract_links(result.text, result.final_url):
                        if not is_epfl_domain(link):
                            continue
                        if has_disallowed_extension(link):
                            continue
                        if not is_allowed_path(link, self.cfg.allow_paths):
                            continue
                        if link not in seen and link not in self.visited:
                            queue.append(link)
                            seen.add(link)

                self.visited.add(url)
                pages_processed += 1

                # Periodic checkpoint and metrics
                if self.cfg.save_frontier and (
                    pages_processed % max(1, self.cfg.checkpoint_every) == 0
                ):
                    self.frontier.save(queue)
                    elapsed = max(1e-6, time.monotonic() - start_ts)
                    rate = pages_processed / elapsed
                    logger.info(
                        "processed=%d rate=%.2f/s skipped=%d frontier=%d",
                        pages_processed,
                        rate,
                        skipped_pages,
                        len(queue),
                    )
        finally:
            await client.close()
            writer.close()
            # Final checkpoint on exit
            try:
                if self.cfg.save_frontier:
                    self.frontier.save(queue)
            except Exception:
                pass
