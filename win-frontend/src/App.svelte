<script>
    import WeatherLottieIcon from "./lib/WeatherLottieIcon.svelte";
    import Slider from "./lib/Slider.svelte";

    // Props
    let dailyForecasts = $state([]);

    // State variables - Svelte 5 style
    let backgroundVideo = $state("");
    let city = $state("Heilbronn");
    let weatherData = $state([]);
    let loading = $state(false);
    let error = $state(null);
    let selectedDayIndex = $state(0);
    let recentSearches = $state([]);
    let isConnected = $state(true);
    let isSocketConnected = $state(false);

    // Voice recognition variables
    let recognition = $state(null);
    let lang = $state('de-DE');
    let isRecording = $state(false);
    let isListening = $state(false);
    let showVoiceHint = $state(true);

    // WebSocket
    let socket = $state(null);

    // Carousel variables
    let currentIndex = $state(1); // Start bei Tag 1 (nicht heute)
    let carouselInterval = $state(null);

    // Derived values
    let shouldStartCarousel = $derived(dailyForecasts.length > 1);

    const TRIGGER_WORD = "Wetter";

    function getWeatherConditionFromIcon(iconCode) {
        if (!iconCode) return "default";

        const map = {
            "01d": "clear",
            "01n": "clear",
            "02d": "cloudy",
            "02n": "cloudy",
            "03d": "cloudy",
            "03n": "cloudy",
            "04d": "cloudy",
            "04n": "cloudy",
            "09d": "rain",
            "09n": "rain",
            "10d": "rain",
            "10n": "rain",
            "11d": "storm",
            "11n": "storm",
            "13d": "snow",
            "13n": "snow",
            "50d": "fog",
            "50n": "fog"
        };

        return map[iconCode] || "default";
    }

    function startCarousel() {
        if (carouselInterval) clearInterval(carouselInterval);

        if (dailyForecasts.length > 1) {
            currentIndex = 1;
            carouselInterval = setInterval(() => {
                currentIndex = (currentIndex + 1) % dailyForecasts.length;
            }, 4000);
        }
    }

    // Function to fetch weather data
    async function fetchWeatherData(cityName) {
        if (!cityName || !cityName.trim()) {
            console.warn("fetchAndDisplayWeatherData: Kein Stadtname angegeben.");
            error = "Bitte geben Sie einen Stadtnamen an.";
            loading = false;
            return;
        }

        loading = true;
        error = null;

        try {
            console.log(`Fetching weather for: ${cityName}`);

            const response = await fetch(`http://localhost:8080/api/weather/${cityName}`);

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Wetterdaten für ${cityName} nicht gefunden (${response.status}): ${errorText || response.statusText}`);
            }

            const newWeatherData = await response.json();
            console.log(JSON.stringify(newWeatherData, null, 2));
            weatherData = newWeatherData;
            processWeatherData();

            const displayCity = (dailyForecasts.length > 0 && dailyForecasts[0].city) ? dailyForecasts[0].city : cityName;
            if (!recentSearches.includes(displayCity)) {
                recentSearches = [displayCity, ...recentSearches.slice(0, 4)];
                localStorage.setItem("recentSearches", JSON.stringify(recentSearches));
            }
            if (dailyForecasts.length > 0 && dailyForecasts[0].city) {
                city = dailyForecasts[0].city;
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

    // Process weather data to group by day
    function processWeatherData() {
        if (!weatherData || weatherData.length === 0) {
            dailyForecasts = [];
            return;
        }

        dailyForecasts = weatherData.map((item) => {
            return {
                date: new Date(item.forecastDate),
                city: item.city,
                temperature: item.temperature,
                minTemperature: item.minTemperature,
                maxTemperature: item.maxTemperature,
                avgHumidity: item.humidity,
                description: item.description,
                iconCode: item.iconCode,
                weatherCondition: getWeatherConditionFromIcon(item.iconCode),
            };
        })

        selectedDayIndex = 0;

        // Video basierend auf Wetter
        if (dailyForecasts.length > 0) {
            const condition = dailyForecasts[0].weatherCondition;

            switch (condition) {
                case "rain":
                    backgroundVideo = `/videos/rain.mp4`;
                    break;
                case "clear":
                    backgroundVideo = `/videos/clear.mp4`;
                    break;
                case "cloudy":
                    backgroundVideo = `/videos/cloudy.mp4`;
                    break;
                case "snow":
                    backgroundVideo = `/videos/snow.mp4`;
                    break;
                default:
                    backgroundVideo = "";
            }
        }

        // Carousel starten (nur nach vollständiger Verarbeitung!)
        startCarousel();
    }

    // Format date to display full date
    function formatDate(date) {
        return new Date(date).toLocaleDateString("de-DE", {
            weekday: "long",
            year: "numeric",
            month: "long",
            day: "numeric",
        });
    }

    // Get background gradient based on time and weather
    function getBackgroundGradient(forecast) {
        if (!forecast || !forecast.forecasts || forecast.forecasts.length === 0 || !forecast.forecasts[0].forecastDate) {
            return "linear-gradient(to bottom, #4b6cb7, #182848)";
        }

        const condition = forecast.weatherCondition;
        const hour = new Date(forecast.forecasts[0].forecastDate).getHours();
        const isDay = hour >= 6 && hour < 20;

        if (condition === "clear") {
            return isDay
                ? "linear-gradient(to bottom, #2980b9, #6dd5fa, #ffffff)"
                : "linear-gradient(to bottom, #0f2027, #203a43, #2c5364)";
        } else if (condition === "cloudy") {
            return isDay
                ? "linear-gradient(to bottom, #757f9a, #d7dde8)"
                : "linear-gradient(to bottom, #373b44, #4286f4)";
        } else if (condition === "rain") {
            return "linear-gradient(to bottom, #616161, #9bc5c3)";
        } else if (condition === "snow") {
            return "linear-gradient(to bottom, #e6dada, #274046)";
        } else if (condition === "storm") {
            return "linear-gradient(to bottom, #232526, #414345)";
        } else if (condition === "fog") {
            return "linear-gradient(to bottom, #b79891, #94716b)";
        }

        return "linear-gradient(to bottom, #4b6cb7, #182848)";
    }

    function setupSpeechRecognition() {
        if (typeof window === 'undefined' || !('SpeechRecognition' in window || 'webkitSpeechRecognition' in window)) {
            if ('SpeechRecognition' in window) {
                console.log("SpeechRecognition")
            } else {
                console.log("webkitSpeechRecognition")
            }
            console.warn("Speech Recognition API nicht im Browser unterstützt.");
            error = "Spracherkennung wird von Ihrem Browser nicht unterstützt.";
            isListening = false;
            return;
        }

        try {
            // @ts-ignore
            const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
            recognition = new SpeechRecognition();
            recognition.lang = lang === 'de-DE' ? 'de-DE' : 'en-US';
            recognition.continuous = false;
            recognition.interimResults = false;

            recognition.onstart = () => isRecording = true;

            recognition.onend = () => {
                console.log("Speech recognition ended.");
                const wasRecording = isRecording;
                isRecording = false;
                if (isListening && wasRecording && isSocketConnected && recognition) {
                    setTimeout(() => {
                        try {
                            if (isListening && recognition && !isRecording) {
                                console.log("Restarting recognition from onend...");
                                recognition.start();
                            }
                        } catch (e) {
                            console.warn("Konnte Spracherkennung nicht neu starten nach onend:", e.name, e.message);
                            if (e.name === 'InvalidStateError') {
                                // Vielleicht war es schon wieder gestartet, ignoriere oder logge nur
                            } else {
                                // Andere Fehler behandeln
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
                console.log("Frontend Heard (final):", transcript);

                if (transcript && socket && socket.readyState === WebSocket.OPEN) {
                    socket.send(JSON.stringify({
                        type: "transcription_frontend",
                        text: transcript
                    }));
                    console.log("Sent to Python backend:", transcript);
                } else if (!transcript) {
                    console.log("Empty transcript, not sending.");
                }
            };

            recognition.onerror = (event) => {
                console.error("Speech recognition error:", event.error, "Message:", event.message);
                isRecording = false;
                if (event.error !== 'no-speech' && event.error !== 'aborted') {
                    error = `Spracherkennungsfehler: ${event.error}. ${event.message || ''}`;
                }

                if (['not-allowed', 'service-not-allowed', 'audio-capture', 'network'].includes(event.error)) {
                    isListening = false;
                    if (recognition) recognition.abort();
                } else if (isListening && isSocketConnected && recognition) {
                    setTimeout(() => {
                        if (isListening && recognition) recognition.start();
                    }, 1000);
                }
            };

            if (isListening) {
                console.log("Attempting to start initial speech recognition...");
                recognition.start();
            }
        } catch (e) {
            console.error("Error setting up speech recognition:", e);
            error = "Fehler bei der Initialisierung der Spracherkennung.";
            isListening = false;
        }
    }

    function initWebSocket() {
        console.log(JSON.stringify({
            event: "WebSocketStart",
            timestamp: new Date().toISOString(),
            message: "🔌 Starte WebSocket-Verbindung zur Sprach-KI …"
        }));

        if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
            console.log("WebSocket ist bereits offen oder verbindet sich.");
            return;
        }

        if (socket) {
            socket.onopen = null;
            socket.onmessage = null;
            socket.onclose = null;
            socket.onerror = null;
            if (socket.readyState !== WebSocket.CLOSED) socket.close();
        }

        console.log("Initializing WebSocket to Python backend...");
        socket = new WebSocket('ws://localhost:8765/ws');

        socket.onopen = () => {
            console.log('WebSocket to Python backend established.');
            isSocketConnected = true;
            error = null;
            isListening = true;
            setupSpeechRecognition();
        };

        socket.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                console.log("Message from Python backend:", data);

                if (data.type === 'command_understood_display_weather' && data.weatherPayload) {
                    weatherData = data.weatherPayload;
                    processWeatherData();
                    city = (dailyForecasts.length > 0 && dailyForecasts[0].city) ? dailyForecasts[0].city : (data.city || "Unbekannt");

                    if (recognition && isListening) {
                        let originalIsListeningState = isListening;
                        isListening = false;
                        recognition.stop();
                        setTimeout(() => {
                            isListening = originalIsListeningState;
                            if (isListening && isSocketConnected && recognition && !isRecording) {
                                recognition.start();
                            }
                        }, 2000);
                    }

                } else if (data.type === 'no_action_needed') {
                    console.log("Python backend:", data.reason || "No action needed.");
                } else if (data.type === 'error_from_python') {
                    console.error("Error from Python backend:", data.message);
                    error = `Fehler vom Sprachserver: ${data.message}`;
                }
            } catch (e) {
                console.error('Error parsing WebSocket message from Python backend:', e);
            }
        };

        socket.onclose = (event) => {
            console.log('WebSocket to Python backend closed.', event.code, event.reason);
            isSocketConnected = false;
            isListening = false;
            if (recognition) {
                recognition.onend = null;
                recognition.stop();
            }
        };

        socket.onerror = (errorEvent) => {
            console.error("WebSocket to Python backend error:", errorEvent);
            isSocketConnected = false;
            isListening = false;
            if (recognition) {
                recognition.onend = null;
                recognition.stop();
            }
            error = "WebSocket-Verbindungsfehler zum Sprachserver.";
        };
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

    // Svelte 5 Effects
    $effect(() => {
        const firstLoad = localStorage.getItem('firstLoadDone');
        if (!firstLoad) {
            localStorage.setItem('firstLoadDone', 'true');

            if (recognition) recognition.abort();
            if (socket) socket.close();

            setTimeout(() => {
                location.reload();
            }, 3000);
        }

        const savedSearches = localStorage.getItem("recentSearches");
        if (savedSearches) {
            recentSearches = JSON.parse(savedSearches);
        }

        checkConnection();
        initWebSocket();

        if (city) {
            fetchWeatherData(city).then(() => {
                startCarousel();
            });
        }

        return () => {
            clearInterval(carouselInterval);
            console.log("App unmounting. Cleaning up resources.");
            isListening = false;

            if (recognition) {
                recognition.onstart = null;
                recognition.onresult = null;
                recognition.onend = null;
                recognition.onerror = null;
                recognition.abort();
                recognition = null;
            }

            if (socket) {
                socket.onopen = null;
                socket.onmessage = null;
                socket.onclose = null;
                socket.onerror = null;
                if (socket.readyState === WebSocket.OPEN) socket.close(1000, "Component unmounting");
                socket = null;
            }

            window.removeEventListener('online', () => isConnected = true);
            window.removeEventListener('offline', () => isConnected = false);
        };
    });

    $effect(() => {
        if (shouldStartCarousel) {
            startCarousel();
        }

        // Cleanup für Carousel
        return () => {
            if (carouselInterval) clearInterval(carouselInterval);
        };
    });
</script>

<main>
    {#if backgroundVideo}
        {#key backgroundVideo}
            <video class="background-video" autoplay muted loop playsinline>
                <source src={backgroundVideo} type="video/mp4"/>
            </video>
        {/key}
    {/if}

    {#if !isSocketConnected && error && (error.includes("WebSocket") || error.includes("Sprachserver getrennt"))}
        <div class="status-overlay error">
            🔴 WebSocket nicht verbunden. Sprachsteuerung ist
            offline. {error.includes("versuche") ? error : "Versuche neu zu verbinden..."} <br/> Fehler: {error}
        </div>
    {:else if !isConnected}
        <div class="status-overlay error">
            🔴 Keine Internetverbindung. Wetterdaten und Sprachsteuerung könnten beeinträchtigt sein.
        </div>
    {/if}

    <div class="weather-app" style="background: {getBackgroundGradient(dailyForecasts[selectedDayIndex] || null)}">
        <div class="app-content-wrapper">
            <div class="app-header">
                <h1>Wetter Vorhersage</h1>
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
                        <button class="voice-hint-close" onclick={() => showVoiceHint = false}>×</button>
                    </div>
                {/if}
            </div>

            {#if error && !error.includes("WebSocket") && !error.includes("Sprachserver")}
                <div class="error-message">{error}</div>
            {/if}

            <!-- Hauptlogik für die Wetteranzeige -->
            {#if loading}
                <div class="loading-container">
                    <div class="loading-spinner"></div>
                    <div class="loading-text">Wetterdaten werden geladen...</div>
                </div>
            {:else if dailyForecasts.length > 0 && dailyForecasts[selectedDayIndex]}
                {@const currentDay = dailyForecasts[selectedDayIndex]}

                <!-- ANZEIGE VON WETTERDATEN (HEUTIGER TAG) -->
                <div class="today-weather-card">
                    <div class="date">{formatDate(currentDay.date)}</div>
                    <div class="city-name-display"
                         style="font-size: 2.2rem; margin-bottom: 10px;">{currentDay.city}</div>
                    <div class="weather-info">
                        <div class="temperature-container">
                            <div class="current-temp" style="font-size: 3.5rem; margin-bottom: 5px;">
                                {(currentDay.temperature !== undefined ? Math.round(currentDay.temperature) : '--')}°C
                            </div>
                            <div class="min-max">
                                <span class="min">Min: {Math.round(currentDay.minTemperature)}°C</span> /
                                <span class="max">Max: {Math.round(currentDay.maxTemperature)}°C</span>
                            </div>
                        </div>
                        <div class="weather-icon">
                            {#key currentDay.weatherCondition}
                                <WeatherLottieIcon condition={currentDay.weatherCondition}/>
                            {/key}
                            <div class="description">{currentDay.description}</div>
                        </div>
                    </div>

                    <div class="details">
                        <div class="detail-item">
                            <span class="label">Luftfeuchtigkeit:</span>
                            <span class="value">{currentDay.avgHumidity}%</span>
                        </div>
                    </div>
                </div>

                <!-- ANZEIGE DER KOMMENDEN TAGE ALS 3D-FLIP-CAROUSEL -->
                {#if dailyForecasts.length > 1}
                    <div class="next-days-header">Kommende Tage</div>
                    <Slider items={dailyForecasts.slice(1, dailyForecasts.length)}/>
                {/if}

            {:else if !loading && !error}
                <div class="no-data">
                    <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 24 24" fill="none"
                         stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z"></path>
                    </svg>
                    <p>Keine Wetterdaten verfügbar. Bitte suchen Sie nach einer Stadt oder verwenden Sie die
                        Sprachsteuerung (sage "{TRIGGER_WORD} [Stadt]").</p>
                </div>
            {/if}

        </div> <!-- Schließt app-content-wrapper -->
    </div> <!-- Schließt weather-app -->
</main>

<style>
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

    /* Globale Stile & Resets aus "Faruk App.svelte" */
    :global(body) {
        margin: 0;
        padding: 0;
        transition: background 0.5s ease;
        color: white;
        display: flex;
        flex-direction: column; /* Besser als flex-wrap für Hauptbody */
        font-family: "Calibri", sans-serif;
        background-color: #2c3e50; /* Standard Dunkelblau/Grau */
    }

    :global(*) {
        box-sizing: border-box;
    }

    main {
        min-height: 100vh;
        display: flex;
        flex-direction: column;
    }

    /* Status-Overlays (aus "Aktuelles App.svelte", da nützlich) */
    .status-overlay {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        padding: 10px 20px;
        text-align: center;
        z-index: 2000;
        color: white;
        font-size: 0.9rem;
        box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
    }

    .status-overlay.error {
        background-color: #e74c3c;
    }

    .weather-app { /* Hauptcontainer aus "Faruk App.svelte" */
        min-height: 100vh;
        padding: 20px;
        transition: background 0.5s ease;
        color: black;
        display: flex;
        flex-direction: column;
        align-items: center; /* Zentriert .app-content-wrapper */
        flex-grow: 1;
    }

    .app-content-wrapper { /* Für max-width, aus "Aktuelles App.svelte" */
        width: 100%;
        max-width: 1200px; /* Oder eine andere passende Breite */
        z-index: 2;
        position: relative;
        display: flex;
        flex-direction: column;
        align-items: center;
    }

    .app-header {
        display: flex;
        justify-content: center;
        align-items: center;
        flex-wrap: wrap;
        gap: 20px;
        flex-direction: column;
        width: 100%;
        margin-top: 40px; /* NEU */
        margin-bottom: 40px; /* statt 30px */
    }

    h1 { /* Aus "Faruk App.svelte" */
        margin: 0;
        font-size: 3.5rem;
        font-weight: 700;
        text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.2);
        text-align: center;
        display: inline-block;
        padding: 4px 16px;
        border-radius: 8px;
        background-color: rgba(255, 255, 255, 0.2);
        backdrop-filter: blur(4px);
    }

    .voice-hint { /* Aus "Faruk App.svelte" mit Anpassungen */
        font-size: 1.1rem;
        background-color: rgba(255, 255, 255, 0.1);
        border-radius: 12px;
        padding: 12px 18px;
        margin-top: 10px;
        margin-bottom: 15px;
        display: inline-flex;
        align-items: center;
        justify-content: space-between;
        backdrop-filter: blur(5px);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
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

    .search-container { /* Aus "Faruk App.svelte" */
        position: relative;
        width: 100%;
        max-width: 450px;
        margin: 0 auto; /* Zentriert, wenn .app-header text-align:center hat */
    }

    .search-form { /* Aus "Aktuelles App.svelte", da es einen submit button hat */
        display: flex;
        gap: 10px;
        align-items: center;
    }

    .input-wrapper { /* Aus "Faruk App.svelte" */
        position: relative;
        flex-grow: 1; /* Nimmt verfügbaren Platz */
    }

    .input-wrapper.listening .city-input { /* Aus "Faruk App.svelte" für den pulsierenden Effekt */
        /* animation: pulse 1.5s infinite; // Kann bleiben, oder durch .recording-status ersetzt werden */
        border-color: rgba(255, 100, 100, 0.7);
    }

    .city-input { /* Aus "Faruk App.svelte" */
        font-size: 1.8rem; /* Etwas größer für Faruk-Stil */
        padding: 10px 15px;
        border: 1px solid rgba(255, 255, 255, 0.2);
        border-radius: 25px;
        width: 100%; /* Passt sich an .input-wrapper an */
        background-color: rgba(255, 255, 255, 0.1);
        color: white;
        backdrop-filter: blur(8px);
        transition: all 0.3s ease;
        text-align: center;
        height: 48px;
    }

    .city-input::placeholder {
        color: rgba(255, 255, 255, 0.7);
    }

    .city-input:focus {
        outline: none;
        background-color: rgba(255, 255, 255, 0.15);
        border-color: rgba(255, 255, 255, 0.5);
        box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.2);
    }

    .search-button { /* Aus "Aktuelles App.svelte" */
        padding: 10px;
        background-color: rgba(255, 255, 255, 0.2);
        border: 1px solid rgba(255, 255, 255, 0.3);
        color: white;
        border-radius: 50%;
        cursor: pointer;
        transition: background-color 0.3s ease, transform 0.2s ease;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 48px;
        height: 48px;
        flex-shrink: 0;
    }

    .search-button:hover {
        background-color: rgba(255, 255, 255, 0.3);
        transform: scale(1.05);
    }

    .search-button svg {
        width: 20px;
        height: 20px;
    }

    .recent-searches { /* Aus "Aktuelles App.svelte" */
        position: absolute;
        top: calc(100% + 5px);
        left: 0;
        right: 0;
        background-color: #fff;
        color: #333;
        border-radius: 8px;
        box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15);
        z-index: 100;
        max-height: 200px;
        overflow-y: auto;
        border: 1px solid #ddd;
    }

    .recent-search-item {
        padding: 10px 15px;
        cursor: pointer;
        transition: background-color 0.2s;
        border-bottom: 1px solid #eee;
        font-size: 0.95rem;
    }

    .recent-search-item:last-child {
        border-bottom: none;
    }

    .recent-search-item:hover, .recent-search-item:focus {
        background-color: #f0f0f0;
        outline: none;
    }

    .today-weather-card {
        width: 100%;
        max-width: 690px; /* +15% von 600px */
        font-size: 1.38rem; /* +15% von 1.2rem */
        text-align: center;
        background-color: rgba(255, 255, 255, 0.7);
        border-radius: 16px;
        padding: 23px 28px; /* +15% */
        margin: 0 auto 30px auto;
        backdrop-filter: blur(10px);
        transition: transform 0.2s;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
        border: 1px solid rgba(255, 255, 255, 0.1);
        margin-bottom: 60px;
    }

    .today-weather-card:hover {
        transform: translateY(-5px);
    }

    .today-weather-card .date {
        font-size: 2.19rem; /* +15% von 1.9rem */
        margin-bottom: 9px;
        opacity: 0.8;
    }

    .today-weather-card .city-name-display {
        font-size: 2.53rem; /* +15% von 2.2rem */
        font-weight: 600;
        margin-bottom: 17px;
    }

    .weather-icon {
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        text-align: center;
    }

    .today-weather-card .weather-info {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 28px; /* +15% von 25px */
        margin-bottom: 17px;
        flex-wrap: wrap;
    }

    .today-weather-card .weather-icon img {
        width: 115px; /* +15% von 100px */
        height: 115px;
    }

    .today-weather-card .temperature-container {
        text-align: left;
    }

    .today-weather-card .current-temp {
        font-size: 4.03rem; /* +15% von 3.5rem */
        font-weight: 300;
        line-height: 1;
        margin: 0;
    }

    .today-weather-card .min-max {
        font-size: 1.27rem; /* +15% von 1.1rem */
        opacity: 0.9;
        margin-top: 4px;
    }

    .today-weather-card .description {
        font-size: 2.19rem; /* +15% von 1.9rem */
        margin-top: 11px;
        text-transform: capitalize;
    }

    .today-weather-card .details {
        border-top: 1px solid rgba(255, 255, 255, 0.1);
        padding-top: 14px;
        margin-top: 17px;
    }

    .today-weather-card .detail-item {
        display: flex;
        justify-content: space-between;
        font-size: 2.19rem; /* +15% von 1.9rem */
        margin-bottom: 6px;
    }

    .today-weather-card .detail-item .label {
        opacity: 0.8;
    }

    .next-days-header { /* Aus Aktuelles App.svelte, passt gut */
        font-size: 3.5rem;
        font-weight: 600;
        text-align: center;
        margin: 130px 0 40px 0;
        display: inline-block;
        padding: 4px 16px;
        border-radius: 8px;
        background-color: rgba(255, 255, 255, 0.2);
        backdrop-filter: blur(4px);
    }

    .next-weather { /* Layout für kommende Tage aus Faruk-CSS */
        display: flex;
        flex-wrap: wrap; /* Erlaubt Umbruch bei Bedarf */
        justify-content: center; /* Zentriert die Karten, wenn sie umbrechen */
        gap: 20px; /* Abstand zwischen Karten */
        width: 100%;
        background: rgba(255, 255, 255, 0.05);
        backdrop-filter: blur(10px);
        border-radius: 20px;
        padding: 20px;
        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
        border: 1px solid rgba(255, 255, 255, 0.1);
    }

    .weather-card { /* Styling der einzelnen Tageskarten aus Faruk-CSS */
        font-size: 1rem; /* Kleinere Schrift für die Karten */
        text-align: center;
        box-shadow: 0 2px 15px rgba(0, 0, 0, 0.1);
        background-color: white; /* Etwas transparenter */
        border-radius: 12px;
        padding: 15px;
        backdrop-filter: blur(8px);
        transition: transform 0.2s ease, box-shadow 0.2s ease;
        border: 1px solid rgba(255, 255, 255, 0.08);
        width: 130px; /* Feste Breite für konsistente Darstellung */
        cursor: pointer;
    }

    .weather-card:hover, .weather-card.active { /* .active für ausgewählten Tag */
        transform: translateY(-4px);
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
        background-color: rgba(0, 0, 0, 0.25);
    }

    .weather-card .date {
        font-size: 1.9em;
        font-weight: 600;
        margin-bottom: 5px;
    }

    .weather-card .weather-icon.small img {
        width: 40px;
        height: 40px;
        margin: 3px auto;
        font-size: 1.9rem;
    }

    .weather-card .description {
        font-size: 1.8em;
        margin-top: 3px;
        opacity: 0.8;
        height: 2.4em;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .weather-card .min-max {
        font-size: 1.85rem;
        margin-top: 5px;
    }

    .weather-card .min-max .max::after {
        opacity: 0.7;
        margin: 0 2px;
    }

    /* Allgemeine .min und .max Farben */
    .max, .max-temp {
        color: #ff9900;
    }

    /* Wärmeres Orange/Gelb */
    .min, .min-temp {
        color: #007bff;
    }

    /* Helleres Blau */

    /* Lade-, Fehler- und Leerzustände (aus Faruk-CSS) */
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

    .error-message { /* Aus Faruk-CSS */
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

    /* Media Queries für Responsiveness */
    @media (max-width: 768px) {
        .app-header {
            align-items: center; /* Zentriert bei Umbruch */
        }

        .today-weather-card .weather-info {
            flex-direction: column;
            gap: 15px;
        }

        .today-weather-card .temperature-container {
            text-align: center;
        }

        .city-input {
            font-size: 1.5rem;
        }

        .search-form {
            width: 90%; /* Etwas breiter */
        }

        h1 {
            font-size: 2.8rem;
        }
    }

    @media (max-width: 480px) {
        .weather-app {
            padding: 15px;
        }

        h1 {
            font-size: 2rem;
        }

        .city-input {
            font-size: 1.2rem;
        }

        .today-weather-card .current-temp {
            font-size: 2.8rem;
        }

        .today-weather-card .min-max {
            font-size: 1rem;
        }

        .today-weather-card .weather-icon img {
            width: 80px;
            height: 80px;
        }

        .weather-card {
            width: 110px;
            padding: 12px;
        }

        /* Kleinere Karten */
        .weather-card .weather-icon img {
            width: 35px;
            height: 35px;
        }
    }
</style>
