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
        background: var(--glass-bg);
        border-radius: var(--glass-border-radius);
        box-shadow: var(--glass-shadow);
        backdrop-filter: var(--glass-blur);
        -webkit-backdrop-filter: var(--glass-blur);
        border: var(--glass-border);
        width: var(--block-width);
        height: 20em;
        margin-left: var(--block-margin-left);
        margin-top: 1em;
        color: white;
    }

    .section-header {
        font-size: var(--section-header-font-size);
        font-weight: 600;
        margin-left: 1em;
        margin-top: 2em;
        padding: 4px 16px;
        border-radius: 8px;
        color: white;
    }
</style>
