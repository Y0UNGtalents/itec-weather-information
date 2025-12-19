<script>
    import * as d3 from "d3";
    import AxisY from './AxisY.svelte';
    import AxisX from './AxisX.svelte';

    // Props vom Hourly-Chart
    export let dayTemperatures = [];

    const width = 1350;
    const height = 480;
    const margin = { top: 20, right: 10, bottom: 50, left: 10 };

    // Map dayTemperatures auf das Format für D3
    $: data = dayTemperatures.map(d => ({
        time: new Date(d.time),   // forecastDate von Backend
        temp: d.temp              // Temperatur
    }));

    $: minTemp = d3.min(data, d => d.temp) ?? 0;
    $: maxTemp = d3.max(data, d => d.temp) ?? 0;


    // X-Scale (Zeitachse)
    $: xScale = d3.scaleUtc()
        .domain(d3.extent(data, d => d.time))
        .range([margin.left, width - margin.right]);

    // Y-Scale (Temperatur)
    $: yScale = d3.scaleLinear()
        .domain([minTemp, maxTemp])  // <-- Startwert = minTemp
        .nice()
        .range([height - margin.bottom, margin.top]);


    // Linie generieren
    const line = d3.line()
        .x(d => xScale(d.time))
        .y(d => yScale(d.temp))
        .curve(d3.curveMonotoneX);
</script>

<svg
    {width}
    {height}
    viewBox={`0 0 ${width} ${height}`}
    style="max-width: 100%; height: 100%;"
>
    <!-- Achsen -->
    <AxisY {yScale} {width} {margin} />
    <AxisX {xScale} {height} {margin} />

    <!-- Linie -->
    <g class="line-group">
        <path
            d={line(data)}
            fill="none"
            stroke="white"
            stroke-width="5"
        />
    </g>
</svg>

<style>
    .line-group path {
        stroke-linecap: round;
        stroke-linejoin: round;
    }
</style>
