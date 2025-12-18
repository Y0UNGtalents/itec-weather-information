<script>
  import Slider from "./Slider.svelte";

    export let mapUrl = "";
    

</script>

<main>
    
    <div class="contain">
        

        
        

        {#if mapUrl}
        <div class="map-container">
            <iframe
                title="Google Maps Stadtansicht"
                width="450px"
                height="500"
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
        height: 25em;
        margin-left: 2em;
        margin-top: 1em;
        color: white;
    }

    .contain{
        display: flex;
        flex-direction: row;
    }

    

    .map-container {
        margin-top: 20px;
        overflow: hidden;
        border-radius: 60px;
        margin-left: 10em;
        border: 1px solid rgba(255, 255, 255, 0.3);
    }

    iframe {
        display: block;
    }

</style>