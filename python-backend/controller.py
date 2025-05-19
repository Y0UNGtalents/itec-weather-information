import os
# import tempfile # Nicht mehr direkt für Audio-Dateien von Client benötigt
from fastapi import FastAPI, WebSocket, WebSocketDisconnect # HTTPException entfernt, da nicht direkt genutzt
from fastapi.middleware.cors import CORSMiddleware
import json
import httpx
from urllib.parse import quote as url_quote


# Annahme: extractorService.py ist im selben Verzeichnis oder im Python-Pfad
from extractorService import WeatherExtractor

# Die URL deines Kotlin-Backends
# Innerhalb von Docker über den Service-Namen (aus docker-compose.yml)
KOTLIN_BACKEND_URL = "http://win-backend:8080/api/weather"

app = FastAPI(
    title="Win Python Processing Backend",
    description="Handles voice commands and fetches weather data via Kotlin backend.",
    version="1.0.0"
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], # In Produktion spezifischer machen, z.B. "http://localhost", "http://localhost:80"
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

weather_extractor = WeatherExtractor() # Instanz deines Extractors

class ConnectionManager:
    def __init__(self):
        self.active_connections: list[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)
        print(f"Client {websocket.client} connected. Total clients: {len(self.active_connections)}")

    def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)
        print(f"Client {websocket.client} disconnected. Total clients: {len(self.active_connections)}")

    async def send_json_message(self, message: dict, websocket: WebSocket):
        try:
            await websocket.send_json(message)
        except Exception as e:
            print(f"Error sending JSON message to {websocket.client}: {e}")
            # Verbindung könnte bereits geschlossen sein, hier nicht erneut disconnecten

manager = ConnectionManager()

@app.on_event("startup")
async def startup_event():
    # Erstelle einen globalen HTTP-Client für die Anwendung
    app.state.http_client = httpx.AsyncClient(timeout=10.0) # Timeout von 10 Sekunden
    print("HTTPX AsyncClient started.")

@app.on_event("shutdown")
async def shutdown_event():
    await app.state.http_client.aclose()
    print("HTTPX AsyncClient closed.")


async def get_weather_from_kotlin_backend(city_name: str, http_client: httpx.AsyncClient) -> dict | None:
    """Holt Wetterdaten vom Kotlin-Backend."""
    if not city_name:
        print("get_weather_from_kotlin_backend: No city name provided.")
        return None

    #encoded_city_name = httpx.utils.quote(city_name) # URL-Kodierung für Städtenamen mit Sonderzeichen/Leerzeichen
    encoded_city_name = url_quote(city_name)
    url = f"{KOTLIN_BACKEND_URL}/{encoded_city_name}"
    print(f"Requesting weather from Kotlin backend: {url}")

    try:
        response = await http_client.get(url)
        response.raise_for_status() # Löst einen Fehler aus bei 4xx/5xx Statuscodes
        print(f"Kotlin backend response status: {response.status_code} for city {city_name}")
        return response.json()
    except httpx.HTTPStatusError as e:
        print(f"HTTP error calling Kotlin backend for city '{city_name}': {e.response.status_code} - {e.response.text}")
        return {"error": f"Kotlin backend error: {e.response.status_code}", "details": e.response.text}
    except httpx.RequestError as e: # Deckt Verbindungsfehler, Timeouts etc. ab
        print(f"Request error calling Kotlin backend for city '{city_name}': {e}")
        return {"error": "Could not connect to weather service (Kotlin backend)."}
    except json.JSONDecodeError as e:
        print(f"JSON decode error from Kotlin backend for city '{city_name}': {e}")
        return {"error": "Invalid response format from weather service."}
    except Exception as e:
        print(f"Unexpected error calling Kotlin backend for city '{city_name}': {type(e).__name__} - {e}")
        return {"error": f"An unexpected error occurred: {str(e)}"}


