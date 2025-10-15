# EPFL /education Scraper (JSONL + .txt)

Polite crawler for `https://www.epfl.ch/education/**` producing JSONL records suitable for RAG, with an optional plain-text mirror per page.

## Setup

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r tools/epfl_scraper/requirements.txt
```

## Usage (French section by default)

```bash
# From repo root
PYTHONPATH=tools/epfl_scraper python -m epfl_scraper \
  --output data/epfl_education.jsonl \
  --mirror-dir data/text \
  --max-pages 500 \
  --rate 1.0
```

- Defaults seed to `https://www.epfl.ch/education/fr/` and allows only `/education/fr` paths, i.e., French pages.
- Respects `robots.txt` and applies rate limiting with jitter.
- Deduplicates by normalized URL and text checksum.
- Saves progress under `--state-dir` (visited URLs, frontier).

To override defaults explicitly:

```bash
python -m epfl_scraper \
  --start https://www.epfl.ch/education/fr/ \
  --allow-path /education/fr \
  --output data/epfl_education.jsonl \
  --mirror-dir data/text \
  --max-pages 500 \
  --rate 1.0
```

### Language shortcut

Switch between FR/EN presets without typing full URLs:

```bash
PYTHONPATH=tools/epfl_scraper python -m epfl_scraper --lang en --rate 1.5
```

This sets `--start` to `https://www.epfl.ch/education/` and `--allow-path` to `/education/`.

### Checkpointing and restart

The crawler periodically checkpoints the frontier every `--checkpoint-every` pages (default 100) and on Ctrl+C. State is kept in `--state-dir`.

To restart a crawl, simply run the same command again; the frontier will be reloaded automatically.

```bash
PYTHONPATH=tools/epfl_scraper python -m epfl_scraper --lang fr --checkpoint-every 100
```

## Output JSONL schema

Each line is a JSON object with fields:

- `url`, `canonical_url`, `fetched_at`, `status_code`, `content_type`, `title`, `lang`, `text`, `checksum`, `section`

Example output record:

```json
{"url":"https://www.epfl.ch/education/admission/admission-2/bachelor-admission-criteria-and-application/","title":"Bachelor/CMS admission criteria & application","text":"In addition to this page, please consult...","fetched_at":"2025-10-09T12:00:00Z","status_code":200,"content_type":"text/html; charset=utf-8","lang":"en","canonical_url":null,"checksum":"<sha256>","section":"education"}
```

## Notes

- Only HTML pages within the allowed path prefixes are crawled. PDFs and binaries are skipped.
- The default `User-Agent` is `EPFL-RAG-Crawler/0.1 (+contact@example.com)`; customize via `--user-agent`.
- You can resume interrupted runs thanks to the state dir. Delete `.crawler_state/` to start fresh.
