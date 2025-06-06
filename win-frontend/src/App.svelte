<script>
    import {onMount, onDestroy} from "svelte";

    // State variables
    let city = "Heilbronn";
    let weatherData = [];
    let loading = false;
    let error = null;
    let dailyForecasts = [];
    let selectedDayIndex = 0;
    let recentSearches = [];
    let showRecentSearches = false;

    let isConnected = true;
    let isSocketConnected = false;    // Für WebSocket-Verbindung

    // Voice recognition variables
    let recognition = null;
    let lang = 'de-DE';
    let isRecording = false;
    let isListening = false;

    let triggerWord = "Wetter";
    let voicePrompt = "";
    let inputRef = null;
    let showVoiceHint = true;

    // WebSocket
    let socket = null;

    //anoimation variablen
    let drops = Array.from({length: 100});
    let snowflakes = Array.from({length: 100});
    let clouds = Array(30).fill(null);

    // Function to fetch weather data
    async function fetchWeatherData(cityName) {

        if (!cityName || !cityName.trim()) {
            console.warn("fetchAndDisplayWeatherData: Kein Stadtname angegeben.");
            error = "Bitte geben Sie einen Stadtnamen an.";
            loading = false; // Sicherstellen, dass Loading beendet wird
            return;
        }
        //city = cityName;
        loading = true;
        error = null;

        try {
            console.log(`Fetching weather for: ${cityName}`);
            const response = await fetch(
                `http://localhost:8080/api/weather/${cityName}`,
            );

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(
                    `Wetterdaten für ${cityName} nicht gefunden (${response.status}): ${errorText || response.statusText}`
                );
            }

            const newWeatherData = await response.json();
            console.log(JSON.stringify( newWeatherData))
            weatherData = newWeatherData;
            processWeatherData();

            // Stadt zur Suchhistorie hinzufügen
            const displayCity = (dailyForecasts.length > 0 && dailyForecasts[0].city) ? dailyForecasts[0].city : cityName;
            if (!recentSearches.includes(displayCity)) {
                recentSearches = [displayCity, ...recentSearches.slice(0, 4)];
                localStorage.setItem(
                    "recentSearches",
                    JSON.stringify(recentSearches),
                );
            }
            if (dailyForecasts.length > 0 && dailyForecasts[0].city) {
                city = dailyForecasts[0].city; // Aktualisiere die globale 'city' Variable
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

        // Group forecasts by day
        const groupedByDay = {};

        weatherData.forEach((item) => {
            const date = new Date(item.forecastDate);

            if (isNaN(date.getTime())) {
                console.warn("Invalid date in weather data:", item);
                return;
            }

            const dayKey = date.toISOString().split("T")[0]; // YYYY-MM-DD format

            if (!groupedByDay[dayKey]) {
                groupedByDay[dayKey] = [];
            }
            groupedByDay[dayKey].push(item);
        });

        // Create daily summary for each day
        dailyForecasts = Object.keys(groupedByDay)
            .map((day) => {
                const forecasts = groupedByDay[day];
                const dayData = forecasts[0]; // Use first forecast for basic info
                const currentTemp = dayData.temperature
                // Find min and max temperatures for the day
                const minTemp = Math.min(...forecasts.map((f) => f.minTemperature));
                const maxTemp = Math.max(...forecasts.map((f) => f.maxTemperature));

                // Calculate average humidity
                const avgHumidity = Math.round(
                    forecasts.reduce((sum, f) => sum + f.humidity, 0) / forecasts.length
                );

                // Get the noon forecast if available, otherwise use the first one
                const noonForecast =
                    forecasts.find((f) => {
                        const date = new Date(f.forecastDate);
                        return date.getHours() >= 12 && date.getHours() < 15;
                    }) || dayData;

                // Get weather condition category
                const weatherCondition = getWeatherCondition(noonForecast.description);

                // Filter out forecasts with duplicate formatted times
                const uniqueForecasts = [];
                const seenTimes = new Set();

                // Sort forecasts by time first
                const sortedForecasts = forecasts.sort(
                    (a, b) => new Date(a.forecastDate) - new Date(b.forecastDate)
                );

                // Keep only one forecast per formatted time
                sortedForecasts.forEach(forecast => {
                    const formattedTime = new Date(forecast.forecastDate).toLocaleTimeString("de-DE", {
                        hour: "2-digit",
                        minute: "2-digit",
                    });

                    if (!seenTimes.has(formattedTime)) {
                        seenTimes.add(formattedTime);
                        uniqueForecasts.push(forecast);
                    }
                });

                return {
                    date: new Date(day),
                    city: dayData.city,
                    temperature: currentTemp,
                    minTemperature: minTemp,
                    maxTemperature: maxTemp,
                    avgHumidity: avgHumidity,
                    description: noonForecast.description,
                    iconCode: noonForecast.iconCode,
                    weatherCondition: weatherCondition,
                    forecasts: uniqueForecasts, // Use filtered forecasts without duplicates
                };
            })
            .sort((a, b) => a.date - b.date); // Sort by date

        // Reset selected day to first day
        selectedDayIndex = 0;
        //city = dailyForecasts[0].city
    }

    // Get weather condition category
    function getWeatherCondition(description) {
        if (!description) return "default";
        const desc = description.toLowerCase();
        if (desc.includes("klar") || desc.includes("himmel")) return "clear";
        if (desc.includes("wolke") || desc.includes("bewölkt")) return "cloudy";
        if (desc.includes("regen")) return "rain";
        if (desc.includes("schnee")) return "snow";
        if (desc.includes("gewitter") || desc.includes("sturm")) return "storm";
        if (desc.includes("nebel")) return "fog";
        return "default";
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

    function formatDay(date) {
        return new Date(date).toLocaleDateString("de-DE", {weekday: "short"});
    }

    function formatTime(dateString) {
        return new Date(dateString).toLocaleTimeString("de-DE", {hour: "2-digit", minute: "2-digit"});
    }

    // Get weather icon URL
    function getWeatherIconUrl(iconCode) {
        if (!iconCode) return "";
        if (iconCode.includes('n')) {
            switch (iconCode) {
                case '01n':
                    return `https://openweathermap.org/img/wn/01d@2x.png`;
                case '02n':
                    return `https://openweathermap.org/img/wn/02d@2x.png`;
                case '03n':
                    return `https://openweathermap.org/img/wn/03d@2x.png`;
                case '04n':
                    return `https://openweathermap.org/img/wn/02d@2x.png`;
                case '10n':
                    return `https://openweathermap.org/img/wn/10d@2x.png`;
            }
        }
        if (iconCode === '04d') {
            return `https://openweathermap.org/img/wn/02d@2x.png`;
        }
        return `https://openweathermap.org/img/wn/${iconCode}@2x.png`;
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
            if ('SpeechRecognition' in window ) {
                console.log("SpeechRecognition")
            } else {
                console.log("webkitSpeechRecognition")
            }
            console.warn("Speech Recognition API nicht im Browser unterstützt.");
            error = "Spracherkennung wird von Ihrem Browser nicht unterstützt.";
            // isListening bleibt false, keine Endlosschleife versuchen
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
                if (isListening && wasRecording && isSocketConnected && recognition) { // Nur neu starten, wenn es gewünscht ist UND es vorher lief
                    setTimeout(() => {
                        try {
                            if (isListening && recognition && !isRecording) { // Erneute Prüfung
                                console.log("Restarting recognition from onend...");
                                recognition.start();
                            }
                        } catch (e) {
                            console.warn("Konnte Spracherkennung nicht neu starten nach onend:", e.name, e.message);
                            if (e.name === 'InvalidStateError') {
                                // Vielleicht war es schon wieder gestartet, ignoriere oder logge nur
                            } else {
                                // Andere Fehler behandeln
                                isListening = false; // Im Zweifel das Lauschen stoppen
                            }
                        }
                    }, 300);
                }
            };

            recognition.onresult = (event) => {
                let transcript = "";
                // Gehe durch alle Ergebnisse (obwohl continuous=false meist nur eines hat)
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
                voicePrompt = "Fehler bei der Spracherkennung.";
                if (event.error !== 'no-speech' && event.error !== 'aborted') {
                    error = `Spracherkennungsfehler: ${event.error}. ${event.message || ''}`;
                }
                // Bei bestimmten Fehlern (z.B. 'not-allowed', 'service-not-allowed') das Lauschen dauerhaft stoppen
                if (['not-allowed', 'service-not-allowed', 'audio-capture', 'network'].includes(event.error)) {
                    isListening = false; // Stoppt die Neustart-Schleife in onend
                    if (recognition) recognition.abort(); // Versuche, es sauber zu stoppen
                } else if (isListening && isSocketConnected && recognition) {
                    // Bei anderen Fehlern (z.B. 'no-speech') ggf. neu starten, wenn gewollt
                    setTimeout(() => {
                        if (isListening && recognition) recognition.start();
                    }, 1000);
                }
            };

            if (isListening) { // isListening wird in initWebSocket.onopen auf true gesetzt
                console.log("Attempting to start initial speech recognition...");
                recognition.start();
            }
        } catch (e) {
            console.error("Error setting up speech recognition:", e);
            error = "Fehler bei der Initialisierung der Spracherkennung.";
            isListening = false; // Sicherstellen, dass es gestoppt ist
        }
    }

    function initWebSocket() {
        if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
            console.log("WebSocket ist bereits offen oder verbindet sich.");
            return;
        }

        if (socket) { // Wenn existiert aber nicht offen/connecting (z.B. closed, closing)
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
            isListening = true; // Signal zum Starten der Spracherkennungsschleife
            setupSpeechRecognition();
        };

        socket.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                console.log("Message from Python backend:", data);

                if (data.type === 'command_understood_display_weather' && data.weatherPayload) {
                    weatherData = data.weatherPayload;
                    processWeatherData();
                    // Stadtname wird durch processWeatherData gesetzt, falls in Daten vorhanden
                    city = (dailyForecasts.length > 0 && dailyForecasts[0].city) ? dailyForecasts[0].city : (data.city || "Unbekannt");

                    // Optional: Kurz die Spracherkennung pausieren, um Echo zu vermeiden
                    if (recognition && isListening) {
                        let originalIsListeningState = isListening;
                        isListening = false; // Pausiert den Neustart in onend
                        recognition.stop();
                        setTimeout(() => {
                            isListening = originalIsListeningState; // Erlaube Neustart wieder
                            if (isListening && isSocketConnected && recognition && !isRecording) { // Erneute Prüfung
                                recognition.start();
                            }
                        }, 2000); // 2 Sekunden Pause
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
                recognition.onend = null; // Verhindere automatischen Neustart
                recognition.stop();
            }
            // Optional: reconnect logic (vorsichtig sein mit Endlosschleifen)
            // if (event.code !== 1000) { // 1000 = Normal closure
            //    console.log("Attempting to reconnect WebSocket in 5 seconds...");
            //    setTimeout(initWebSocket, 5000);
            // }
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

    /**
     function parseVoiceCommand(command) {
     // Extract city and days from command
     const words = command.trim().split(/\s+/);
     let newDays = 5;
     let cityWords = [];

     for (let i = 0; i < words.length; i++) {
     const num = parseInt(words[i]);
     if (!isNaN(num)) {
     newDays = Math.min(Math.max(num, 1), 5); // Limit between 1-5
     } else {
     cityWords.push(words[i]);
     }
     }

     const newCity = cityWords.join(" ").trim();
     if (newCity) {
     city = newCity;
     days = newDays;
     fetchWeatherData();
     }
     }*/

    function checkConnection() {
        isConnected = navigator.onLine;
        window.addEventListener('online', () => isConnected = true);
        window.addEventListener('offline', () => {
            isConnected = false;
            isListening = false;
            if (recognition && isRecording) recognition.stop();
        });
    }

    // Handle form submission
    function handleSubmit(event) {
        event.preventDefault();
        fetchWeatherData(city);
        showRecentSearches = false;
    }

    function selectDay(index) {
        selectedDayIndex = index;
    }

    // Select a recent search
    function selectRecentSearch(searchTerm) {
        city = searchTerm;
        fetchWeatherData(searchTerm);
        showRecentSearches = false;
    }

    // Initialize with a default city and load recent searches
    onMount(() => {
        const savedSearches = localStorage.getItem("recentSearches");
        if (savedSearches) {
            recentSearches = JSON.parse(savedSearches);
        }
        checkConnection();
        initWebSocket();
        if (city) fetchWeatherData(city);

        return () => { // Cleanup-Funktion für onDestroy
            console.log("App unmounting. Cleaning up resources.");
            isListening = false; // Wichtig, um die onend-Schleife zu stoppen
            if (recognition) {
                recognition.onstart = null;
                recognition.onresult = null;
                recognition.onend = null;
                recognition.onerror = null;
                recognition.abort(); // Versuche, die Erkennung aktiv zu stoppen
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

    // Animationsvariablen und -funktionen
    function generateCloudStyle() {
        const left = Math.random() * 120;
        const bottom = Math.random() * 550 + 150;
        const opacity = Math.random() * (0.8 - 0.4) + 0.4;
        const scale = Math.random() * (1 - 0.4) + 0.4;
        const animationDelay = Math.floor(Math.random() * 10); // Zufälliger Delay
        const animationDuration = Math.random() * (30 - 15) + 15; // Zufällige Dauer
        return `
        left: ${left}%;
        bottom: ${bottom}px;
        opacity: ${opacity};
        transform: scale(${scale});
        animation-delay: ${animationDelay}s;
        animation-duration: ${animationDuration}s;
        --cloud-y: ${Math.random() * 10 - 5}; /* Leichte y-Variation */
        --cloud-scale: ${scale};
        --cloud-opacity: ${opacity};
    `;
    }
</script>

<main>
    {#if !isSocketConnected && error && (error.includes("WebSocket") || error.includes("Sprachserver getrennt"))}
        <div class="status-overlay error">
            🔴 WebSocket nicht verbunden. Sprachsteuerung ist offline. {error.includes("versuche") ? error : "Versuche neu zu verbinden..."} <br/> Fehler: {error}
        </div>
    {:else if !isConnected}
        <div class="status-overlay error">
            🔴 Keine Internetverbindung. Wetterdaten und Sprachsteuerung könnten beeinträchtigt sein.
        </div>
    {/if}

    <div class="weather-app" style="background: {getBackgroundGradient(dailyForecasts[selectedDayIndex] || null)}">
        {#if dailyForecasts.length > 0 && dailyForecasts[selectedDayIndex]}
            {@const currentDayForAnimation = dailyForecasts[selectedDayIndex]} <!-- Eigene Konstante für Animationen -->
            <div class="weather-animation">
                {#if currentDayForAnimation.weatherCondition === 'rain'}
                    <div class="rain-animation">
                        {#each drops as _, i (i)}
                            <div class="drop" style="left: {Math.random() * 100}%; animation-delay: {Math.random()}s;"></div>
                        {/each}
                    </div>
                {/if}
                {#if currentDayForAnimation.weatherCondition === 'snow'}
                    <div class="snow-animation">
                        {#each snowflakes as _, i (i)}
                            <div class="snowflake" style="left: {Math.random() * 100}%; animation-delay: {Math.random() * 5}s; animation-duration: {5 + Math.random() * 5}s; --wind: {Math.random() * 4 - 2};"></div>
                        {/each}
                    </div>
                {/if}
                {#if currentDayForAnimation.weatherCondition === 'cloudy'}
                    <div class="cloudy-animation">
                        {#each clouds as _, i (i)}
                            <div class="cloud" style="{generateCloudStyle()}"></div>
                        {/each}
                    </div>
                {/if}
                {#if currentDayForAnimation.weatherCondition === 'fog'}
                    <div class="fog-animation">
                        {#each Array.from({length: 5}) as _, i (i)}
                            <div class="fog-layer" style="--fog-opacity: {0.4 - i * 0.05}; --fog-animation-duration: {20 + i * 3}s; --fog-animation-delay: {i * 0.7}s;"></div>
                        {/each}
                    </div>
                {/if}
                {#if currentDayForAnimation.weatherCondition === 'storm'}
                    <div class="storm-animation">
                        {#each drops as _, i (i)}
                            <div class="drop storm-drop" style="left: {Math.random() * 100}%; animation-delay: {Math.random() * 0.5}s;"></div>
                        {/each}
                        <div class="lightning-container">
                            <div class="lightning"></div>
                            <div class="lightning delayed"></div>
                        </div>
                    </div>
                {/if}
            </div>
        {/if}

        <div class="app-content-wrapper">
            <div class="app-header">
                <h1>Wetter Vorhersage</h1>
                {#if showVoiceHint}
                    <div class="voice-hint">
                        <div class="voice-hint-content">
                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
                                <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
                                <line x1="12" y1="19" x2="12" y2="23"></line>
                                <line x1="8" y1="23" x2="16" y2="23"></line>
                            </svg>
                            <span>Sage "{triggerWord} [Stadtname]" (z.B. "{triggerWord} Berlin")</span>
                        </div>
                        <button class="voice-hint-close" on:click={() => showVoiceHint = false} aria-label="Hinweis schließen">×</button>
                    </div>
                {/if}

                <div class="search-container">
                    <form on:submit|preventDefault={handleSubmit} class="search-form">
                        <div class="input-wrapper {isListening || isRecording ? 'listening' : ''}">
                            <input bind:value={city} bind:this={inputRef}
                                   placeholder={isRecording ? voicePrompt : "Stadt eingeben..."}
                                   class="city-input"
                                   on:focus={() => showRecentSearches = true}
                                   on:keypress={(e) => { if (e.key === 'Enter') handleSubmit(e); }}
                                   aria-label="Stadtsuche Textfeld"
                                   readonly={isRecording}
                            />
                            {#if showRecentSearches && recentSearches.length > 0}
                                <div class="recent-searches">
                                    {#each recentSearches as searchItem (searchItem)}
                                        <div class="recent-search-item" role="button"
                                             on:click={() => selectRecentSearch(searchItem)}
                                             on:keydown={(e) => {if(e.key === 'Enter') selectRecentSearch(searchItem)}}
                                             tabindex="0">{searchItem}</div>
                                    {/each}
                                </div>
                            {/if}
                        </div>
                        <button type="submit" class="search-button" aria-label="Wetter suchen">
                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                <circle cx="11" cy="11" r="8"></circle>
                                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                            </svg>
                        </button>
                    </form>
                </div>
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
                    <div class="city-name-display" style="font-size: 2.2rem; margin-bottom: 10px;">{currentDay.city}</div>

                    <div class="weather-info">
                        <div class="temperature-container">
                            <div class="current-temp" style="font-size: 3.5rem; margin-bottom: 5px;">
                                {currentDay.forecasts && currentDay.forecasts.length > 0 && currentDay.forecasts[0].temperature !== undefined ?
                                    Math.round(currentDay.forecasts[0].temperature) :
                                    (currentDay.temperature !== undefined ? Math.round(currentDay.temperature) : '--')
                                }°C
                            </div>
                            <div class="min-max">
                                <span class="max">Max: {Math.round(currentDay.maxTemperature)}°C</span> / <span class="min">Min: {Math.round(currentDay.minTemperature)}°C</span>
                            </div>
                        </div>

                        <div class="weather-icon">
                            <img src={getWeatherIconUrl(currentDay.iconCode)} alt={currentDay.description}/>
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

                <!-- ANZEIGE DER KOMMENDEN TAGE -->
                {#if dailyForecasts.length > 1}
                    <div class="next-days-header">Kommende Tage</div>
                    <div class="next-weather">
                        {#each dailyForecasts as day, index (day.date)}
                            {#if index > 0 && index < 6}
                                <div class="weather-card"
                                     on:click={() => selectDay(index)}
                                     on:keydown={e => { if (e.key === 'Enter' || e.key === ' ') selectDay(index); }}
                                     role="button" tabindex="0"
                                     class:active={selectedDayIndex === index}>
                                    <div class="date" style="font-size: 0.9rem; margin-bottom: 5px;">{formatDay(day.date)}</div>
                                    <div class="weather-icon small">
                                        <img src={getWeatherIconUrl(day.iconCode)} alt={day.description}/>
                                    </div>
                                    <div class="min-max" style="font-size:0.9rem; margin-top: 5px;">
                                        <span class="max">{Math.round(day.maxTemperature)}°</span>/<span class="min">{Math.round(day.minTemperature)}°</span>
                                    </div>
                                    <div class="description" style="font-size: 0.8rem; margin-top: 3px;">{day.description}</div>
                                </div>
                            {/if}
                        {/each}
                    </div>
                {/if}
            {:else if !loading && !error } <!-- Dieser 'else if' ist jetzt korrekt innerhalb des Haupt-Blocks -->
                <div class="no-data">
                    <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round"><path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z"></path></svg>
                    <p>Keine Wetterdaten verfügbar. Bitte suchen Sie nach einer Stadt oder verwenden Sie die Sprachsteuerung (sage "{triggerWord} [Stadt]").</p>
                </div>
            {/if} <!-- Schließt den {#if loading} ... {:else if ...} ... {:else if ...} Block -->
        </div> <!-- Schließt app-content-wrapper -->
    </div> <!-- Schließt weather-app -->
</main>

<style>
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
        color: white;
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

    .weather-animation { /* Aus "Faruk App.svelte" */
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        pointer-events: none;
        overflow: hidden;
        z-index: 0;
    }

    /* Header und Suche (Kombination) */
    .app-header { /* Aus "Faruk App.svelte" */
        display: flex;
        justify-content: center;
        align-items: center;
        margin-bottom: 30px;
        flex-wrap: wrap;
        gap: 20px;
        flex-direction: column;
        width: 100%;
    }

    h1 { /* Aus "Faruk App.svelte" */
        margin: 0;
        font-size: 3.5rem;
        font-weight: 700;
        text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.2);
        text-align: center;
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


    /* Aktueller Tag & Kommende Tage (basiert auf Faruk-Struktur) */
    .today-weather-card { /* Klasse aus Faruk-CSS */
        width: 100%; /* Nimmt volle Breite des .app-content-wrapper */
        max-width: 600px; /* Zentriert und begrenzt */
        font-size: 1.2rem; /* Basisschriftgröße für die Karte */
        text-align: center; /* Standardausrichtung */
        background-color: rgba(0, 0, 0, 0.2);
        border-radius: 16px;
        padding: 20px 25px;
        margin: 0 auto 30px auto; /* Zentriert und Abstand */
        backdrop-filter: blur(10px);
        transition: transform 0.2s;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
        border: 1px solid rgba(255, 255, 255, 0.1);
    }

    .today-weather-card:hover {
        transform: translateY(-5px);
    }

    .today-weather-card .date {
        font-size: 1rem;
        margin-bottom: 8px;
        opacity: 0.8;
    }

    .today-weather-card .city-name-display {
        font-size: 2.2rem;
        font-weight: 600;
        margin-bottom: 15px;
    }

    .today-weather-card .weather-info { /* Flex-Layout für Icon und Temp */
        display: flex;
        align-items: center; /* Vertikal zentrieren */
        justify-content: center; /* Horizontal zentrieren */
        gap: 25px;
        margin-bottom: 15px;
        flex-wrap: wrap; /* Falls nicht genug Platz */
    }

    .today-weather-card .weather-icon img {
        width: 100px;
        height: 100px;
    }

    /* Größe aus Faruk-CSS */
    .today-weather-card .temperature-container {
        text-align: left;
    }

    .today-weather-card .current-temp {
        font-size: 3.5rem;
        font-weight: 300;
        line-height: 1;
        margin: 0;
    }

    .today-weather-card .min-max {
        font-size: 1.1rem;
        opacity: 0.9;
        margin-top: 3px;
    }

    .today-weather-card .min-max .max::after {
        content: " / ";
        opacity: 0.7;
        margin: 0 4px;
    }

    .today-weather-card .description {
        font-size: 1.2rem;
        margin-top: 10px;
        text-transform: capitalize;
    }

    .today-weather-card .details {
        border-top: 1px solid rgba(255, 255, 255, 0.1);
        padding-top: 12px;
        margin-top: 15px;
    }

    .today-weather-card .detail-item {
        display: flex;
        justify-content: space-between;
        font-size: 1rem;
        margin-bottom: 5px;
    }

    .today-weather-card .detail-item .label {
        opacity: 0.8;
    }


    .next-days-header { /* Aus Aktuelles App.svelte, passt gut */
        font-size: 1.5rem;
        font-weight: 600;
        text-align: center;
        margin: 25px 0 15px 0;
        width: 100%;
    }

    .next-weather { /* Layout für kommende Tage aus Faruk-CSS */
        display: flex;
        flex-wrap: wrap; /* Erlaubt Umbruch bei Bedarf */
        justify-content: center; /* Zentriert die Karten, wenn sie umbrechen */
        gap: 20px; /* Abstand zwischen Karten */
        padding: 10px 0;
        width: 100%;
    }

    .weather-card { /* Styling der einzelnen Tageskarten aus Faruk-CSS */
        font-size: 1rem; /* Kleinere Schrift für die Karten */
        text-align: center;
        box-shadow: 0 2px 15px rgba(0, 0, 0, 0.1);
        background-color: rgba(0, 0, 0, 0.15); /* Etwas transparenter */
        border-radius: 12px;
        padding: 15px;
        /* margin-bottom: 20px; /* Entfernt, da gap im Flex-Container */
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
        font-size: 0.9em;
        font-weight: 600;
        margin-bottom: 5px;
    }

    .weather-card .weather-icon.small img {
        width: 40px;
        height: 40px;
        margin: 3px auto;
    }

    .weather-card .description {
        font-size: 0.8em;
        margin-top: 3px;
        opacity: 0.8;
        height: 2.4em;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .weather-card .min-max {
        font-size: 0.85em;
        margin-top: 5px;
    }

    .weather-card .min-max .max::after {
        content: "/";
        opacity: 0.7;
        margin: 0 2px;
    }


    /* Allgemeine .min und .max Farben */
    .max, .max-temp {
        color: #ffddaa;
    }

    /* Wärmeres Orange/Gelb */
    .min, .min-temp {
        color: #aaddff;
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

    @keyframes spin {
        to {
            transform: rotate(360deg);
        }
    }

    .loading-text {
        font-size: 1rem;
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
        font-size: 1.1rem;
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

    /* Animationen für Wettereffekte */
    .rain-animation .drop {
        position: absolute;
        top: -10%;
        width: 1.5px; /* Etwas dünner */
        height: 15px; /* Etwas kürzer */
        background: rgba(200, 220, 255, 0.5); /* Hellblauer Regen */
        animation: rainDrop 0.6s linear infinite;
        border-radius: 0 0 50% 50%; /* Tropfenform */
    }

    @keyframes rainDrop {
        0% {
            transform: translateY(0) scaleY(1);
            opacity: 1;
        }
        90% {
            opacity: 1;
        }
        100% {
            transform: translateY(110vh) scaleY(0.5);
            opacity: 0;
        }
    }

    .snow-animation .snowflake {
        position: absolute;
        top: -10%;
        background-color: rgba(255, 255, 255, 0.9);
        border-radius: 50%;
        width: clamp(3px, 0.8vw, 7px);
        height: clamp(3px, 0.8vw, 7px);
        animation: snowfall linear infinite;
        opacity: 0; /* Startet unsichtbar */
        filter: blur(0.5px);
    }

    @keyframes snowfall {
        0% {
            transform: translate(0, 0) rotate(0deg);
            opacity: 0;
        }
        5% {
            opacity: 1;
        }
        /* Wird sichtbar */
        100% {
            transform: translate(calc(var(--wind, 0) * 20px), 105vh) rotate(720deg);
            opacity: 0;
        }
        /* --wind kann für Windeffekt genutzt werden */
    }

    .cloudy-animation .cloud {
        position: absolute;
        background-image: radial-gradient(circle, white 60%, transparent 61%),
        radial-gradient(circle, white 60%, transparent 61%),
        radial-gradient(circle, white 60%, transparent 61%),
        radial-gradient(circle, white 60%, transparent 61%);
        background-repeat: no-repeat;
        /* Positionen der "Puffs" für eine Wolkenform */
        background-position: 0% 50%, 30% 30%, 60% 60%, 80% 40%;
        background-size: 50% 50%, 60% 60%, 55% 55%, 45% 45%;
        filter: blur(2px);
        animation: floatCloud linear infinite;
        /* Basiswerte, werden durch generateCloudStyle überschrieben/ergänzt */
        width: 200px;
        height: 100px;
        opacity: 0.5;
    }

    @keyframes floatCloud {
        0% {
            transform: translateX(-250px) translateY(calc(var(--cloud-y, 0) * 2px)) scale(var(--cloud-scale, 1));
            opacity: var(--cloud-opacity, 0.5);
        }
        100% {
            transform: translateX(calc(100vw + 250px)) translateY(calc(var(--cloud-y, 0) * 2px)) scale(var(--cloud-scale, 1));
            opacity: var(--cloud-opacity, 0.5);
        }
    }

    .fog-animation .fog-layer {
        position: absolute;
        width: 250%; /* Breiter für weichere Kanten */
        height: 100%;
        background: linear-gradient(90deg,
        rgba(200, 210, 220, 0) 0%,
        rgba(200, 210, 220, var(--fog-opacity)) 20%,
        rgba(200, 210, 220, var(--fog-opacity)) 80%,
        rgba(200, 210, 220, 0) 100%);
        animation: fogMove var(--fog-animation-duration) linear infinite alternate; /* Alternate für hin und her */
        animation-delay: var(--fog-animation-delay);
        top: calc(15% + var(--fog-animation-delay) * 8); /* Angepasste Position */
        opacity: 0.7;
        filter: blur(10px);
    }

    @keyframes fogMove {
        0% {
            transform: translateX(-30%);
        }
        100% {
            transform: translateX(0%);
        }
    }

    .storm-animation .storm-drop {
        height: 25px;
        width: 2px;
        transform: rotate(20deg); /* Stärkerer Winkel */
        animation: stormDrop 0.4s linear infinite;
        background: rgba(180, 200, 230, 0.6);
    }

    @keyframes stormDrop {
        0% {
            transform: translateY(0vh) rotate(20deg);
            opacity: 1;
        }
        100% {
            transform: translateY(110vh) rotate(20deg);
            opacity: 0;
        }
    }

    .storm-animation .lightning-container {
        position: absolute;
        width: 100%;
        height: 100%;
        top: 0;
        left: 0;
        pointer-events: none;
        overflow: hidden;
    }

    .storm-animation .lightning {
        position: absolute;
        top: 0;
        left: 50%; /* Startet mittig */
        width: 6px; /* Dickerer Blitzkern */
        height: 100%;
        background-color: rgba(255, 255, 255, 0.8);
        opacity: 0;
        transform-origin: top center;
        transform: translateX(-50%) skewX(-20deg) scaleY(0); /* Startet oben, unsichtbar */
        box-shadow: 0 0 20px 10px rgba(255, 255, 255, 0.5);
        animation: lightningStrike 5s ease-out infinite;
    }

    .storm-animation .lightning.delayed {
        animation-delay: 2.5s;
    }

    @keyframes lightningStrike {
        5% {
            opacity: 0;
            transform: translateX(-50%) skewX(-20deg) scaleY(0);
        }
        /* Kurz unsichtbar vor dem Blitz */
        10% {
            opacity: 1;
            transform: translateX(-50%) skewX(-20deg) scaleY(1);
        }
        /* Blitz erscheint */
        12% {
            opacity: 0;
            transform: translateX(-50%) skewX(-20deg) scaleY(1);
        }
        /* Blitz verschwindet kurz */
        14% {
            opacity: 0.7;
            transform: translateX(-50%) skewX(-15deg) scaleY(1);
        }
        /* Nachblitz */
        16% {
            opacity: 0;
            transform: translateX(-50%) skewX(-15deg) scaleY(1);
        }
        100% {
            opacity: 0;
        }
    }

    @keyframes pulse { /* Aus Faruk-CSS, für .input-wrapper.listening */
        0% {
            box-shadow: 0 0 0 0 rgba(255, 100, 100, 0.5);
        }
        70% {
            box-shadow: 0 0 0 10px rgba(255, 100, 100, 0);
        }
        100% {
            box-shadow: 0 0 0 0 rgba(255, 100, 100, 0);
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
