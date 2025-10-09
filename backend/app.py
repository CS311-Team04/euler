from fastapi import FastAPI, Depends
from dotenv import load_dotenv
from auth import require_user
from rag import router as rag_router

load_dotenv()

app = FastAPI(title="Euler Backend")

# Health
@app.get("/health")
def health():
    return {"status": "ok"}

# Protected route example
@app.get("/me")
def me(user = Depends(require_user)):
    return {
        "uid": user.get("uid"),
        "email": user.get("email"),
        "provider": user.get("firebase", {}).get("sign_in_provider"),
    }

# Include RAG endpoints
app.include_router(rag_router)
