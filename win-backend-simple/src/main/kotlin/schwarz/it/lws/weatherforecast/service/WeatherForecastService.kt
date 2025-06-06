package schwarz.it.lws.weatherforecast.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import schwarz.it.lws.weatherforecast.model.DailyForecast
import schwarz.it.lws.weatherforecast.model.FiveDayForecast
import schwarz.it.lws.weatherforecast.service.dto.WeatherEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

@Service
class WeatherForecastService(
    private val weatherApiService: OpenWeatherMapService,
    private val iconSelectionStrategy: IconSelectionStrategy
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WeatherForecastService::class.java)
    }

    fun getFiveDayForecast(city: String): FiveDayForecast {
        logger.info("Erstelle 5-Tage Vorhersage für: city=$city")

        val apiResponse = weatherApiService.fetchForecast(city)
        val groupedByDate = groupEntriesByDate(apiResponse.list)

        val today = LocalDate.now()

        // 5 Tage verarbeiten
        val forcasts = (0..4).mapNotNull { dayOffset ->
            val date = today.plusDays(dayOffset.toLong())
            val entries = groupedByDate[date]
            entries?.let { createDailyForecast(date, it) }
        }

        logger.info("5-Tage Vorhersage erfolgreich erstellt für ${apiResponse.city.name}")

        return FiveDayForecast(
            city = apiResponse.city,
            dailyForecasts = forcasts,
        )
    }

    private fun groupEntriesByDate(entries: List<WeatherEntry>): Map<LocalDate, List<WeatherEntry>> {
        return entries.groupBy { entry ->
            Instant.ofEpochSecond(entry.dt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
    }

    private fun createDailyForecast(
        date: LocalDate,
        entries: List<WeatherEntry>,
    ): DailyForecast {

        if (entries.isEmpty()) {
            logger.warn("Keine Einträge für Datum $date gefunden")
            return createEmptyDailyForecast(date)
        }

        val temperatures = entries.map { it.main.temp }
        val humidities = entries.map { it.main.humidity }
        val temperature = if (date == LocalDate.now()) temperatures[0] else Math.round(temperatures.average() * 100) / 100.0

        return DailyForecast(
            date = date,
            temperature = temperature,
            minTemperature = temperatures.minOrNull() ?: 0.0,
            maxTemperature = temperatures.maxOrNull() ?: 0.0,
            averageHumidity = humidities.average().roundToInt(),
            dominantIcon = findDominantIcon(entries),
            dominantWeatherMain = findDominantWeatherMain(entries),
            dominantDescription = findDominantDescription(entries),
            precipitationProbability = entries.map { it.pop }.maxOrNull() ?: 0.0,
            averageWindSpeed = entries.map { it.wind.speed }.average(),
            averagePressure = entries.map { it.main.pressure }.average().roundToInt(),
            hourlyEntries = entries
        )
    }

    private fun findDominantWeatherMain(entries: List<WeatherEntry>): String {
        val forcast12 = entries.find { it.dt_txt.endsWith("12:00:00") }
        if (forcast12 != null) {
            return forcast12.weather[0].main
        } else {
            return entries
                .flatMap { it.weather }
                .groupingBy { it.main }
                .eachCount()
                .maxByOrNull { it.value }?.key ?: "Clear"
        }
    }

    private fun findDominantDescription(entries: List<WeatherEntry>): String {

        val forcast12 = entries.find { it.dt_txt.endsWith("12:00:00") }
        if (forcast12 != null) {
            return forcast12.weather[0].description
        } else {
            return entries
                .flatMap { it.weather }
                .groupingBy { it.description }
                .eachCount()
                .maxByOrNull { it.value }?.key ?: "Klarer Himmel"
        }
    }

    private fun findDominantIcon(entries: List<WeatherEntry>): String {

        val forcast12 = entries.find { it.dt_txt.endsWith("12:00:00") }
        if (forcast12 != null) {
            return forcast12.weather[0].icon
        } else {
            return iconSelectionStrategy.selectDominantIcon(entries)
        }
    }

    private fun createEmptyDailyForecast(date: LocalDate): DailyForecast {
        return DailyForecast(
            date = date,
            minTemperature = 0.0,
            maxTemperature = 0.0,
            temperature = 0.0,
            averageHumidity = 0,
            dominantIcon = "01d",
            dominantWeatherMain = "Unknown",
            dominantDescription = "Keine Daten verfügbar",
            precipitationProbability = 0.0,
            averageWindSpeed = 0.0,
            averagePressure = 1013,
            hourlyEntries = emptyList()
        )
    }
}
