from datetime import datetime, timedelta, timezone
from secrets import token_urlsafe
from fastapi import FastAPI, HTTPException, UploadFile, File, Form
from pydantic import BaseModel

import base64
import io
import qrcode
import os

app = FastAPI(title="Smart Capture API")

def generate_qr_base64(data: str) -> str:
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_M,
        box_size=10,
        border=4,
    )

    qr.add_data(data)
    qr.make(fit=True)

    image = qr.make_image()

    buffer = io.BytesIO()
    image.save(buffer, format="PNG")

    encoded = base64.b64encode(
        buffer.getvalue()
    ).decode("utf-8")

    return f"data:image/png;base64,{encoded}"

# ---------------------------------------------------------
# Temporary in-memory data for development only
# ---------------------------------------------------------

sessions = {}
UPLOAD_DIR = "uploads"

os.makedirs(
    UPLOAD_DIR,
    exist_ok=True
)
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

    # Generate a random session token
    session_token = token_urlsafe(32)

    now = datetime.now(timezone.utc)
    expires_at = now + timedelta(minutes=5)

    # Store session in memory
    sessions[session_token] = {
        "created_at": now,
        "expires_at": expires_at,
        "status": "CREATED",
        "staff_id": None,
        "capture_token": None
    }

    # Generate QR code containing the session token
    qr_code = generate_qr_base64(session_token)

    print("========== SESSION REQUEST ==========")
    print("session token:", session_token)
    print("expires_at:", expires_at.isoformat())
    print("=====================================")

    return {
        "success": True,
        "session_token": session_token,
        "qr_code": qr_code,
        "expires_at": expires_at.isoformat(),
        "status": "CREATED"
    }


@app.post("/api/auth/login")
def login(request: LoginRequest):
    print("========== LOGIN REQUEST ==========")
    print("staff_id:", request.staff_id)
    print("session_token present:", bool(request.session_token))
    print("session_token value:", request.session_token)
    print("password present:", bool(request.password))
    print("===================================")

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

@app.post("/api/capture/upload")
async def upload_capture(
    capture_token: str = Form(...),
    image: UploadFile = File(...)
    ):

    print("========== IMAGE UPLOAD ==========")
    print("capture_token present:", bool(capture_token))
    print("filename:", image.filename)
    print("content_type:", image.content_type)
    print("token:", capture_token)
    print("==================================")

    # ---------------------------------------------------------
    # Validate capture token
    # ---------------------------------------------------------

    authenticated_session = None

    for session in sessions.values():

        if session.get("capture_token") == capture_token:

            authenticated_session = session

            break

    if authenticated_session is None:

        raise HTTPException(
            status_code=401,
            detail="Invalid capture token"
        )

    # ---------------------------------------------------------
    # Check session expiration
    # ---------------------------------------------------------

    now = datetime.now(timezone.utc)

    if now >= authenticated_session["expires_at"]:

        authenticated_session["status"] = "EXPIRED"

        raise HTTPException(
            status_code=401,
            detail="Capture session has expired"
        )

    # ---------------------------------------------------------
    # Check session status
    # ---------------------------------------------------------

    if authenticated_session["status"] != "AUTHENTICATED":

        raise HTTPException(
            status_code=401,
            detail="Capture session is not authenticated"
        )

    # ---------------------------------------------------------
    # Validate uploaded file
    # ---------------------------------------------------------

    if image.content_type not in [
        "image/jpeg",
        "image/png"
    ]:

        raise HTTPException(
            status_code=400,
            detail="Only JPEG and PNG images are supported"
        )

    # ---------------------------------------------------------
    # Read image
    # ---------------------------------------------------------

    image_data = await image.read()

    if not image_data:

        raise HTTPException(
            status_code=400,
            detail="Uploaded image is empty"
        )

    # ---------------------------------------------------------
    # Generate filename
    # ---------------------------------------------------------

    timestamp = datetime.now().strftime(
        "%Y%m%d_%H%M%S_%f"
    )

    extension = ".jpg"

    if image.content_type == "image/png":
        extension = ".png"

    filename = (
        f"{authenticated_session['staff_id']}_"
        f"{timestamp}"
        f"{extension}"
    )

    file_path = os.path.join(
        UPLOAD_DIR,
        filename
    )

    # ---------------------------------------------------------
    # Save image
    # ---------------------------------------------------------

    with open(file_path, "wb") as output_file:

        output_file.write(image_data)

    print("Image saved:", file_path)

    return {
        "success": True,
        "message": "Image uploaded successfully",
        "filename": filename,
        "staff_id": authenticated_session["staff_id"],
        "size": len(image_data)
    }