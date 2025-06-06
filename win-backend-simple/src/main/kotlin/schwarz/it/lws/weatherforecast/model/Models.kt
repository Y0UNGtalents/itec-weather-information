package schwarz.it.lws.weatherforecast.model

import schwarz.it.lws.weatherforecast.service.dto.City
import schwarz.it.lws.weatherforecast.service.dto.WeatherEntry
import java.time.LocalDate

data class DailyForecast(
    val date: LocalDate,
    val minTemperature: Double,
    val maxTemperature: Double,
    val temperature: Double,
    val averageHumidity: Int,
    val dominantIcon: String,
    val dominantWeatherMain: String,
    val dominantDescription: String,
    val precipitationProbability: Double,
    val averageWindSpeed: Double,
    val averagePressure: Int,
    val hourlyEntries: List<WeatherEntry>
)

data class FiveDayForecast(
    val city: City,
    val dailyForecasts: List<DailyForecast>,
)
