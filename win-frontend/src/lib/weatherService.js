const OWM_BASE_URL = "https://api.openweathermap.org/data/2.5/forecast";

/**
 * Fetches weather forecast from OpenWeatherMap and transforms it
 * to match the format expected by processWeatherData() in App.svelte.
 *
 * @param {string} cityName
 * @returns {Promise<Array>}
 */
export async function fetchWeatherForecast(cityName) {
    const apiKey = import.meta.env.VITE_OPENWEATHERMAP_API_KEY;
    if (!apiKey) {
        throw new Error("API-Key nicht konfiguriert. Bitte VITE_OPENWEATHERMAP_API_KEY in .env.local setzen.");
    }

    const url = `${OWM_BASE_URL}?q=${encodeURIComponent(cityName)}&lang=de&units=metric&cnt=40&appid=${apiKey}`;
    const response = await fetch(url);

    if (!response.ok) {
        if (response.status === 404) {
            throw new Error(`Stadt "${cityName}" nicht gefunden.`);
        }
        if (response.status === 401) {
            throw new Error("Ungueltiger API-Key. Bitte VITE_OPENWEATHERMAP_API_KEY pruefen.");
        }
        throw new Error(`OpenWeatherMap Fehler: ${response.status} ${response.statusText}`);
    }

    const data = await response.json();
    return transformForecast(data);
}

/**
 * Transforms the raw OWM /forecast response into the format
 * that processWeatherData() in App.svelte expects.
 *
 * @param {Object} owmData - Raw OWM API response
 * @returns {Array}
 */
function transformForecast(owmData) {
    const cityName = owmData.city.name;
    const timezoneOffsetSeconds = owmData.city.timezone;

    const byDay = {};
    for (const entry of owmData.list) {
        const dateKey = entry.dt_txt.slice(0, 10);
        if (!byDay[dateKey]) {
            byDay[dateKey] = [];
        }
        byDay[dateKey].push(entry);
    }

    const forecasts = Object.keys(byDay).sort().map((dateKey) => {
        const entries = byDay[dateKey];

        const representative = entries.find((e) => e.dt_txt.includes("12:00:00")) || entries[0];

        const minTemperature = Math.min(...entries.map((e) => e.main.temp_min));
        const maxTemperature = Math.max(...entries.map((e) => e.main.temp_max));
        const windSpeedKmh = Math.round(representative.wind.speed * 3.6 * 100) / 100;

        const dayTemperatures = entries.map((e) => ({
            temperature: e.main.temp,
            forecastDate: e.dt_txt,
        }));

        return {
            city: cityName,
            timezoneOffsetSeconds,
            forecastDate: representative.dt_txt,
            temperature: representative.main.temp,
            minTemperature,
            maxTemperature,
            humidity: representative.main.humidity,
            pressure: representative.main.pressure,
            windSpeed: windSpeedKmh,
            description: representative.weather[0].description,
            iconCode: representative.weather[0].icon,
            dayTemperatures,
        };
    });

    const firstEntryIcon = owmData.list[0].weather[0].icon;
    forecasts[0].iconCode = firstEntryIcon;
    return forecasts;
}
