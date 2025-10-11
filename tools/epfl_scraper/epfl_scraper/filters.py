from __future__ import annotations

from typing import Iterable, List, Optional, Set
from urllib.parse import urljoin, urlparse, urlunparse, parse_qsl, urlencode
from bs4 import BeautifulSoup

# Extensions to block (non-HTML). Keep PDFs excluded per scope.
DISALLOWED_EXTENSIONS: Set[str] = {
    ".pdf", ".zip", ".tar", ".gz", ".bz2", ".xz",
    ".7z", ".rar", ".png", ".jpg", ".jpeg", ".gif", ".webp",
    ".svg", ".mp4", ".mp3", ".wav", ".avi", ".mov", ".mkv",
    ".woff", ".woff2", ".ttf", ".otf",
}


def normalize_url(url: str) -> str:
    parsed = urlparse(url)
    # Lowercase scheme and host
    scheme = (parsed.scheme or "http").lower()
    netloc = parsed.netloc.lower()

    # Sort query params, drop fragment
    query = urlencode(sorted(parse_qsl(parsed.query, keep_blank_values=True)), doseq=True)

    # Collapse multiple slashes, remove trailing slash except for root
    parts = [p for p in parsed.path.split("/") if p]
    path = "/" + "/".join(parts)
    if path != "/" and path.endswith("/"):
        path = path[:-1]

    normalized = urlunparse((scheme, netloc, path, "", query, ""))
    return normalized


def is_epfl_domain(url: str) -> bool:
    host = urlparse(url).netloc.lower()
    return host.endswith(".epfl.ch") or host == "epfl.ch"


def has_disallowed_extension(url: str) -> bool:
    path = urlparse(url).path
    dot = path.rfind(".")
    if dot == -1:
        return False
    ext = path[dot:].lower()
    return ext in DISALLOWED_EXTENSIONS


def is_allowed_path(url: str, allow_paths: Iterable[str]) -> bool:
    path = urlparse(url).path or "/"
    for prefix in allow_paths:
        if not prefix:
            continue
        if not prefix.startswith("/"):
            prefix = "/" + prefix
        if path.startswith(prefix):
            return True
    return False


def is_html_like_content_type(content_type: Optional[str]) -> bool:
    if not content_type:
        return False
    return content_type.startswith("text/html") or "html" in content_type


def resolve_link(base_url: str, href: Optional[str]) -> Optional[str]:
    if not href:
        return None
    href = href.strip()
    if href.startswith("javascript:") or href.startswith("mailto:") or href.startswith("tel:"):
        return None
    # Resolve relative against base
    absolute = urljoin(base_url, href)
    return normalize_url(absolute)


def extract_links(html: str, base_url: str) -> List[str]:
    soup = BeautifulSoup(html, "lxml")
    links: List[str] = []
    for a in soup.find_all("a"):
        candidate = resolve_link(base_url, a.get("href"))
        if candidate:
            links.append(candidate)
    return links
