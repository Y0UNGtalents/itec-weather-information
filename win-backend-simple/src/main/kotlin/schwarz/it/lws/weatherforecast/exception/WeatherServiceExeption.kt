package schwarz.it.lws.weatherforecast.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import schwarz.it.lws.weatherforecast.controller.dto.ErrorResponse

class WeatherServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

@ControllerAdvice
class WeatherExceptionHandler {

    @ExceptionHandler(WeatherServiceException::class)
    fun handleWeatherServiceException(ex: WeatherServiceException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse("WEATHER_SERVICE_ERROR", ex.message ?: "Unbekannter Fehler"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.badRequest()
            .body(ErrorResponse("VALIDATION_ERROR", "Ungültige Parameter"))
    }
}