@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await manager.connect(websocket)
    http_client = app.state.http_client # Nutze den globalen HTTP-Client

    try:
        while True:
            raw_data = await websocket.receive_text()
            try:
                data = json.loads(raw_data)
            except json.JSONDecodeError:
                print(f"Received non-JSON message from {websocket.client}: {raw_data}")
                await manager.send_json_message({"type": "error_from_python", "message": "Invalid message format received."}, websocket)
                continue

            message_type = data.get("type")
            text_content = data.get("text")

            print(f"Received from {websocket.client}: type='{message_type}', text='{text_content}'")

            if message_type == "transcription_frontend" and isinstance(text_content, str):
                normalized_text = text_content.lower().strip()
                trigger_word_backend = "wetter" # Serverseitiges Trigger-Wort

                if normalized_text.startswith(trigger_word_backend):
                    potential_command_phrase = normalized_text[len(trigger_word_backend):].strip()

                    if not potential_command_phrase:
                        print(f"No command phrase after trigger word in: '{normalized_text}'")
                        await manager.send_json_message({
                            "type": "error_from_python", # Geändet von no_action_needed zu error für klareres Feedback
                            "message": "Bitte geben Sie nach 'Wetter' einen Stadtnamen an."
                        }, websocket)
                        continue

                    print(f"Potential command phrase for extractor: '{potential_command_phrase}'")

                    extracted_info = weather_extractor.extract(potential_command_phrase)
                    extracted_city = extracted_info.get("location")

                    if extracted_city:
                        print(f"City extracted by WeatherExtractor: '{extracted_city}'. Fetching weather...")

                        weather_payload = await get_weather_from_kotlin_backend(extracted_city, http_client)

                        if weather_payload and "error" not in weather_payload:
                            await manager.send_json_message({
                                "type": "command_understood_display_weather",
                                "city": extracted_city, # Die vom Extractor gefundene Stadt
                                "weatherPayload": weather_payload
                            }, websocket)
                        else: # Fehler beim Abrufen vom Kotlin-Backend oder Kotlin-Backend gab Fehler zurück
                            error_message = f"Wetterdaten für '{extracted_city}' konnten nicht abgerufen werden."
                            if weather_payload and "details" in weather_payload:
                                error_message += f" Details: {weather_payload['details']}"
                            elif weather_payload and "error" in weather_payload:
                                error_message += f" Grund: {weather_payload['error']}"

                            await manager.send_json_message({
                                "type": "error_from_python",
                                "message": error_message
                            }, websocket)
                    else:
                        print(f"WeatherExtractor found no city in: '{potential_command_phrase}' (Original: '{text_content}')")
                        await manager.send_json_message({
                            "type": "error_from_python", # Geändet von no_action_needed zu error
                            "message": f"Keine Stadt in '{potential_command_phrase}' erkannt. Versuchen Sie z.B. 'Wetter Berlin'."
                        }, websocket)
                else:
                    # Kein Trigger-Wort, aber Text wurde gesendet. Hier nichts tun oder Logik für andere Befehle.
                    print(f"Trigger word '{trigger_word_backend}' not found at start of: '{normalized_text}'. Ignoring.")
                    # Optional: Sende eine "nicht verstanden" Nachricht, wenn immer Feedback gewünscht ist.
                    # await manager.send_json_message({
                    #     "type": "no_action_needed",
                    #     "reason": "Trigger word not found or command not recognized."
                    # }, websocket)
            elif message_type: # Nachrichtentyp vorhanden, aber nicht "transcription_frontend" oder kein Text
                print(f"Received unhandled message type '{message_type}' or missing text from {websocket.client}")
                await manager.send_json_message({"type": "error_from_python", "message": "Unbekannter Nachrichtentyp oder fehlender Text."}, websocket)
            else: # Weder Typ noch Text
                print(f"Received invalid/empty message structure from {websocket.client}: {data}")
                await manager.send_json_message({"type": "error_from_python", "message": "Ungültige Nachricht erhalten."}, websocket)


    except WebSocketDisconnect:
        print(f"WebSocket disconnected by client: {websocket.client}")
    except ConnectionResetError: # Kann bei abruptem Schließen auftreten
        print(f"WebSocket connection reset by client: {websocket.client}")
    except Exception as e:
        print(f"Unexpected error in WebSocket endpoint for {websocket.client}: {type(e).__name__} - {e}")
        # Versuche, einen Fehler an den Client zu senden, wenn möglich
        try:
            # Prüfe, ob die Verbindung noch als aktiv im Manager geführt wird,
            # bevor ein Sendeversuch unternommen wird.
            if websocket in manager.active_connections:
                await manager.send_json_message({
                    "type": "error_from_python",
                    "message": "Ein interner Serverfehler ist aufgetreten."
                }, websocket)
        except Exception as send_exc:
            print(f"Could not send error to client {websocket.client} after main exception: {send_exc}")
    finally:
        manager.disconnect(websocket)

@app.get("/")
async def root():
    return {
        "message": "Python Processing Backend is running",
        "status": "healthy",
        "services": {
            "weather_extractor_ready": True,
            "kotlin_backend_target": KOTLIN_BACKEND_URL
        }
    }