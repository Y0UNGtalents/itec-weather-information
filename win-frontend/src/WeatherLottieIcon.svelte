<script>
    import { onMount, afterUpdate } from 'svelte';
    import lottie from 'lottie-web';

    export let condition;

    let container;
    let animation;

    const getLottieFile = (condition) => {
        const map = {
            rain: '/lottie/rain.json',
            snow: '/lottie/snow.json',
            cloudy: '/lottie/cloudSun.json',
            fog: '/lottie/fog.json',
            storm: '/lottie/rainStorm.json',
            clear: '/lottie/sun.json',
        };
        return map[condition] || '/lottie/default.json';
    };

    function loadAnimation() {
        if (animation) {
            animation.destroy(); // Alte Animation entfernen
        }
        animation = lottie.loadAnimation({
            container,
            renderer: 'svg',
            loop: true,
            autoplay: true,
            path: getLottieFile(condition),
        });
    }

    onMount(() => {
        loadAnimation();
    });

    afterUpdate(() => {
        loadAnimation(); // Reagiert auf geänderte `condition`
    });
</script>

<div bind:this={container} class="weather-lottie-icon"></div>

<style>
    .weather-lottie-icon {
        width: 120px;
        height: 120px;
        display: flex;
        justify-content: center;
        align-items: center;
    }
</style>
