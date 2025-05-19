import re
from typing import Dict, Optional, List, Any


class WeatherExtractor:
    """
    Service for extracting weather-related information from text.
    """

    def __init__(self):
        """
        Initialize the WeatherExtractor with word lists for pattern matching.
        """
        # Words that indicate a weather query
        self.weather_words: List[str] = [
            "wetter", "temperatur", "regen", "schnee", "sonne", "wind",
            "kalt", "warm", "gewitter", "niederschlag", "bewölkt", "wolken",
            "grad", "celsius", "vorhersage"
        ]

        # Time-related words
        self.today_words: List[str] = ["heute", "jetzt", "aktuell"]
        self.tomorrow_words: List[str] = ["morgen"]
        self.week_words: List[str] = ["woche", "tage", "übermorgen"]

    def extract(self, text: str) -> Dict[str, Any]:
        if not text:
            return {"is_weather_query": False, "location": None, "time_period": "today"}

        # Convert to lowercase for case-insensitive matching
        text = text.lower()

        # Check if this is a weather query
        is_weather = any(word in text for word in self.weather_words)

        # Find location (city)
        location = self._extract_potential_location_phrase(text)

        # If a city was found, it's definitely a weather query
        if location:
            is_weather = True

        # Determine time period
        time_period = self._extract_time_period(text)

        return {
            "is_weather_query": is_weather,
            "location": location,
            "time_period": time_period
        }

    def _extract_potential_location_phrase(self, text: str) -> Optional[str]:
        """
        Extrahiert den Textteil, der den Stadtnamen enthalten könnte.
        Nimmt im Wesentlichen alles nach bekannten Wetter-Phrasen.
        """
        # Dein Frontend sendet "wetter stuttgart", d.h. "potential_command_phrase" im controller
        # ist bereits "stuttgart". Dieser Text kommt hier als `text` an.
        # Daher ist es am einfachsten, den gesamten Input-Text als potenziellen Ort zu betrachten.
        # Die Validierung erfolgt dann durch das Kotlin-Backend.

        # Wenn der Text nach der Entfernung von Zeitwörtern noch Substanz hat,
        # nehmen wir an, es ist der Stadtname.
        temp_text = text
        for time_word_list in [self.today_words, self.tomorrow_words, self.week_words]:
            for word in time_word_list:
                temp_text = temp_text.replace(word, "").strip()

        # Entferne auch gängige Füllwörter, die keine Städte sind
        # (optional, kann aber helfen, das Rauschen zu reduzieren)
        # common_fillers = ["in", "für", "fuer", "bitte", "nach", "von"]
        # for filler in common_fillers:
        #    temp_text = temp_text.replace(filler, "").strip()

        if temp_text: # Wenn nach Bereinigung noch etwas übrig ist
            return temp_text.strip()

        return None # Kein valider Kandidat für einen Ort gefunden


    def _extract_time_period(self, text: str) -> str:
        # (Diese Funktion kann so bleiben wie sie ist)
        time_period = "today"
        if any(word in text for word in self.tomorrow_words):
            time_period = "tomorrow"
        if any(word in text for word in self.week_words):
            time_period = "week"
        return time_period
