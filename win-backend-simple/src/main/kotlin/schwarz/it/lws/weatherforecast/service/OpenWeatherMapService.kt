package schwarz.it.lws.weatherforecast.service

import io.github.cdimascio.dotenv.dotenv
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import schwarz.it.lws.weatherforecast.exception.WeatherServiceException
import schwarz.it.lws.weatherforecast.service.dto.WeatherApiResponse

@Service
class OpenWeatherMapService(
    private val restTemplate: RestTemplate,
    @Value("\${openweathermap.api.url}") private val apiUrl: String
) {

    private val dotenv = dotenv {
        directory = "./"
        ignoreIfMissing = true
    }

    // Get the OpenWeatherMap API key from .env file
    private val apiKey: String = dotenv["OPENWEATHERMAP_API_KEY"]

    fun fetchForecast(city: String, units: String = "metric"): WeatherApiResponse {
        val url = "$apiUrl?q=$city&appid=$apiKey&units=$units&lang=de"

        return try {
            restTemplate.getForObject(url, WeatherApiResponse::class.java)
                ?: throw WeatherServiceException("Keine Daten von OpenWeatherMap erhalten")
        } catch (ex: Exception) {
            throw WeatherServiceException("Fehler beim Abrufen der Wetterdaten: ${ex.message}", ex)
        }
    }
}
