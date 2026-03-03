<script>
    import Map from "./Map.svelte";
    import Slider from "./Slider.svelte";

    let { mapUrl = "", cityName = "", items = [] } = $props();
</script>

<div class="map-slider-row">
    <div class="panel">
        <Slider {items} />
    </div>
    <div class="panel">
        <Map {mapUrl} {cityName} />
    </div>
</div>

<style>
    .map-slider-row {
        display: flex;
        flex-direction: row;
        justify-content: space-around;
        align-items: center;
        margin-left: var(--block-margin-left);
        width: var(--block-width);
        margin-top: 1em;
    }

    .panel {
        display: flex;
        justify-content: center;
        align-items: center;
    }
</style>
