<script>
    import lottie from 'lottie-web';

    let { condition } = $props();

    let container = $state(null);
    let animation = null;
    let currentCondition = '';

    function getLottieFile(weatherCondition) {
        const conditionToFile = {
            rain: '/lottie/rain.json',
            snow: '/lottie/snow.json',
            cloud: '/lottie/cloud.json',
            fog: '/lottie/fog.json',
            storm: '/lottie/storm.json',
            clear: '/lottie/sun.json',
            lightrain: '/lottie/lightrain.json',
            lightcloud: '/lottie/lightcloud.json',
            midcloud: '/lottie/lightcloud.json'
        };
        return conditionToFile[weatherCondition] || '/lottie/default.json';
    }

    function loadAnimation() {
        if (!container || !condition || currentCondition === condition) {
            return;
        }

        if (animation) {
            animation.destroy();
            animation = null;
        }

        try {
            animation = lottie.loadAnimation({
                container,
                renderer: 'svg',
                loop: true,
                autoplay: true,
                path: getLottieFile(condition),
            });
            currentCondition = condition;
        } catch (err) {
            console.error('Lottie error:', err);
        }
    }

    $effect(() => {
        if (container && condition && currentCondition !== condition) {
            const timeout = setTimeout(() => {
                loadAnimation();
            }, 100);

            return () => {
                clearTimeout(timeout);
                if (animation) {
                    animation.destroy();
                    animation = null;
                }
            };
        }
    });
</script>

<div bind:this={container} class="weather-lottie-icon"></div>

<style>
    .weather-lottie-icon {
        width: 200px;
        height: 200px;
        display: flex;
        justify-content: center;
        align-items: center;
    }
</style>
