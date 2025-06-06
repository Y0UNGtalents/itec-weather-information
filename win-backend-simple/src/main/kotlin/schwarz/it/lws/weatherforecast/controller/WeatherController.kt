package schwarz.it.lws.weatherforecast.controller

import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import schwarz.it.lws.weatherforecast.controller.dto.DaySummaryDto
import schwarz.it.lws.weatherforecast.controller.dto.ForecastsSummaryDto
import schwarz.it.lws.weatherforecast.model.FiveDayForecast
import schwarz.it.lws.weatherforecast.service.WeatherForecastService

@RestController
@RequestMapping("/api/weather")
@Validated
class WeatherController(private val weatherForecastService: WeatherForecastService) {

    @GetMapping("/raw/{city}")
    fun getForecast(
        @PathVariable city: String,
    ): ResponseEntity<FiveDayForecast> {
        val forecast = weatherForecastService.getFiveDayForecast(city)
        return ResponseEntity.ok(forecast)
    }

    @GetMapping("/{city}")
    fun getForecastSummary(
        @PathVariable city: String,
    ): ResponseEntity<List<DaySummaryDto>> {

        val forecast = weatherForecastService.getFiveDayForecast(city)
        val dailyForecasts = forecast.dailyForecasts.map { day ->
            DaySummaryDto(
                city = city,
                forecastDate = day.date.toString(),
                minTemperature = day.minTemperature,
                maxTemperature = day.maxTemperature,
                temperature = day.temperature,
                iconCode = day.dominantIcon,
                description = day.dominantDescription,
                humidity = day.averageHumidity,
            )
        }

        return ResponseEntity.ok(dailyForecasts)
    }
}
