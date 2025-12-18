<script>
    export let mapUrl = "";
    

</script>

<main>
    <div class="next-days-header">Tägliche Vorhersage</div>
    <div class="contain">
        <div class="hourly-forecast"></div>

        {#if mapUrl}
        <div class="map-container">
            <iframe
                title="Google Maps Stadtansicht"
                width="450px"
                height="320"
                src={mapUrl}
                style="border:0;"
                loading="lazy"
                allowfullscreen
            />
        </div>
        {/if}
    </div>

</main>

<style>
    .hourly-forecast{
        background: rgba(255, 255, 255, 0.06);
        border-radius: 60px;
        box-shadow: 0 4px 30px rgba(0, 0, 0, 0.1);
        backdrop-filter: blur(5px);
        -webkit-backdrop-filter: blur(5px);
        border: 1px solid rgba(255, 255, 255, 0.3);
        width: 45%;
        height: 20em;
        margin-left: 2em;
        margin-top: 1em;
        color: white;
    }

    .contain{
        display: flex;
        flex-direction: row;
    }

    .next-days-header {
        font-size: 2rem;
        font-weight: 600;
        margin-left: 1em;
        margin-top: 20px;
        
        padding: 4px 16px;
        border-radius: 8px;
        color: white;
        
            
    }

    .map-container {
        margin-top: 20px;
        overflow: hidden;
        border-radius: 60px;
        margin-left: 2em;
    }

    iframe {
        display: block;
    }

</style>