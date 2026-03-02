# Win – Wetterinformationen

Eine Wetter-App mit Sprachsteuerung. Zeigt aktuelle Wettervorhersagen per Spracheingabe oder Stadtname an.

## Architektur

Reine Frontend-Applikation (Single Page App) ohne Backend:

- **`win-frontend/`** – Svelte 5 App (Vite), ruft die OpenWeatherMap API direkt aus dem Browser auf

## Features

- 5-Tages-Wettervorhersage (OWM `/forecast`-Endpunkt)
- Stündliches Temperaturdiagramm für den aktuellen Tag
- Luftfeuchtigkeit, Luftdruck, Windgeschwindigkeit
- Google Maps Einbettung der gesuchten Stadt
- **Sprachsteuerung** via Browser Speech API – Trigger: `"Wetter [Stadtname]"` (z.B. `"Wetter Berlin"`)
- Dynamisches Hintergrundvideo je nach Wetterbedingung und Tageszeit
- Zuletzt gesuchte Städte (localStorage)

## Voraussetzungen

- Node.js & npm (oder ein kompatibler Package Manager)
- [OpenWeatherMap API-Key](https://home.openweathermap.org/api_keys) (kostenloser Account)

## Setup

1. In das Frontend-Verzeichnis wechseln:
   ```bash
   cd win-frontend
   ```

2. Abhängigkeiten installieren:
   ```bash
   npm install
   ```

3. API-Key konfigurieren – Datei `.env.local` anlegen (wird nicht eingecheckt):
   ```bash
   cp .env.example .env.local
   ```
   Dann `VITE_OPENWEATHERMAP_API_KEY=dein_api_key` in `.env.local` eintragen.

4. Entwicklungsserver starten:
   ```bash
   npm run dev
   ```

Die App ist anschließend unter `http://localhost:5173` erreichbar.

## Build & Docker

Produktions-Build erstellen:
```bash
npm run build
```

Alternativ per Docker Compose (startet den Frontend-Container auf Port 80):
```bash
docker-compose up --build
```

## Projektstruktur

```
win-frontend/
├── src/
│   ├── App.svelte              # Hauptkomponente (State, Spracherkennung, Datenabruf)
│   └── lib/
│       ├── weatherService.js   # OWM API-Aufruf & Datentransformation
│       ├── Temp.svelte         # Aktuelle Temperatur & Wettericon
│       ├── Luft.svelte         # Luftfeuchtigkeit, Druck, Wind
│       ├── Hourly.svelte       # Stündliches Temperaturdiagramm
│       ├── Slider.svelte       # Tages-Karussell (Folgetage)
│       ├── Map.svelte          # Google Maps Einbettung
│       └── WeatherLottieIcon.svelte
├── .env.example                # Vorlage für den API-Key
└── vite.config.js
```

## OWM API

- Endpunkt: `https://api.openweathermap.org/data/2.5/forecast`
- Parameter: `q={Stadt}&lang=de&units=metric&cnt=40`
- Daten werden nach Tag gruppiert; Mittagseintrag (12:00 Uhr) dient als Tagesrepräsentant
