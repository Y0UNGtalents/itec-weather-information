<script>
    import LineChart from './chart/LineChart.svelte';

    let { dayTemperatures = [] } = $props();
</script>

<div class="section-header">Stündliche Vorhersage</div>

<div class="hourly-forecast">
    <LineChart {dayTemperatures} />
</div>

<style>
    .hourly-forecast {
        background: rgba(255, 255, 255, 0.06);
        border-radius: 60px;
        box-shadow: 0 4px 30px rgba(0, 0, 0, 0.1);
        backdrop-filter: blur(5px);
        -webkit-backdrop-filter: blur(5px);
        border: 1px solid rgba(255, 255, 255, 0.3);
        width: 93%;
        height: 20em;
        margin-left: 2em;
        margin-top: 1em;
        color: white;
    }

    .section-header {
        font-size: 2rem;
        font-weight: 600;
        margin-left: 1em;
        margin-top: 2em;
        padding: 4px 16px;
        border-radius: 8px;
        color: white;
    }
</style>
