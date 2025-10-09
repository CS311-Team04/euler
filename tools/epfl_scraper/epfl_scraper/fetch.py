from __future__ import annotations

import asyncio
import random
import time
from dataclasses import dataclass
from typing import Optional
from urllib.parse import urlparse

import httpx
from httpx import Response
from urllib import robotparser

from .config import ScraperConfig


@dataclass
class FetchResult:
    url: str
    status_code: int
    content_type: Optional[str]
    text: Optional[str]
    final_url: str


class RobotsCache:
    def __init__(self) -> None:
        self._cache: dict[str, robotparser.RobotFileParser] = {}

    def get(self, root: str) -> robotparser.RobotFileParser:
        if root in self._cache:
            return self._cache[root]
        rp = robotparser.RobotFileParser()
        rp.set_url(f"{root}/robots.txt")
        try:
            rp.read()
        except Exception:
            # If robots cannot be fetched, be conservative: disallow nothing (will rely on rate limiting)
            pass
        self._cache[root] = rp
        return rp

    def allowed(self, user_agent: str, url: str) -> bool:
        parsed = urlparse(url)
        root = f"{parsed.scheme}://{parsed.netloc}"
        rp = self.get(root)
        try:
            return rp.can_fetch(user_agent, url)
        except Exception:
            return True


class PoliteHttpClient:
    def __init__(self, cfg: ScraperConfig) -> None:
        self.cfg = cfg
        self._client = httpx.AsyncClient(timeout=cfg.request_timeout_s, headers={"User-Agent": cfg.user_agent})
        self._robots = RobotsCache()
        self._last_request_ts: float = 0.0

    async def close(self) -> None:
        await self._client.aclose()

    async def _respect_rate_limit(self) -> None:
        min_interval = 1.0 / max(self.cfg.rate_per_sec, 0.001)
        now = time.monotonic()
        elapsed = now - self._last_request_ts
        if elapsed < min_interval:
            await asyncio.sleep(min_interval - elapsed + random.uniform(0, self.cfg.jitter_s))

    async def fetch(self, url: str) -> Optional[FetchResult]:
        if self.cfg.obey_robots and not self._robots.allowed(self.cfg.user_agent, url):
            return None

        attempts = 0
        backoff = self.cfg.backoff_base_s

        while attempts < self.cfg.max_retries:
            await self._respect_rate_limit()
            try:
                resp: Response = await self._client.get(url, follow_redirects=True)
                self._last_request_ts = time.monotonic()
                status = resp.status_code
                ctype = resp.headers.get("content-type")

                text: Optional[str] = None
                if resp.content and len(resp.content) <= self.cfg.max_content_bytes:
                    # Prefer response.text to decode using charset
                    text = resp.text

                return FetchResult(
                    url=url,
                    status_code=status,
                    content_type=ctype,
                    text=text,
                    final_url=str(resp.url),
                )
            except httpx.HTTPStatusError as e:
                status = e.response.status_code
                if status in (429, 500, 502, 503, 504):
                    await asyncio.sleep(backoff + random.uniform(0, self.cfg.jitter_s))
                    backoff *= 2
                    attempts += 1
                    continue
                raise
            except (httpx.ConnectError, httpx.ReadTimeout, httpx.RemoteProtocolError):
                await asyncio.sleep(backoff + random.uniform(0, self.cfg.jitter_s))
                backoff *= 2
                attempts += 1
            except Exception:
                # Non-retryable
                return None
        return None
