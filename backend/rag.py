# backend/rag.py
import os
from fastapi import APIRouter, HTTPException, Query, Body
import httpx
from qdrant_client import QdrantClient
from uuid import UUID, uuid4
from dotenv import load_dotenv

load_dotenv()

router = APIRouter(prefix="/rag", tags=["rag"])

QDRANT_URL = os.getenv("QDRANT_URL")
QDRANT_API_KEY = os.getenv("QDRANT_API_KEY")
QDRANT_COLLECTION = os.getenv("QDRANT_COLLECTION", "epfl")
EMBEDDINGS_URL = os.getenv("EMBEDDINGS_URL")
EMBED_MODEL_ID = os.getenv("EMBED_MODEL_ID", "jina-embeddings-v3")
JINA_API_KEY = os.getenv("JINA_API_KEY")

client = QdrantClient(url=QDRANT_URL, api_key=QDRANT_API_KEY)


def normalize_point_id(x):
    if isinstance(x, int) and x >= 0:
        return x
    s = str(x)
    try:
        UUID(s)
        return s
    except Exception:
        return str(uuid4())


async def embed_texts(texts: list[str]) -> list[list[float]]:
    """Send text list to Jina-compatible embedding API."""
    headers = {
        "Authorization": f"Bearer {JINA_API_KEY}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    }
    data = {"model": EMBED_MODEL_ID, "input": texts}
    async with httpx.AsyncClient(timeout=60.0) as http:
        r = await http.post(EMBEDDINGS_URL, headers=headers, json=data)
        if r.status_code != 200:
            raise HTTPException(500, f"Embedding API failed: {r.text}")
        j = r.json()
        return [d["embedding"] for d in j["data"]]


@router.get("/search")
async def rag_search(q: str = Query(..., description="Query text"), k: int = 5):
    """Search similar chunks in Qdrant."""
    vectors = await embed_texts([q])
    vector = vectors[0]
    try:
        res = client.search(collection_name=QDRANT_COLLECTION, query_vector=vector, limit=k)
    except Exception as e:
        raise HTTPException(500, f"Qdrant search failed: {e}")

    return [{"id": str(p.id), "score": p.score, "payload": p.payload} for p in res]


@router.post("/index")
async def rag_index(body: dict = Body(...)):
    """Index a single text chunk."""
    text = body.get("text")
    doc_id = body.get("id")
    metadata = body.get("metadata", {})

    if not text:
        raise HTTPException(400, "Missing text")

    vectors = await embed_texts([text])
    vector = vectors[0]
    pid = normalize_point_id(doc_id)
    try:
        client.upsert(
            collection_name=QDRANT_COLLECTION,
            points=[{"id": pid, "vector": vector, "payload": metadata | {"text": text}}],
        )
    except Exception as e:
        raise HTTPException(500, f"Qdrant upsert failed: {e}")

    return {"ok": True, "id": pid}


@router.post("/index-batch")
async def rag_index_batch(body: dict = Body(...)):
    """Index multiple chunks."""
    items = body.get("chunks", [])
    if not items:
        raise HTTPException(400, "Missing 'chunks' array")
    texts = [it["text"] for it in items]
    vecs = await embed_texts(texts)
    points = []
    for it, v in zip(items, vecs):
        pid = normalize_point_id(it.get("id"))
        meta = it.get("metadata", {})
        points.append({"id": pid, "vector": v, "payload": meta | {"text": it["text"]}})
    try:
        client.upsert(collection_name=QDRANT_COLLECTION, points=points)
    except Exception as e:
        raise HTTPException(500, f"Qdrant upsert failed: {e}")
    return {"ok": True, "count": len(points)}
