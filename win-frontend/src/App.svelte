<script>
    import Slider from "./lib/Slider.svelte";
    import Temp from "./lib/Temp.svelte";
    import Luft from "./lib/Luft.svelte";
    import Hourly from "./lib/Hourly.svelte";
    import Map from "./lib/Map.svelte";
    import { fetchWeatherForecast } from "./lib/weatherService.js";
    import { onMount } from "svelte";

    let dailyForecasts = $state([]);
    let backgroundVideo = $state("");
    let city = $state(localStorage.getItem('lastCity') || "Heilbronn");
    let weatherData = $state([]);
    let loading = $state(false);
    let error = $state(null);
    let selectedDayIndex = $state(0);
    let recentSearches = $state([]);
    let isConnected = $state(true);
    let mapUrl = $state("");

    let recognition = $state(null);
    let isRecording = $state(false);
    let isListening = $state(false);
    let showVoiceHint = $state(true);

    const MORNING_START = 7;
    const MORNING_END = 9;
    const SUNSET_START = 16;
    const SUNSET_END = 17;
    const NIGHT_START = 17;
    const NIGHT_END = 7;

    const TRIGGER_WORD = "Wetter";

    function getWeatherConditionFromIcon(iconCode) {
        if (!iconCode) return "default";

        const iconToCondition = {
            "01d": "clear",
            "01n": "clear",
            "02d": "lightCloudy",
            "02n": "lightCloudy",
            "03d": "midcloudy",
            "03n": "midcloudy",
            "04d": "cloudy",
            "04n": "cloudy",
            "09d": "rain",
            "09n": "rain",
            "10d": "lightrain",
            "10n": "lightrain",
            "11d": "storm",
            "11n": "storm",
            "13d": "snow",
            "13n": "snow",
            "50d": "fog",
            "50n": "fog"
        };

        return iconToCondition[iconCode] || "default";
    }

    async function fetchWeatherData(cityName) {
        if (!cityName || !cityName.trim()) {
            error = "Bitte geben Sie einen Stadtnamen an.";
            loading = false;
            return;
        }

        loading = true;
        error = null;

        try {
            const newWeatherData = await fetchWeatherForecast(cityName);
            weatherData = newWeatherData;
            processWeatherData();

            const displayCity = (dailyForecasts.length > 0 && dailyForecasts[0].city)
                ? dailyForecasts[0].city
                : cityName;

            if (!recentSearches.includes(displayCity)) {
                recentSearches = [displayCity, ...recentSearches.slice(0, 4)];
                localStorage.setItem("recentSearches", JSON.stringify(recentSearches));
            }

            if (dailyForecasts.length > 0 && dailyForecasts[0].city) {
                city = dailyForecasts[0].city;
                localStorage.setItem('lastCity', city);
            }
        } catch (err) {
            console.error("Error fetching weather data:", err);
            error = `Error fetching weather data: ${err.message}`;
            weatherData = [];
            dailyForecasts = [];
        } finally {
            loading = false;
        }
    }

    function processWeatherData() {
        if (!weatherData || weatherData.length === 0) {
            dailyForecasts = [];
            mapUrl = "";
            return;
        }

        dailyForecasts = weatherData.map((item) => ({
            date: new Date(item.forecastDate),
            city: item.city,
            temperature: item.temperature,
            minTemperature: item.minTemperature,
            maxTemperature: item.maxTemperature,
            avgHumidity: item.humidity,
            description: item.description,
            pressure: item.pressure,
            windSpeed: item.windSpeed,
            iconCode: item.iconCode,
            weatherCondition: getWeatherConditionFromIcon(item.iconCode),
            dayTemperatures: item.dayTemperatures
                ? item.dayTemperatures.map((entry) => ({
                    temp: entry.temperature,
                    time: new Date(entry.forecastDate)
                }))
                : []
        }));

        const currentCity = dailyForecasts[0].city;
        if (currentCity) {
            mapUrl = `https://maps.google.com/maps?q=${encodeURIComponent(currentCity)}&t=&output=embed`;
        }

        selectedDayIndex = 0;
        const currentHour = new Date().getHours();
        const condition = dailyForecasts[0].weatherCondition;

        if (currentHour >= NIGHT_START || currentHour <= NIGHT_END) {
            backgroundVideo = "/videos/Night.mp4";
        } else if (currentHour >= MORNING_START && currentHour < MORNING_END) {
            backgroundVideo = "/videos/sunset.mp4";
        } else if (currentHour >= SUNSET_START && currentHour < SUNSET_END) {
            backgroundVideo = "/videos/sunset.mp4";
        } else {
            switch (condition) {
                case "rain":
                    backgroundVideo = "/videos/Regen.mp4";
                    break;
                case "clear":
                    backgroundVideo = "/videos/Clean.mp4";
                    break;
                case "lightCloudy":
                    backgroundVideo = "/videos/day.mp4";
                    break;
                case "cloudy":
                    backgroundVideo = "/videos/Clouds.mp4";
                    break;
                case "snow":
                    backgroundVideo = "/videos/Snowy.mp4";
                    break;
                case "midcloudy":
                    backgroundVideo = "/videos/midcloud.mp4";
                    break;
                case "lightrain":
                    backgroundVideo = "/videos/taube.mp4";
                    break;
                default:
                    backgroundVideo = "/videos/day.mp4";
            }
        }
    }

    function getBackgroundGradient(condition) {
        if (!condition) return "linear-gradient(to bottom, #4b6cb7, #182848)";

        switch (condition) {
            case "clear":
                return "linear-gradient(to bottom, #2980b9, #6dd5fa, #ffffff)";
            case "cloudy":
            case "lightCloudy":
                return "linear-gradient(to bottom, #757f9a, #d7dde8)";
            case "rain":
                return "linear-gradient(to bottom, #616161, #9bc5c3)";
            case "snow":
                return "linear-gradient(to bottom, #e6dada, #274046)";
            case "storm":
                return "linear-gradient(to bottom, #232526, #414345)";
            case "fog":
                return "linear-gradient(to bottom, #b79891, #94716b)";
            default:
                return "linear-gradient(to bottom, #4b6cb7, #182848)";
        }
    }

    function setupSpeechRecognition() {
        if (typeof window === 'undefined' || !('SpeechRecognition' in window || 'webkitSpeechRecognition' in window)) {
            console.warn("Speech Recognition API nicht im Browser unterstuetzt.");
            error = "Spracherkennung wird von Ihrem Browser nicht unterstuetzt.";
            isListening = false;
            return;
        }

        try {
            // @ts-ignore
            const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
            recognition = new SpeechRecognition();
            recognition.lang = 'de-DE';
            recognition.continuous = false;
            recognition.interimResults = false;

            recognition.onstart = () => isRecording = true;

            recognition.onend = () => {
                const wasRecording = isRecording;
                isRecording = false;

                if (isListening && wasRecording && recognition) {
                    setTimeout(() => {
                        try {
                            if (isListening && recognition && !isRecording) {
                                recognition.start();
                            }
                        } catch (e) {
                            if (e.name !== 'InvalidStateError') {
                                isListening = false;
                            }
                        }
                    }, 300);
                }
            };

            recognition.onresult = (event) => {
                let transcript = "";
                for (let i = event.resultIndex; i < event.results.length; ++i) {
                    if (event.results[i].isFinal) {
                        transcript += event.results[i][0].transcript;
                    }
                }
                transcript = transcript.trim();

                const lowerTranscript = transcript.toLowerCase();
                if (lowerTranscript.startsWith(TRIGGER_WORD.toLowerCase())) {
                    const spokenCity = lowerTranscript.slice(TRIGGER_WORD.length).trim();
                    if (spokenCity) {
                        city = spokenCity;
                        fetchWeatherData(spokenCity);
                    }
                }
            };

            recognition.onerror = (event) => {
                console.error("Speech recognition error:", event.error);
                isRecording = false;

                if (event.error !== 'no-speech' && event.error !== 'aborted') {
                    error = `Spracherkennungsfehler: ${event.error}. ${event.message || ''}`;
                }

                if (['not-allowed', 'service-not-allowed', 'audio-capture', 'network'].includes(event.error)) {
                    isListening = false;
                    if (recognition) recognition.abort();
                } else if (isListening && recognition) {
                    setTimeout(() => {
                        if (isListening && recognition) recognition.start();
                    }, 1000);
                }
            };

            if (isListening) {
                recognition.start();
            }
        } catch (e) {
            console.error("Error setting up speech recognition:", e);
            error = "Fehler bei der Initialisierung der Spracherkennung.";
            isListening = false;
        }
    }

    function checkConnection() {
        isConnected = navigator.onLine;
        window.addEventListener('online', () => isConnected = true);
        window.addEventListener('offline', () => {
            isConnected = false;
            isListening = false;
            if (recognition && isRecording) recognition.stop();
        });
    }

    const savedSearches = localStorage.getItem("recentSearches");
    if (savedSearches) {
        recentSearches = JSON.parse(savedSearches);
    }

    onMount(() => {
        checkConnection();
        isListening = true;
        setupSpeechRecognition();

        if (city) {
            fetchWeatherData(city);
        }

        return () => {
            isListening = false;

            if (recognition) {
                recognition.onstart = null;
                recognition.onresult = null;
                recognition.onend = null;
                recognition.onerror = null;
                recognition.abort();
                recognition = null;
            }

            window.removeEventListener('online', () => isConnected = true);
            window.removeEventListener('offline', () => isConnected = false);
        };
    });
