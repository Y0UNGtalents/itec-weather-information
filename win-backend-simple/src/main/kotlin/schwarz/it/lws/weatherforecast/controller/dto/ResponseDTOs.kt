package schwarz.it.lws.weatherforecast.controller.dto

data class DaySummaryDto(
    val city: String,
    val forecastDate: String,
    val temperature: Double,
    val minTemperature: Double,
    val maxTemperature: Double,
    val iconCode: String,
    val description: String,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,

)

data class ForecastsSummaryDto(
    val forcasts: List<DaySummaryDto>,
)

data class ErrorResponse(
    val code: String,
    val message: String
)
