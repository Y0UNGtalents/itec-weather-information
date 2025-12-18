package schwarz.it.lws.weatherforecast.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import schwarz.it.lws.weatherforecast.controller.dto.DayTemperatureDto
import schwarz.it.lws.weatherforecast.model.DailyForecast
import schwarz.it.lws.weatherforecast.model.DayTemperature
import schwarz.it.lws.weatherforecast.model.FiveDayForecast
import schwarz.it.lws.weatherforecast.service.dto.WeatherEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Service
class WeatherForecastService(
    private val weatherApiService: OpenWeatherMapService
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
        val forecasts = (0..4).mapNotNull { dayOffset ->
            val date = today.plusDays(dayOffset.toLong())
            val entries = groupedByDate[date]
            entries?.let { createDailyForecast(date, it) }
        }

        logger.info("5-Tage Vorhersage erfolgreich erstellt für ${apiResponse.city.name}")

        return FiveDayForecast(
            city = apiResponse.city,
            dailyForecasts = forecasts,
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

        val temperature: Double
        val humidity: Int
        val iconCode: String
        val description: String

        val forcast12 = entries.find { it.dt_txt.endsWith("12:00:00") }
        if (forcast12 != null) {
            temperature = forcast12.main.temp
            humidity = forcast12.main.humidity
            description = forcast12.weather[0].description
            iconCode = forcast12.weather[0].icon
        } else {
            temperature = entries[0].main.temp
            humidity = entries[0].main.humidity
            description = entries[0].weather[0].description
            iconCode = entries[0].weather[0].icon
        }

        val dayTemperatures = entries.map { entry ->
            DayTemperature(
                temperature = entry.main.temp,
                forecastDate = entry.dt_txt
            )
        }

        return DailyForecast(
            date = date,
            temperature = temperature,
            minTemperature = temperatures.minOrNull() ?: 0.0,
            maxTemperature = temperatures.maxOrNull() ?: 0.0,
            humidity = humidity,
            iconCode = iconCode,
            description = description,
            dayTemperatures = dayTemperatures
        )
    }

    private fun createEmptyDailyForecast(date: LocalDate): DailyForecast {
        return DailyForecast(
            date = date,
            minTemperature = 0.0,
            maxTemperature = 0.0,
            temperature = 0.0,
            humidity = 0,
            iconCode = "01d",
            description = "Keine Daten verfügbar",
            dayTemperatures = emptyList(),
        )
    }
}
