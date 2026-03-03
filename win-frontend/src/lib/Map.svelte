<script>
    let { mapUrl = "", cityName = "" } = $props();
</script>

<div class="container">
    {#if mapUrl}
        <div class="map-container">
            <iframe
                title="Google Maps Stadtansicht"
                src={mapUrl}
                style="border:0;"
                loading="lazy"
                allowfullscreen
            ></iframe>
            {#if cityName}
                <div class="city-overlay">{cityName}</div>
            {/if}
        </div>
    {/if}
</div>

<style>
    .container {
        display: flex;
        flex-direction: row;
    }

    .map-container {
        width: 480px;
        height: 400px;
        overflow: hidden;
        border-radius: var(--glass-border-radius);
        border: var(--glass-border);
        position: relative;
    }

    iframe {
        display: block;
        position: absolute;
        top: -50px;
        left: 0;
        width: 500px;
        height: 500px;
    }

    .city-overlay {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        height: 52px;
        background: rgba(255, 255, 255, 0.15);
        backdrop-filter: blur(200px);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 1.4rem;
        font-weight: 700;
        color: black;
        text-shadow: 0 1px 4px rgba(0, 0, 0, 0.4);
        border-radius: var(--glass-border-radius) var(--glass-border-radius) 0 0;
    }
</style>
