# EULER Backend — FastAPI

This backend exposes HTTP endpoints used by the EULER agent and mobile app.  
It connects to the Qdrant vector database for EPFL website Q&A, and later to Firebase for authentication.

---

## 🧩 Features

- `GET /health` — basic check, always public  
- `GET /me` — shows authenticated user (uses Firebase later)  
- `GET /rag/search?q=...` — searches Qdrant for related EPFL content  
- `POST /rag/index` — inserts or updates one document in Qdrant  

---

## ⚙️ Setup

### 1. Requirements
- Python 3.11+
- `pip install -r requirements.txt`




