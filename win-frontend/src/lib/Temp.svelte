<script>
    import WeatherLottieIcon from "./WeatherLottieIcon.svelte";

    export let day; // das aktuelle Tagesobjekt wird übergeben

    function formatDate(date) {
        return new Date(date).toLocaleDateString("de-DE", {
            weekday: "long",
            year: "numeric",
            month: "long",
            day: "numeric",
        });
    }
</script>

<main>
<div class="today-weather-card">
    <div class="city-name-display">{day.city}</div>
    <div class="date">{formatDate(day.date)}</div>

    <div class="weather-info">
        <div class="temperature-container">
            <div class="container-temp">
                <div class="current-temp">
                    {(day.temperature !== undefined ? Math.round(day.temperature) : '--')}°C
                </div>
                <div class="weather-icon">
                    {#key day.weatherCondition}
                    <WeatherLottieIcon condition={day.weatherCondition}/>
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

</main>

<style>

    main {
        margin: 0;
        padding: 0;
        color: #ffffff;
        display: flex;
        flex-direction: column;
        font-family: "Inter", sans-serif;
        
        
        }
    

    * { box-sizing: border-box; }

    

    .today-weather-card {
        width: 100%;
        max-width: 620px;
        font-size: 1.38rem;
        text-align: start;
        margin-left: 2em;
        
        
        border-radius: 16px;
        padding: 23px 28px;
        
        
        
    }

    .today-weather-card .date {
        font-size: 1em;
        margin-bottom: 9px;
        text-align: left;
        font-weight: 500;
        
    }

    .today-weather-card .city-name-display {
        font-size: 1.5em;
        font-weight: 600;
        margin-bottom: 4px;;
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

    .container-temp{
        display: flex;
        margin-left: 10em;

    }

    .current-temp {
        font-size: 12rem;
        font-weight: 400;
        margin-left: 1em;
        margin-top: 10px;
    }

    .gefuhlt-temp {
        

        font-size: 1.2rem;
        font-weight: 400;
        margin-left: 3.5em;
        
        
    }

    .min-max {
        font-size: 1.4rem;
        margin-top: 25px;
        display: flex;
        flex-direction: column;
        padding-left: 20px;
        
    }

    .max { 
        color: #FF9041;
        padding-top: 10px;
        text-align: left;
        
    
    }
    .min { 
        color: rgb(157, 95, 205);
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

    .details {
        background: rgba(255, 255, 255, 0.06);
        border-radius: 60px;
        box-shadow: 0 4px 30px rgba(0, 0, 0, 0.1);
        backdrop-filter: blur(5px);
        -webkit-backdrop-filter: blur(5px);
        border: 1px solid rgba(255, 255, 255, 0.3);
        padding-top: 14px;
        margin-top: 17px;
        width: 175%;
        height: 9em;
        
    }

    .detail-item {
        display: flex;
        justify-content: center;
        font-size: 2rem;
    }

    
    

    

    

    

    
</style>
