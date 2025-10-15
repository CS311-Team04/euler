from __future__ import annotations

import asyncio
import time

import pytest

from epfl_scraper.config import ScraperConfig
from epfl_scraper.fetch import PoliteHttpClient


@pytest.mark.asyncio
async def test_respect_rate_limit_adds_jitter_after_min_interval(monkeypatch):
    cfg = ScraperConfig(
        start_urls=["https://www.epfl.ch/education/fr/"],
        allow_paths=["/education/fr"],
        output_jsonl=__import__("pathlib").Path("/tmp/out.jsonl"),
        rate_per_sec=2.0,  # min interval 0.5s
        jitter_s=0.2,
    )
    client = PoliteHttpClient(cfg)
    try:
        # Simulate last request just now to force sleeping
        client._last_request_ts = time.monotonic()

        start = time.monotonic()
        await client._respect_rate_limit()
        elapsed = time.monotonic() - start

        # Should be at least min_interval (0.5) and at most min_interval + jitter (0.7) + a small tolerance
        assert elapsed >= 0.49
        assert elapsed <= 0.75
    finally:
        await client.close()


