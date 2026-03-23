const OWM_BASE_URL = "https://api.openweathermap.org/data/2.5/forecast";
const OWM_GEO_URL = "https://api.openweathermap.org/geo/1.0/direct";

/**
 * Fetches weather forecast from OpenWeatherMap and transforms it
 * to match the format expected by processWeatherData() in App.svelte.
 *
 * @param {string} cityName
 * @returns {Promise<Array>}
 */
/**
 * Fetches geo coordinates for a location name via OpenWeatherMap Geocoding API.
 *
 * @param {string} locationName
 * @param {number} [limit=1] - Max number of results
 * @returns {Promise<Array<{name: string, lat: number, lon: number, country: string, state?: string}>>}
 */
export async function fetchGeoCoordinates(locationName, limit = 1) {
    const apiKey = import.meta.env.VITE_OPENWEATHERMAP_API_KEY;
    if (!apiKey) {
        throw new Error("API-Key nicht konfiguriert. Bitte VITE_OPENWEATHERMAP_API_KEY in .env.local setzen.");
    }

    const url = `${OWM_GEO_URL}?q=${encodeURIComponent(locationName)}&limit=${limit}&appid=${apiKey}`;
    const response = await fetch(url);

    if (!response.ok) {
        if (response.status === 401) {
            throw new Error("Ungueltiger API-Key. Bitte VITE_OPENWEATHERMAP_API_KEY pruefen.");
        }
        throw new Error(`OpenWeatherMap Geocoding Fehler: ${response.status} ${response.statusText}`);
    }

    const data = await response.json();

    if (data.length === 0) {
        throw new Error(`Location "${locationName}" nicht gefunden.`);
    }

    return data.map(({ name, lat, lon, country, state }) => ({
        name,
        lat,
        lon,
        country,
        ...(state && { state }),
    }));
}

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
    const nowUtcSeconds = Math.floor(Date.now() / 1000);

    const byDay = {};
    for (const entry of owmData.list) {
        const cityLocalDateKey = new Date((entry.dt + timezoneOffsetSeconds) * 1000)
            .toISOString()
            .slice(0, 10);
        if (!byDay[cityLocalDateKey]) {
            byDay[cityLocalDateKey] = [];
        }
        byDay[cityLocalDateKey].push(entry);
    }

    return Object.keys(byDay).sort().map((dateKey, index) => {
        const entries = byDay[dateKey];

        const representative = index === 0
            ? findCurrentTimeslot(entries, nowUtcSeconds)
            : entries.find((e) => e.dt_txt.includes("12:00:00")) ?? entries[0];

        const minTemperature = Math.min(...entries.map((e) => e.main.temp_min));
        const maxTemperature = Math.max(...entries.map((e) => e.main.temp_max));
        const windSpeedKmh = Math.round(representative.wind.speed * 3.6 * 100) / 100;
        const cityLocalTimeMs = index === 0
            ? Date.now() + timezoneOffsetSeconds * 1000
            : (representative.dt + timezoneOffsetSeconds) * 1000;

        const dayTemperatures = entries.map((e) => ({
            temperature: e.main.temp,
            forecastDate: e.dt_txt,
        }));

        return {
            city: cityName,
            timezoneOffsetSeconds,
            cityLocalTimeMs,
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
}

/**
 * Finds the forecast entry closest to the current UTC time.
 *
 * @param {Array} entries
 * @param {number} nowUtcSeconds
 * @returns {Object}
 */
function findCurrentTimeslot(entries, nowUtcSeconds) {
    return entries.reduce((closest, entry) =>
        Math.abs(entry.dt - nowUtcSeconds) < Math.abs(closest.dt - nowUtcSeconds)
            ? entry
            : closest
    );
}
