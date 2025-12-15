<script>
    import { onMount } from 'svelte';
    import WeatherLottieIcon from './WeatherLottieIcon.svelte';

    export let items = [];

    export let currentIndex = 0;
    export let selectDay = (i) => {};
    export let formatDay = (date) =>
        new Date(date).toLocaleDateString('de-DE', { weekday: 'short' });

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
            elements[i].style.transform = `translateX(${310 * stt}px) scale(${1 - 0.2 * stt}) perspective(32px) rotateY(-1deg)`;
            elements[i].style.zIndex = -stt;
            elements[i].style.filter = 'blur(5px)';
            elements[i].style.opacity = stt > 3 ? 0 : 0.6;
            elements[i].style.left = `${centerOffset}px`;
        }

        // Linke Karten
        stt = 0;
        for (let i = active - 1; i >= 0; i--) {
            stt++;
            elements[i].style.transform = `translateX(${-330 * stt}px) scale(${1 - 0.2 * stt}) perspective(32px) rotateY(1deg)`;
            elements[i].style.zIndex = -stt;
            elements[i].style.filter = 'blur(5px)';
            elements[i].style.opacity = stt > 6 ? 0 : 0.6;
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
        display: flex;
        justify-content: center;
        align-items: center;
        perspective: 1000px;
        position: relative;
        height: 700px;
        margin-bottom: 30px;
        transform-style: preserve-3d;
        transition: transform 1s linear;
        max-width: 100%;

    }

    .card {
        width: 460px;
        height: 620px;
        position: absolute;
        top: 0;
        left: 0;
        transition: transform 1s ease, opacity 0.6s ease;
        transform-style: preserve-3d;
        background: rgba(255, 255, 255);
        color: black;
        border-radius: 16px;
        display: flex;
        overflow: hidden;
        justify-content: center;
        align-items: center;
        backdrop-filter: blur(5px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
        padding: 20px;
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
        font-size: 3rem;
        font-weight: 700;
        margin-top: 20px;
        margin-bottom: 15px;
    }

    .weather-icon.small {
        flex-grow: 1;
        transform: scale(2.5);
        margin-top: 150px;
        padding-top: 15px;
    }

    .min-max {
        font-size: 2.0rem;
        font-weight: bold;
        margin-bottom: 24px;
    }

    .description {
        font-size: 2.5rem;
        font-weight: 500;
        opacity: 0.9;
        margin-bottom: 20px;
    }

    .max {
        color: #ff9900;
    }

    .min {
        color: #007bff;
    }

</style>
