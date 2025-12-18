package schwarz.it.lws.weatherforecast.model

import schwarz.it.lws.weatherforecast.service.dto.City
import java.time.LocalDate

data class DailyForecast(
    val date: LocalDate,
    val temperature: Double,
    val minTemperature: Double,
    val maxTemperature: Double,
    val humidity: Int,
    val iconCode: String,
    val description: String,
    val pressure: Int,
    val windSpeed: Double,
)

data class FiveDayForecast(
    val city: City,
    val dailyForecasts: List<DailyForecast>,
    
)

