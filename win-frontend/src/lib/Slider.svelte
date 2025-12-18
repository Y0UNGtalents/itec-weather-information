<script>
    import { onMount } from 'svelte';
    import WeatherLottieIcon from './WeatherLottieIcon.svelte';

    export let items = [];

    export let currentIndex = 0;
    export let selectDay = (i) => {};
    export let formatDay = (date) =>
        new Date(date).toLocaleDateString('de-DE', { weekday: 'long' });

    let active = 1;
    let sliderContainer;
    let interval;
    let elements = [];
    const loadShow = () => {
        if (!elements[active]) return;

        const containerWidth = sliderContainer.offsetWidth;
        const cardWidth = elements[active].offsetWidth;
        const centerOffset = (containerWidth - cardWidth) / 2;

        // Zentrale Karte
        elements[active].style.transform = 'none';
        elements[active].style.zIndex = 100;
        elements[active].style.filter = 'none';
        elements[active].style.opacity = 1;
        elements[active].style.left = `${centerOffset}px`;

        // Rechte Karten
        let stt = 0;
        for (let i = active + 1; i < elements.length; i++) {
            stt++;
            elements[i].style.transform = `translateX(${3 * stt}px) scale(${1 - 0.2 * stt})`;
            elements[i].style.zIndex = -stt;
            elements[i].style.filter = 'blur(5px)';
            elements[i].style.opacity = stt > 0 ? 0 : 0;
            elements[i].style.left = `${centerOffset}px`;
        }

        // Linke Karten
        stt = 0;
        for (let i = active - 1; i >= 0; i--) {
            stt++;
            elements[i].style.transform = `translateX(${-3 * stt}px) scale(${1 - 0.2 * stt})`;
            elements[i].style.zIndex = -stt;
            elements[i].style.filter = 'blur(5px)';
            elements[i].style.opacity = stt > 0 ? 0 : 0.0;
            elements[i].style.left = `${centerOffset}px`;
        }
    };

    onMount(() => {
        elements = sliderContainer.querySelectorAll('.card');
        loadShow();

        interval = setInterval(() => {
            active = (active + 1) % elements.length;
            loadShow();
        }, 4000);

        return () => clearInterval(interval);
    });
</script>


<div class="flip-carousel" bind:this={sliderContainer}>
    {#each items as day, index (day.date + '-' + currentIndex)}
        <div class="card" on:click={() => selectDay(index)}>
            <div class="content">
                <div class="date">{formatDay(day.date)}</div>
                <div class="weather-icon small">
                    <WeatherLottieIcon condition={day.weatherCondition} />
                </div>
                <div class="min-max">
                    <span class="min">Min: {Math.round(day.minTemperature)}°</span> /
                    <span class="max">Max: {Math.round(day.maxTemperature)}°</span>

                </div>

                <div class="description">{day.description}</div>
            </div>
        </div>
    {/each}
</div>


<style>
    .flip-carousel {
        margin-top: 2em;
        display: flex;
        justify-content: center;
        align-items: center;
        perspective: 1000px;
        position: relative;
        height: 300px;
        margin-bottom: 30px;
        transform-style: preserve-3d;
        transition: transform 1s linear;
        max-width: 40%;
        margin-left: 18em;
    }

    .card {
        width: 500px;
        height: 400px;
        position: absolute;
        top: 0;
        left: 0;
        opacity: 0.3;
        transition: transform 1s ease, opacity 0.2s ease;
        transform-style: preserve-3d;
        background: rgba(255, 255, 255, 0.06);
        border-radius: 60px;
        box-shadow: 0 4px 30px rgba(0, 0, 0, 0.1);
        backdrop-filter: blur(5px);
        -webkit-backdrop-filter: blur(5px);
        border: 1px solid rgba(255, 255, 255, 0.3);
        color: white;
        
        display: flex;
        
        justify-content: center;
        align-items: center;
        
        
    }

    .card .content {
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;
        width: 100%;
        height: 100%;
    }

    .date {
        font-size: 2rem;
        font-weight: 700;
        padding-top: 20px;
        
    }

    .weather-icon.small {
        flex-grow: 0.8;
        transform: scale(0.8);
        
        
    }

    

    .min-max {
        font-size: 1.5rem;
        font-weight: bold;
        
    }

    .description {
        font-size: 2.4rem;
        font-weight: 700;
        opacity: 0.9;
        padding-top: 20px;
        padding-bottom: 20px;
        
    }

    .max {
        color: #ff9900;
    }

    .min {
        color: #007bff;
    }

</style>