</script>

<main>
    {#if dailyForecasts.length > 0 && dailyForecasts[selectedDayIndex]}
        {@const currentDay = dailyForecasts[selectedDayIndex]}

        {#if backgroundVideo}
            {#key backgroundVideo}
                <video class="background-video" autoplay muted loop playsinline>
                    <source src={backgroundVideo} type="video/mp4" />
                </video>
            {/key}
        {/if}

        {#if !isConnected}
            <div class="status-overlay error">
                Keine Internetverbindung. Wetterdaten und Sprachsteuerung koennten beeintraechtigt sein.
            </div>
        {/if}

        <div class="weather-app" style="background: {getBackgroundGradient(currentDay.weatherCondition)}">
            <div class="app-content-wrapper">
                <div class="app-header">
                    <h1>Wetter</h1>
                    {#if showVoiceHint}
                        <div class="voice-hint">
                            <div class="voice-hint-content">
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24"
                                     fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                                     stroke-linejoin="round">
                                    <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
                                    <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
                                    <line x1="12" y1="19" x2="12" y2="23"></line>
                                    <line x1="8" y1="23" x2="16" y2="23"></line>
                                </svg>
                                <span>Sage "{TRIGGER_WORD} [Stadtname]" (z.B. "{TRIGGER_WORD} Berlin")</span>
                            </div>
                            <button class="voice-hint-close" onclick={() => showVoiceHint = false}>x</button>
                        </div>
                    {/if}
                </div>

                {#if error}
                    <div class="error-message">{error}</div>
                {/if}

                {#if loading}
                    <div class="loading-container">
                        <div class="loading-spinner"></div>
                        <div class="loading-text">Wetterdaten werden geladen...</div>
                    </div>
                {:else if dailyForecasts.length > 0 && dailyForecasts[selectedDayIndex]}
                    <Temp day={currentDay} />

                    <Luft
                        humidity={currentDay.avgHumidity}
                        pressure={currentDay.pressure}
                        windSpeed={currentDay.windSpeed}
                    />

                    <Hourly dayTemperatures={currentDay.dayTemperatures} />

                    <div class="daily">Tägliche Vorhersage</div>

                    <div class="down">
                        <Slider items={dailyForecasts.slice(1)} />
                        <Map mapUrl={mapUrl} cityName={city} />
                    </div>
                {:else if !loading && !error}
                    <div class="no-data">
                        <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 24 24" fill="none"
                             stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z"></path>
                        </svg>
                        <p>Keine Wetterdaten verfuegbar. Bitte suchen Sie nach einer Stadt oder verwenden Sie die
                            Sprachsteuerung (sage "{TRIGGER_WORD} [Stadt]").</p>
                    </div>
                {/if}
            </div>
        </div>
    {/if}

    {#if loading && dailyForecasts.length === 0}
        <div class="initial-loading">
            <div class="loading-spinner"></div>
            <div class="loading-text">Wetterdaten werden geladen...</div>
        </div>
    {:else if error && dailyForecasts.length === 0}
        <div class="initial-error">{error}</div>
    {/if}
</main>

<style>
    .initial-loading, .initial-error {
        position: fixed;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        color: white;
        text-align: center;
        z-index: 10;
    }

    .initial-error {
        background: rgba(200, 0, 0, 0.7);
        padding: 1rem 2rem;
        border-radius: 8px;
    }

    .background-video {
        position: fixed;
        top: 0;
        left: 0;
        min-width: 100%;
        min-height: 100%;
        object-fit: cover;
        z-index: 1;
        pointer-events: none;
    }

    .map-container iframe {
        display: block;
    }

    :global(body) {
        margin: 0;
        padding: 0;
        transition: background 0.5s ease;
        color: white;
        display: flex;
        flex-direction: column;
        font-family: "Calibri", sans-serif;
        background-color: #2c3e50;
        max-width: 100%;
    }

    .down {
        display: flex;
        flex-direction: row;
        height: 800px;
    }

    :global(*) {
        box-sizing: border-box;
    }

    main {
        min-height: 100vh;
        display: flex;
        flex-direction: column;
    }

    .status-overlay {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        padding: 10px 20px;
        z-index: 2000;
        color: white;
        font-size: 0.9rem;
        box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
    }

    .status-overlay.error {
        background-color: #e74c3c;
    }

    .weather-app {
        min-height: 100vh;
        transition: background 0.5s ease;
        color: black;
        display: flex;
        flex-direction: column;
        flex-grow: 1;
    }

    .app-content-wrapper {
        width: 100%;
        max-width: 1200px;
        z-index: 2;
        position: relative;
        display: flex;
        flex-direction: column;
        overflow: hidden;
        padding-top: 5rem;
    }

    .app-header {
        display: flex;
        justify-content: center;
        align-items: center;
        flex-wrap: wrap;
        gap: 20px;
        flex-direction: column;
        width: 100%;
        margin-bottom: 60px;
    }

    h1 {
        margin: 0;
        font-size: 3.5rem;
        font-weight: 700;
        color: white;
        text-align: center;
        background-color: transparent;
        padding: 4px 16px;
    }

    .voice-hint {
        font-size: 1.1rem;
        background: rgba(255, 255, 255, 0.06);
        border-radius: 60px;
        box-shadow: 0 4px 30px rgba(0, 0, 0, 0.1);
        backdrop-filter: blur(5px);
        -webkit-backdrop-filter: blur(5px);
        border: 1px solid rgba(255, 255, 255, 0.3);
        color: white;
        padding: 12px 18px;
        margin-top: 10px;
        margin-bottom: 15px;
        display: inline-flex;
        align-items: center;
        justify-content: space-between;
        animation: fadeIn 0.5s ease-out;
    }

    .voice-hint-content {
        display: flex;
        align-items: center;
        text-align: center;
        gap: 10px;
    }

    .voice-hint-content svg {
        flex-shrink: 0;
        width: 20px;
        height: 20px;
    }

    .voice-hint-close {
        background: none;
        border: none;
        color: white;
        font-size: 1.5rem;
        cursor: pointer;
        padding: 0 0 0 10px;
        line-height: 1;
        opacity: 0.7;
        transition: opacity 0.2s;
    }

    .voice-hint-close:hover {
        opacity: 1;
    }

    .loading-container {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        margin: 50px 0;
        gap: 15px;
    }

    .loading-spinner {
        width: 40px;
        height: 40px;
        border: 4px solid rgba(255, 255, 255, 0.3);
        border-radius: 50%;
        border-top-color: white;
        animation: spin 1s ease-in-out infinite;
    }

    .loading-text {
        font-size: 1.9rem;
        color: white;
        opacity: 0.9;
    }

    .no-data {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        margin: 50px auto;
        color: white;
        text-align: center;
        gap: 15px;
        padding: 20px;
    }

    .daily {
        font-size: 2rem;
        font-weight: 600;
        margin-left: 1em;
        margin-top: 20px;
        padding: 4px 16px;
        border-radius: 8px;
        color: white;
    }

    .no-data svg {
        opacity: 0.7;
        width: 50px;
        height: 50px;
    }

    .no-data p {
        font-size: 1.9rem;
        max-width: 400px;
        opacity: 0.9;
    }

    .error-message {
        color: #fff;
        text-align: center;
        margin: 20px auto;
        padding: 12px 18px;
        background-color: rgba(231, 76, 60, 0.85);
        border-radius: 8px;
        max-width: 550px;
        backdrop-filter: blur(5px);
        box-shadow: 0 3px 8px rgba(0, 0, 0, 0.15);
    }

    @keyframes spin {
        to {
            transform: rotate(360deg);
        }
    }

    @keyframes fadeIn {
        from {
            opacity: 0;
            transform: translateY(-10px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }

    @media (max-width: 768px) {
        .app-header {
            align-items: center;
        }

        h1 {
            font-size: 2.8rem;
            padding-top: 20em;
        }
    }

    @media (max-width: 480px) {
        .weather-app {
            padding: 15px;
        }

        h1 {
            font-size: 2rem;
        }
    }
</style>
