from __future__ import annotations

import argparse
import asyncio
import logging
from pathlib import Path
from typing import List, Optional

from .config import ScraperConfig
from .crawler import Crawler
from .logging_setup import configure_logging


def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="EPFL /education scraper")
    parser.add_argument("--lang", dest="lang", choices=["fr", "en"], help="Language preset (sets --start and --allow-path)")
    parser.add_argument(
        "--start",
        dest="start",
        nargs="+",
        default=None,
        help="Seed URLs to start crawling (space-separated)",
    )
    parser.add_argument(
        "--allow-path",
        dest="allow_path",
        nargs="+",
        default=None,
        help="Allowed path prefixes within epfl.ch (space-separated)",
    )
    parser.add_argument(
        "--output",
        dest="output",
        type=Path,
        default=Path("data/epfl_education.jsonl"),
        help="JSONL output file",
    )
    parser.add_argument(
        "--mirror-dir",
        dest="mirror_dir",
        type=Path,
        default=Path("data/text"),
        help="Directory to save plain-text mirrors",
    )
    parser.add_argument(
        "--state-dir",
        dest="state_dir",
        type=Path,
        default=Path(".crawler_state"),
        help="Directory to store visited/frontier state",
    )
    parser.add_argument(
        "--user-agent",
        dest="user_agent",
        default="EPFL-RAG-Crawler/0.1 (+contact@example.com)",
        help="User-Agent header",
    )
    parser.add_argument("--rate", dest="rate", type=float, default=1.0, help="Requests per second")
    parser.add_argument("--max-pages", dest="max_pages", type=int, default=5000, help="Maximum pages to crawl")
    parser.add_argument("--timeout", dest="timeout", type=float, default=20.0, help="Request timeout (seconds)")
    parser.add_argument("--retries", dest="retries", type=int, default=3, help="Max retries for 429/5xx")
    parser.add_argument("--backoff", dest="backoff", type=float, default=1.0, help="Base backoff (seconds)")
    parser.add_argument("--jitter", dest="jitter", type=float, default=0.2, help="Jitter added to sleeps (seconds)")
    parser.add_argument("--checkpoint-every", dest="checkpoint_every", type=int, default=100, help="Persist frontier every N processed pages")
    parser.add_argument("--log-level", dest="log_level", default="INFO", help="Logging level (e.g., INFO, DEBUG)")
    parser.add_argument("--section", dest="section", default="education", help="Section label for output records")
    return parser.parse_args(argv)


def apply_lang_presets(args: argparse.Namespace) -> argparse.Namespace:
    """Mutate args to apply --lang presets unless explicit flags were provided."""
    if getattr(args, "lang", None):
        if args.lang == "fr":
            preset_start = ["https://www.epfl.ch/education/fr/"]
            preset_allow = ["/education/fr"]
        else:
            preset_start = ["https://www.epfl.ch/education/"]
            preset_allow = ["/education/"]
        if getattr(args, "start", None) is None:
            args.start = preset_start
        if getattr(args, "allow_path", None) is None:
            args.allow_path = preset_allow
    # Fallback defaults to French if still unset
    if getattr(args, "start", None) is None:
        args.start = ["https://www.epfl.ch/education/fr/"]
    if getattr(args, "allow_path", None) is None:
        args.allow_path = ["/education/fr"]
    return args


def main(argv: Optional[List[str]] = None) -> None:
    args = parse_args(argv)

    # Configure logging
    level = getattr(logging, str(args.log_level).upper(), logging.INFO)
    listener = configure_logging(level=level)

    # Apply language presets
    args = apply_lang_presets(args)

    cfg = ScraperConfig(
        start_urls=list(args.start),
        allow_paths=list(args.allow_path),
        output_jsonl=args.output,
        mirror_dir=args.mirror_dir,
        state_dir=args.state_dir,
        section=args.section,
        user_agent=args.user_agent,
        rate_per_sec=args.rate,
        max_pages=args.max_pages,
        request_timeout_s=args.timeout,
        max_retries=args.retries,
        backoff_base_s=args.backoff,
        jitter_s=args.jitter,
        obey_robots=True,
        checkpoint_every=args.checkpoint_every,
    )

    crawler = Crawler(cfg)
    try:
        asyncio.run(crawler.crawl())
    finally:
        try:
            listener.stop()
        except Exception:
            pass
