<script>
    import lottie from 'lottie-web';

    // Props
    let { condition } = $props();

    // State
    let container = $state(null);
    let animation = $state(null);
    let currentCondition = $state('');

    const getLottieFile = (condition) => {
        const map = {
            rain: '/lottie/rain.json',
            snow: '/lottie/snow.json',
            cloudy: '/lottie/cloudSun.json',
            fog: '/lottie/fog.json',
            storm: '/lottie/rainStorm.json',
            clear: '/lottie/sun.json',
            lightCloudy: '/lottie/lightCloudy.json',
        };
        return map[condition] || '/lottie/default.json';
    };

    function loadAnimation() {
        // Prevent multiple calls
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
        } catch (error) {
            console.error('Lottie error:', error);
        }
    }

    // Single effect with all logic
    $effect(() => {
        if (container && condition && currentCondition !== condition) {
            // Small delay to ensure DOM is ready
            const timeout = setTimeout(() => {
                loadAnimation();
            }, 100);

            // Cleanup
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
        width: 180px;
        height: 120px;
        display: flex;
        justify-content: center;
        align-items: center;
    }
</style>
