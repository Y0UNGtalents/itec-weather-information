<script>
    import WeatherLottieIcon from "./WeatherLottieIcon.svelte";

    let { day } = $props();

    function formatCityLocalDate(localTime) {
        return new Date(localTime).toLocaleDateString("de-DE", {
            weekday: "long",
            year: "numeric",
            month: "long",
            day: "numeric",
            timeZone: "UTC",
        });
    }

    function formatCityLocalClock(localTime) {
        return new Date(localTime).toLocaleTimeString("de-DE", {
            hour: "2-digit",
            minute: "2-digit",
            timeZone: "UTC",
        });
    }
</script>

<div class="today-weather-card">
    <div class="city-name-display">{day.city}</div>
    <div class="date">{formatCityLocalDate(day.localTime ?? day.date)}</div>
    <div class="local-time">{formatCityLocalClock(day.localTime ?? day.date)} Ortszeit</div>

    <div class="weather-info">
        <div class="temperature-container">
            <div class="container-temp">
                <div class="current-temp">
                    {(day.temperature !== undefined ? Math.round(day.temperature) : '--')}°C
                </div>
                <div class="weather-icon">
                    {#key day.weatherCondition}
                        <WeatherLottieIcon condition={day.weatherCondition} />
                    {/key}
                </div>
                <div class="min-max">
                    <div class="description">{day.description}</div>
                    <span class="min">Min: {Math.round(day.minTemperature)}°C</span>
                    <span class="max">Max: {Math.round(day.maxTemperature)}°C</span>
                </div>
            </div>
        </div>
    </div>
</div>

<style>
    .today-weather-card {
        width: 100%;
        max-width: 620px;
        font-size: 1.38rem;
        text-align: start;
        margin-left: 2em;
        border-radius: 16px;
        padding: 23px 28px;
        color: #ffffff;
        font-family: "Inter", sans-serif;
    }

    .today-weather-card .date {
        font-size: 1em;
        margin-bottom: 4px;
        text-align: left;
        font-weight: 500;
    }

    .today-weather-card .local-time {
        font-size: 0.85em;
        margin-bottom: 9px;
        text-align: left;
        opacity: 0.8;
    }

    .today-weather-card .city-name-display {
        font-size: 1.5em;
        font-weight: 600;
        margin-bottom: 4px;
        text-align: left;
    }

    .weather-info {
        display: flex;
        align-items: center;
        justify-content: center;
        flex-wrap: wrap;
    }

    .temperature-container {
        text-align: left;
        flex: 1;
    }

    .container-temp {
        display: flex;
        margin-left: 10em;
    }

    .current-temp {
        font-size: 12rem;
        font-weight: 400;
        margin-left: 1em;
        margin-top: 10px;
    }

    .min-max {
        font-size: 1.4rem;
        margin-top: 25px;
        display: flex;
        flex-direction: column;
        padding-left: 20px;
    }

    .max {
        color: var(--temperature-max-color);
        padding-top: 10px;
        text-align: left;
    }

    .min {
        color: var(--temperature-min-color);
        padding-top: 10px;
        text-align: left;
    }

    .weather-icon {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        margin-left: 80px;
        margin-top: 20px;
    }

    .description {
        font-size: 1.6rem;
        margin-top: 40px;
        font-weight: bold;
        text-align: left;
        width: 300px;
    }
</style>
