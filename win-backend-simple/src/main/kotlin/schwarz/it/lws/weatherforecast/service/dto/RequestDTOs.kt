package schwarz.it.lws.weatherforecast.service.dto

data class WeatherApiResponse(
    val cod: String,
    val message: Int,
    val cnt: Int,
    val list: List<WeatherEntry>,
    val city: City
)

data class WeatherEntry(
    val dt: Long,
    val main: MainWeatherData,
    val weather: List<WeatherDescription>,
    val clouds: Clouds,
    val wind: Wind,
    val visibility: Int,
    val pop: Double,
    val rain: Rain?,
    val sys: Sys,
    val dt_txt: String
)

data class MainWeatherData(
    val temp: Double,
    val feels_like: Double,
    val temp_min: Double,
    val temp_max: Double,
    val pressure: Int,
    val sea_level: Int,
    val grnd_level: Int,
    val humidity: Int,
    val temp_kf: Double
)

data class WeatherDescription(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class Clouds(val all: Int)
data class Wind(val speed: Double, val deg: Int, val gust: Double?)
data class Rain(val `3h`: Double?)
data class Sys(val pod: String)

data class City(
    val id: Long,
    val name: String,
    val coord: Coord,
    val country: String,
    val population: Long,
    val timezone: Int,
    val sunrise: Long,
    val sunset: Long
)

data class Coord(val lat: Double, val lon: Double)
