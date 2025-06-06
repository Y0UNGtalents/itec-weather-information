package schwarz.it.lws.weatherforecast.service

import org.springframework.stereotype.Component
import schwarz.it.lws.weatherforecast.service.dto.WeatherEntry

interface IconSelectionStrategy {
    fun selectDominantIcon(entries: List<WeatherEntry>): String
}

@Component
class WeightedIconSelectionStrategy : IconSelectionStrategy {

    companion object {
        // Gewichtung verschiedener Wetter-IDs (höhere Werte = wichtiger)
        private val WEATHER_PRIORITY = mapOf(
            // Schwere Wetterereignisse (höchste Priorität)
            200 to 100, 201 to 100, 202 to 100, // Gewitter mit Regen
            230 to 95, 231 to 95, 232 to 95,    // Gewitter mit Nieselregen

            // Regen (hohe Priorität)
            500 to 80, 501 to 85, 502 to 90,    // Leichter bis starker Regen
            520 to 75, 521 to 80, 522 to 85,    // Schauer

            // Schnee (hohe Priorität)
            600 to 85, 601 to 90, 602 to 95,    // Leichter bis starker Schnee

            // Nebel/Dunst (mittlere Priorität)
            701 to 60, 741 to 65,               // Nebel

            // Bewölkung (niedrige Priorität)
            804 to 40,                           // Bedeckt
            803 to 30,                           // Überwiegend bewölkt
            802 to 20,                           // Mäßig bewölkt
            801 to 10,                           // Leicht bewölkt
            800 to 5                             // Klar
        )
    }

    override fun selectDominantIcon(entries: List<WeatherEntry>): String {
        if (entries.isEmpty()) return "01d"

        // Zähle Vorkommen jeder Icon-ID mit Gewichtung
        val iconFrequency = mutableMapOf<String, Double>()

        entries.forEach { entry ->
            entry.weather.forEach { weather ->
                val priority = WEATHER_PRIORITY[weather.id] ?: 1
                val currentValue = iconFrequency.getOrDefault(weather.icon, 0.0)
                iconFrequency[weather.icon] = currentValue + priority
            }
        }

        // Wähle Icon mit höchster gewichteter Häufigkeit
        return iconFrequency.maxByOrNull { it.value }?.key ?: "01d"
    }

    /**
     * Gibt die Priorität für eine Weather-ID zurück
     * Wird vom Service für konsistente Weather-Auswahl verwendet
     */
    fun getWeatherPriority(weatherId: Int): Int {
        return WEATHER_PRIORITY[weatherId] ?: 1
    }
}