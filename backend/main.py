from datetime import datetime, timedelta, timezone
from secrets import token_urlsafe
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="Smart Capture API")


# ---------------------------------------------------------
# Temporary in-memory data for development only
# ---------------------------------------------------------

sessions = {}

staff_users = {
    "EMP001": {
        "password": "Password123!",
        "name": "Test Staff",
        "authorized": True
    }
}


class LoginRequest(BaseModel):
    session_token: str
    staff_id: str
    password: str


@app.get("/")
def root():
    return {
        "application": "Smart Capture API",
        "status": "running"
    }


@app.post("/api/sessions")
def create_session():

    session_token = token_urlsafe(32)

    now = datetime.now(timezone.utc)
    expires_at = now + timedelta(minutes=5)

    sessions[session_token] = {
        "created_at": now,
        "expires_at": expires_at,
        "status": "CREATED",
        "staff_id": None,
        "capture_token": None
    }

    return {
        "session_token": session_token,
        "expires_at": expires_at.isoformat(),
        "status": "CREATED"
    }


@app.post("/api/auth/login")
def login(request: LoginRequest):

    session = sessions.get(request.session_token)

    if session is None:
        raise HTTPException(
            status_code=401,
            detail="Invalid capture session"
        )

    now = datetime.now(timezone.utc)

    if now >= session["expires_at"]:
        session["status"] = "EXPIRED"

        raise HTTPException(
            status_code=401,
            detail="Capture session has expired"
        )

    if session["status"] != "CREATED":
        raise HTTPException(
            status_code=401,
            detail="Capture session is not available"
        )

    staff = staff_users.get(request.staff_id)

    if staff is None:
        raise HTTPException(
            status_code=401,
            detail="Invalid staff ID or password"
        )

    if staff["password"] != request.password:
        raise HTTPException(
            status_code=401,
            detail="Invalid staff ID or password"
        )

    if not staff["authorized"]:
        raise HTTPException(
            status_code=403,
            detail="Staff is not authorized for mobile capture"
        )

    capture_token = token_urlsafe(32)

    session["status"] = "AUTHENTICATED"
    session["staff_id"] = request.staff_id
    session["capture_token"] = capture_token

    return {
        "success": True,
        "staff_id": request.staff_id,
        "staff_name": staff["name"],
        "capture_token": capture_token,
        "expires_at": session["expires_at"].isoformat()
    }
