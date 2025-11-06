# tools/epfl_scraper/index_texts.py
import os, re, json, time, hashlib, requests, nltk
from pathlib import Path
from typing import Dict, Any, Iterable, List
from nltk.tokenize import sent_tokenize, word_tokenize
from dotenv import load_dotenv

# ---------- env ----------
load_dotenv(dotenv_path=Path(__file__).resolve().parent / ".env")
INDEX_URL = os.getenv("INDEX_URL")
INDEX_KEY = os.getenv("INDEX_KEY")
if not INDEX_URL or not INDEX_KEY:
    raise SystemExit("[error] Missing INDEX_URL or INDEX_KEY in .env")
ensure_nltk()

# ---------- NLTK ----------
def ensure_nltk():
    try:
        nltk.data.find("tokenizers/punkt")
    except LookupError:
        nltk.download("punkt", quiet=True)

def sent_tokenize_lang(text: str, lang: str | None) -> List[str]:
    # basic language switch; extend if you need more languages
    l = (lang or "en").lower()
    if l.startswith("fr"):
        return sent_tokenize(text, language="french")
    return sent_tokenize(text, language="english")

# ---------- ids ----------
def stable_id(s: str) -> str:
    return hashlib.sha1(s.encode("utf-8")).hexdigest()

# ---------- normalization + chunking ----------
def normalize(text: str) -> str:
    text = text.replace("-\n", "")
    text = re.sub(r"[ \t]+\n", "\n", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text

def _split_long_sentence(s: str, max_chars: int) -> List[str]:
    out, buf, cur_len = [], [], 0
    for w in word_tokenize(s):
        add = (1 if buf else 0) + len(w)
        if cur_len + add <= max_chars:
            buf.append(w); cur_len += add
        else:
            out.append(" ".join(buf)); buf = [w]; cur_len = len(w)
    if buf: out.append(" ".join(buf))
    return out

def chunk_text(text: str, lang: str | None, max_chars=1500, overlap_sents=2) -> List[str]:
    text = normalize(text)
    sents = [s.strip() for s in sent_tokenize_lang(text, lang) if s.strip()]
    chunks, cur, cur_len = [], [], 0

    def push():
        if cur: chunks.append(" ".join(cur).strip())

    for s in sents:
        if len(s) > max_chars:
            push(); chunks.extend(_split_long_sentence(s, max_chars)); cur, cur_len = [], 0; continue
        add_len = (1 if cur else 0) + len(s)
        if cur_len + add_len <= max_chars:
            cur.append(s); cur_len += add_len
        else:
            push()
            overlap = cur[-overlap_sents:] if overlap_sents > 0 else []
            cur = overlap[:] + [s]
            cur_len = sum(len(x) for x in cur) + max(0, len(cur) - 1)
    push()
    return chunks

# ---------- HTTP ----------
def index_batches(chunks: List[Dict[str, Any]], batch=64, pause=0.05):
    for i in range(0, len(chunks), batch):
        payload = {"chunks": chunks[i:i+batch]}
        r = requests.post(
            INDEX_URL,
            headers={"x-api-key": INDEX_KEY, "Content-Type": "application/json"},
            data=json.dumps(payload).encode("utf-8"),
            timeout=200,
        )
        if not r.ok:
            raise RuntimeError(f"index batch {i//batch}: HTTP {r.status_code} {r.text[:1000]}")
        time.sleep(pause)

# ---------- record loaders (JSON + JSONL) ----------
def load_json_records(file: Path) -> Iterable[Dict[str, Any]]:
    """
    Supports:
      - JSONL/NDJSON: one JSON object per line
      - JSON array
      - single JSON object
    """
    txt = file.read_text(encoding="utf-8").strip()
    if not txt:
        return []
    # JSONL if multiple lines that each look like an object
    if file.suffix.lower() in (".jsonl", ".ndjson"):
        for line in txt.splitlines():
            line = line.strip()
            if line:
                yield json.loads(line)
        return
    # Try JSON array/object
    obj = json.loads(txt)
    if isinstance(obj, list):
        for rec in obj:
            yield rec
    else:
        yield obj

# ---------- per-record indexing ----------
def build_chunks_from_record(rec: Dict[str, Any], max_chars=1500, overlap_sents=2) -> List[Dict[str, Any]]:
    text = (rec.get("text") or "").strip()
    if not text:
        return []
    url = rec.get("canonical_url") or rec.get("url") or ""
    title = rec.get("title") or ""
    lang = rec.get("lang")

    base_key = rec.get("checksum") or url or (title + text[:80])
    base = stable_id(base_key)

    out: List[Dict[str, Any]] = []
    for i, c in enumerate(chunk_text(text, lang=lang, max_chars=max_chars, overlap_sents=overlap_sents)):
        out.append({
            "id": f"{base}_{i}",
            "text": c,
            "title": title or None,
            "url": url or None,
            "payload": {
                "lang": lang,
                "section": rec.get("section"),
                "fetched_at": rec.get("fetched_at"),
                "source": "epfl_scraper",
            },
        })
    return out

# ---------- file indexing ----------
def index_json_file(path: Path, max_chars=1500, overlap_sents=2):
    records = list(load_json_records(path))
    if not records:
        print(f"{path} -> 0 records (skipped)")
        return
    all_chunks: List[Dict[str, Any]] = []
    for rec in records:
        all_chunks.extend(build_chunks_from_record(rec, max_chars, overlap_sents))
    if not all_chunks:
        print(f"{path} -> 0 chunks (skipped)")
        return
    index_batches(all_chunks)
    print(f"{path} -> {len(all_chunks)} chunks")

# ---------- main ----------
def main():
    # Default to tools/epfl_scraper/data next to this script
    base = Path(__file__).resolve().parent
    data_dir = base / "data"

    # Scan both .jsonl and .json
    files = list(data_dir.rglob("*.jsonl")) + list(data_dir.rglob("*.json"))
    print(f"[info] data_dir={data_dir}  files={len(files)}")
    if not files:
        raise SystemExit("[error] no JSON/JSONL files found")

    for f in files:
        print(f"[index] {f}")
        index_json_file(f, max_chars=1500, overlap_sents=2)

if __name__ == "__main__":
    main()
