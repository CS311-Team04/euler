from __future__ import annotations

from typing import Optional, Tuple

import trafilatura
from bs4 import BeautifulSoup


def extract_with_trafilatura(html: str, url: str) -> Tuple[Optional[str], Optional[str], Optional[str]]:
    text = trafilatura.extract(
        html,
        url=url,
        include_comments=False,
        include_links=False,
        include_tables=False,
        no_fallback=False,
        output="txt",
    )
    meta = trafilatura.extract_metadata(html)
    title = meta.title if meta else None
    lang = meta.language if meta else None
    return text, title, lang


def fallback_extract(html: str) -> Tuple[Optional[str], Optional[str]]:
    soup = BeautifulSoup(html, "lxml")
    title_tag = soup.find("title")
    title = title_tag.get_text(strip=True) if title_tag else None

    # Remove script/style/nav/footer headers
    for tag in soup(["script", "style", "noscript", "header", "footer", "nav", "form"]):
        tag.decompose()

    # Prefer main/article if present
    main = soup.find(["main", "article"]) or soup
    # Join paragraphs and headings
    chunks = []
    for el in main.find_all(["h1", "h2", "h3", "h4", "h5", "h6", "p", "li"]):
        text = el.get_text(" ", strip=True)
        if text:
            chunks.append(text)
    text = "\n\n".join(chunks) if chunks else None
    return text, title


def extract_text(html: str, url: str) -> tuple[Optional[str], Optional[str], Optional[str]]:
    text, title, lang = extract_with_trafilatura(html, url)
    if text:
        return text, title, lang
    # Fallback
    fb_text, fb_title = fallback_extract(html)
    return fb_text, fb_title, lang
