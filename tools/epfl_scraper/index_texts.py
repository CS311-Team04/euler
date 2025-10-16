# tools/index_texts.py
import os, requests, hashlib, nltk
from pathlib import Path
from nltk.tokenize import sent_tokenize #library used to break document into linguistic sentences, not arbitrary character spans.
from dotenv import load_dotenv
import os

load_dotenv()

INDEX_URL = os.environ["INDEX_URL"]
INDEX_KEY = os.environ["INDEX_KEY"]

def chunk_text(text, max_chars=1500, overlap=200):
    sentences = sent_tokenize(text)
    
    chunks, buf = [], ""
    for s in sentences:
        if len(buf) + len(s) + 1 < max_chars:
            buf += " " + s
        else:
            chunks.append(buf.strip())
            buf = (buf[-overlap:].strip() + " " + s).strip() 
    if buf.strip():
        chunks.append(buf.strip())
    return chunks

def file_id(path):
    return hashlib.sha1(path.encode()).hexdigest()

def index_file(path):
    with open(path, encoding="utf-8") as f:
        text = f.read()
    chunks = [
        {"id": f"{file_id(path)}_{i}", "text": chunk, "url": path}
        for i, chunk in enumerate(chunk_text(text))
    ]
    r = requests.post(
        INDEX_URL,
        headers={"X-API-Key": INDEX_KEY, "Content-Type": "application/json"},
        json={"chunks": chunks},
        timeout=60,
    )
    print(path, r.status_code, r.text)

def main():
    for txt in Path("data/text").glob("*.txt"):
        index_file(str(txt))

if __name__ == "__main__":
    main()
