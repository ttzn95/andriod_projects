from datetime import datetime, timedelta, timezone
from secrets import token_urlsafe
from fastapi import FastAPI, HTTPException, UploadFile, File, Form, Request
from pydantic import BaseModel 
from fastapi.responses import FileResponse

import base64
import io
import qrcode
import os
import uuid

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
captures = {}
UPLOAD_DIR = "uploads"
QR_SESSION_MINUTES = 5
AUTHENTICATED_SESSION_MINUTES = 30

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
    expires_at = now + timedelta(minutes=QR_SESSION_MINUTES)

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
    session["expires_at"] = now + timedelta(minutes=AUTHENTICATED_SESSION_MINUTES)

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
    capture_type: str = Form(...),
    photo_type: str | None = Form(None),
    photo_side: str | None = Form(None),
    document_size: str | None = Form(None),
    custom_document_width_mm: float | None = Form(None),
    custom_document_height_mm: float | None = Form(None),
    color_mode: str = Form(...),
    orientation: str = Form(...),
    images: list[UploadFile] = File(...)
    ):
    print("========== IMAGE UPLOAD ==========")
    print("capture_token:", capture_token)
    print("capture_type:", capture_type)
    print("photo_type:", photo_type)
    print("photo_side:", photo_side)
    print("document_size:", document_size)
    print("custom_document_width_mm:", custom_document_width_mm)
    print("custom_document_height_mm:", custom_document_height_mm)
    print("color_mode:", color_mode)
    print("orientation:", orientation)
    print("image count:", len(images))
    print("==================================")

    # ---------------------------------------------------------
    # Find authenticated session
    # ---------------------------------------------------------

    session = None

    for session_token, current_session in sessions.items():

        if current_session.get("capture_token") == capture_token:
            session = current_session
            break

    if session is None:
        raise HTTPException(
            status_code=401,
            detail="Invalid capture token"
        )

    # ---------------------------------------------------------
    # Check session status
    # ---------------------------------------------------------

    if session["status"] != "AUTHENTICATED":
        raise HTTPException(
            status_code=401,
            detail="Capture session is not authenticated"
        )

    # ---------------------------------------------------------
    # Check expiration
    # ---------------------------------------------------------

    now = datetime.now(timezone.utc)

    if now >= session["expires_at"]:

        session["status"] = "EXPIRED"

        raise HTTPException(
            status_code=401,
            detail="Capture session has expired"
        )

    if capture_type == "PHOTO" and photo_side not in {"FRONT", "BACK"}:
        raise HTTPException(
            status_code=400,
            detail="Photo side must be FRONT or BACK"
        )

    if not images:
        raise HTTPException(
            status_code=400,
            detail="At least one image is required"
        )

    # ---------------------------------------------------------
    # Generate capture ID
    # ---------------------------------------------------------

    capture_id = str(uuid.uuid4()) 

    allowed_extensions = {".jpg", ".jpeg", ".png", ".svg"}
    image_records = []

    for sequence, image in enumerate(images):
        original_filename = image.filename or f"capture_{sequence}"
        extension = os.path.splitext(original_filename)[1].lower()

        if extension not in allowed_extensions:
            raise HTTPException(status_code=400, detail="Unsupported image format")

        stored_filename = f"{capture_id}_{sequence}{extension}"
        file_path = os.path.join(UPLOAD_DIR, stored_filename)
        contents = await image.read()

        with open(file_path, "wb") as file:
            file.write(contents)

        image_records.append({
            "sequence": sequence,
            "filename": original_filename,
            "content_type": image.content_type,
            "stored": str(file_path),
            "size": len(contents)
        })

    first_image = image_records[0]

    # ---------------------------------------------------------
    # Create capture record
    # ---------------------------------------------------------

    capture = {
        "capture_id": capture_id,
        "staff_id": session["staff_id"],
        "capture_type": capture_type,
        "photo_type": photo_type,
        "photo_side": photo_side,
        "document_size": document_size,
        "custom_document_width_mm": custom_document_width_mm,
        "custom_document_height_mm": custom_document_height_mm,
        "color_mode": color_mode,
        "orientation": orientation,
        "filename": first_image["filename"],
        "content_type": first_image["content_type"],
        "stored": first_image["stored"],
        "images": image_records,
        "created_at": datetime.now(timezone.utc).isoformat(),
        "status": "UPLOADED"
    }

    captures[capture_id] = capture

    print("========== CAPTURE SAVED ==========")
    print("capture_id:", capture_id)
    print("staff_id:", session["staff_id"])
    print("image count:", len(image_records))
    print("===================================")

    return {
        "success": True,
        "message": "Image uploaded successfully",
        "capture_id": capture_id,
        "filename": first_image["filename"],
        "image_count": len(image_records),
        "photo_side": photo_side,
        "staff_id": session["staff_id"],
        "status": "UPLOADED",
        "uploaded_at": now.isoformat()
    }

@app.get("/api/capture/{capture_id}")
async def get_capture(capture_id: str):

    capture = captures.get(capture_id)

    if capture is None:
        raise HTTPException(
            status_code=404,
            detail="Capture not found"
        )

    return {
        "success": True,
        "capture": capture
    }

@app.get("/api/captures")
async def list_captures(staff_id: str):

    results = []

    for capture in captures.values():

        if capture.get("staff_id") == staff_id:
            results.append(capture)

    return {
        "success": True,
        "count": len(results),
        "captures": results
    }

@app.get("/api/capture/{capture_id}/image")
async def get_capture_image(capture_id: str):

    capture = captures.get(capture_id)

    if capture is None:
        raise HTTPException(
            status_code=404,
            detail="Capture not found"
        )

    file_path = capture.get("stored")

    if not file_path:
        raise HTTPException(
            status_code=404,
            detail="Capture file not found"
        )

    if not os.path.exists(file_path):
        raise HTTPException(
            status_code=404,
            detail="Stored image does not exist"
        )

    return FileResponse(
        path=file_path,
        media_type=capture.get("content_type") or "application/octet-stream",
        filename=capture["filename"]
    )