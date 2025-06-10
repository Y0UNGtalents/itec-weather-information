<script>
    import WeatherLottieIcon from './WeatherLottieIcon.svelte';

    // Props - Svelte 5 runes mode
    let { items = [] } = $props();

    $effect(() => {
        console.log("@@@@ ITEMS length", items.length)
        console.log("@@@@ ITEMS", JSON.stringify(items, null, 4))
    })

    // State variables
    let activeIndex = $state(1);
    let sliderContainer = $state(null);
    let intervalId = $state(null);
    let cardElements = $state([]);
    let isInitialized = $state(false);

    // Derived values
    let hasItems = $derived(items && items.length > 0);
    let totalItems = $derived(items.length);

    // Constants
    const CARD_SPACING = 400;
    const SCALE_REDUCTION = 0.15;
    const BLUR_FACTOR = 3;
    const OPACITY_REDUCTION = 0.15;
    const MIN_OPACITY = 0.5;
    const AUTO_SLIDE_INTERVAL = 4000;

    // Helper functions
    function styleCard(element, styles) {
        if (!element) return;
        Object.assign(element.style, styles);
    }

    function formatDay(date) {
        return new Date(date).toLocaleDateString('de-DE', {weekday: 'long'})
    }

    function updateSliderDisplay() {
        if (!isInitialized || !cardElements[activeIndex]) return;

        cardElements.forEach((card, index) => {
            if (!card) return;

            if (index === activeIndex) {
                styleCard(card, {
                    transform: 'translate(-50%, -50%) scale(1)',
                    zIndex: 100,
                    filter: 'none',
                    opacity: 1,
                    left: '50%',
                    top: '50%'
                });
            } else {
                const position = index - activeIndex;
                const offset = position * CARD_SPACING;
                const scale = Math.max(0.5, 1 - Math.abs(position) * SCALE_REDUCTION);
                const blur = Math.abs(position) * BLUR_FACTOR;
                const opacity = Math.max(MIN_OPACITY, 1 - Math.abs(position) * OPACITY_REDUCTION);

                styleCard(card, {
                    transform: `translate(-50%, -50%) translateX(${offset}px) scale(${scale})`,
                    zIndex: 50 - Math.abs(position),
                    filter: `blur(${blur}px)`,
                    opacity: opacity,
                    left: '50%',
                    top: '50%'
                });
            }
        });
    }

    function startAutoSlide() {
        if (intervalId) {
            clearInterval(intervalId);
            intervalId = null;
        }

        if (totalItems <= 1) return;

        intervalId = setInterval(() => {
            activeIndex = (activeIndex + 1) % totalItems;
        }, AUTO_SLIDE_INTERVAL);
    }

    function stopAutoSlide() {
        if (intervalId) {
            clearInterval(intervalId);
            intervalId = null;
        }
    }

    // Effects
    $effect(() => {
        if (sliderContainer && hasItems && !isInitialized) {
            setTimeout(() => {
                cardElements = Array.from(sliderContainer.querySelectorAll('.card'));
                isInitialized = cardElements.length > 0;

                if (isInitialized) {
                    updateSliderDisplay();
                }
            }, 50);
        }
    });

    $effect(() => {
        if (isInitialized && totalItems > 1 && !intervalId) {
            startAutoSlide();
        }
    });

    $effect(() => {
        if (isInitialized && cardElements.length > 0) {
            updateSliderDisplay();
        }
    });

    $effect(() => {
        return () => {
            if (intervalId) {
                clearInterval(intervalId);
                intervalId = null;
            }
        };
    });
</script>

