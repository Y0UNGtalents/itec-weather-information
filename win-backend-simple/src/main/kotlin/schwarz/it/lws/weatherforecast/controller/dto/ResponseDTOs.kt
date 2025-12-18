package schwarz.it.lws.weatherforecast.controller.dto

import schwarz.it.lws.weatherforecast.model.DailyForecast

data class DayTemperatureDto(
    val temperature: Double,
    val forecastDate: String
)

data class DaySummaryDto(
    val city: String,
    val forecastDate: String,
    val temperature: Double,
    val minTemperature: Double,
    val maxTemperature: Double,
    val iconCode: String,
    val description: String,
    val humidity: Int,
    val dayTemperatures: List<DayTemperatureDto>
)

data class ForecastsSummaryDto(
    val forcasts: List<DaySummaryDto>,
)

data class ErrorResponse(
    val code: String,
    val message: String
)
