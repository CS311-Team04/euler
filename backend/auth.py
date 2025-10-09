import os
from fastapi import Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import firebase_admin
from firebase_admin import credentials as fb_credentials, auth as fb_auth

# Load environment
AUTH_MODE = os.getenv("AUTH_MODE", "none").lower()
ALLOWED_EMAIL_DOMAIN = os.getenv("ALLOWED_EMAIL_DOMAIN")
ALLOWED_TENANT_ID = os.getenv("ALLOWED_TENANT_ID")

bearer = HTTPBearer(auto_error=False) #parse the Authorization header

# Initialize Firebase Admin only once
if AUTH_MODE == "firebase" and not firebase_admin._apps:
    cred = fb_credentials.ApplicationDefault()  # reads GOOGLE_APPLICATION_CREDENTIALS
    firebase_admin.initialize_app(cred)


def require_user(creds: HTTPAuthorizationCredentials = Depends(bearer)):
    """Verify Firebase ID token and return decoded claims."""
    if AUTH_MODE != "firebase":
        return {"uid": "anonymous", "email": None}

    if creds is None or creds.scheme.lower() != "bearer":
        raise HTTPException(401, "Missing bearer token")

    try:
        decoded = fb_auth.verify_id_token(creds.credentials, check_revoked=True)
    except Exception:
        raise HTTPException(401, "Invalid or expired token")

    # Optional domain restriction
    email = decoded.get("email")
    if ALLOWED_EMAIL_DOMAIN and (not email or not email.endswith(f"@{ALLOWED_EMAIL_DOMAIN}")):
        raise HTTPException(403, "Forbidden: email domain not allowed")

    # Optional tenant restriction (if your teammate adds custom claim microsoft_tid)
    if ALLOWED_TENANT_ID and decoded.get("microsoft_tid") != ALLOWED_TENANT_ID:
        raise HTTPException(403, "Forbidden: tenant not allowed")

    return decoded