{#if hasItems}
    <div class="flip-carousel" bind:this={sliderContainer}>
        {#each items as day, index (day.date + '-' + index)}
            <div
                    class="card"
                    class:active={index === activeIndex}
                    role="button"
                    tabindex="0"
                    aria-label="Wettervorhersage für {formatDay(day.date)}"
            >
                <div class="content">
                    <div class="date">{formatDay(day.date)}</div>
                    <div class="weather-icon small">
                        {#key day.weatherCondition}
                            <WeatherLottieIcon condition={day.weatherCondition}/>
                        {/key}
                    </div>
                    <div class="min-max">
                        <span class="min-temp">Min: {Math.round(day.minTemperature)}°</span> /
                        <span class="max-temp">Max: {Math.round(day.maxTemperature)}°</span>
                    </div>
                    <div class="description">{day.description}</div>
                </div>
            </div>
        {/each}
    </div>
{:else}
    <div class="no-items">
        <p>Keine Wetterdaten verfügbar</p>
    </div>
{/if}

<style>
    .flip-carousel {
        display: flex;
        justify-content: center;
        align-items: center;
        perspective: 1200px;
        position: relative;
        height: 500px;
        margin-bottom: 30px;
        transform-style: preserve-3d;
        transition: transform 1s linear;
        overflow: visible;
        width: 100%;
        min-width: 1400px;
    }

    .card {
        width: 460px;
        height: 620px;
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        transition: transform 1s ease, opacity 0.6s ease, filter 0.6s ease;
        transform-style: preserve-3d;
        background: rgba(255, 255, 255, 0.95);
        color: black;
        border-radius: 16px;
        display: flex;
        justify-content: center;
        align-items: center;
        backdrop-filter: blur(5px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
        padding: 20px;
        cursor: pointer;
        border: 2px solid rgba(0, 0, 0, 0.2);
        user-select: none;
        z-index: 1;
    }

    .card.active {
        border-color: rgba(74, 144, 226, 0.6);
    }

    .card .content {
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;
        width: 100%;
        height: 100%;
        pointer-events: none;
    }

    .date {
        font-size: 3rem;
        font-weight: 700;
        margin-top: 20px;
        margin-bottom: 15px;
        color: #2c3e50;
    }

    .weather-icon.small {
        flex-grow: 1;
        transform: scale(2.5);
        margin-top: 165px;
        display: flex;
        justify-content: center;
        align-items: center;
    }

    .min-max {
        font-size: 2.8rem;
        font-weight: bold;
        margin-bottom: 24px;
        display: flex;
        gap: 5px;
        align-items: center;
        flex-wrap: wrap;
        justify-content: center;
    }

    .description {
        font-size: 2.8rem;
        font-weight: 500;
        opacity: 0.9;
        margin-bottom: 10px;
        text-transform: capitalize;
        text-align: center;
        line-height: 1.2;
        max-width: 100%;
        overflow-wrap: break-word;
    }

    .max-temp {
        color: #ff9900;
    }

    .min-temp {
        color: #007bff;
    }

    .no-items {
        display: flex;
        justify-content: center;
        align-items: center;
        height: 200px;
        color: rgba(255, 255, 255, 0.8);
        font-size: 1.2rem;
        background: rgba(255, 255, 255, 0.1);
        border-radius: 12px;
        margin: 20px 0;
    }

    @media (max-width: 768px) {
        .flip-carousel {
            height: 400px;
            min-width: 1000px;
        }

        .card {
            width: 320px;
            height: 450px;
            padding: 15px;
        }

        .date {
            font-size: 2.2rem;
            margin-top: 15px;
            margin-bottom: 10px;
        }

        .weather-icon.small {
            transform: scale(2);
            margin-top: 120px;
        }

        .min-max {
            font-size: 2rem;
            margin-bottom: 18px;
        }

        .description {
            font-size: 2rem;
            margin-bottom: 8px;
        }
    }

    @media (max-width: 480px) {
        .flip-carousel {
            height: 350px;
            min-width: 800px;
        }

        .card {
            width: 280px;
            height: 380px;
            padding: 12px;
        }

        .date {
            font-size: 1.8rem;
            margin-top: 10px;
            margin-bottom: 8px;
        }

        .weather-icon.small {
            transform: scale(1.5);
            margin-top: 100px;
        }

        .min-max {
            font-size: 1.6rem;
            margin-bottom: 15px;
        }

        .description {
            font-size: 1.6rem;
            margin-bottom: 6px;
        }
    }

    @media (prefers-reduced-motion: reduce) {
        .card {
            transition: none;
        }

        .flip-carousel {
            transition: none;
        }
    }
</style>