<script>
	import { timeFormat } from 'd3-time-format';
    import * as d3 from 'd3';

	export let xScale;
	export let height;
	export let margin;

	$: xTicks = xScale.ticks(d3.timeHour.every(3));

	const timeFormatter = timeFormat("%H"); // Stunden nur
</script>

<g class="x-axis" transform="translate(0, {height - margin.bottom})">
	{#each xTicks as tick}
		<g class='tick' transform="translate({xScale(tick)}, 0)">
			<line x1={0} x2={0} y1={0} y2={6} stroke="hsla(212, 10%, 53%, 1)" />
			<text y={6} dy={9} fill="currentColor" text-anchor="middle" dominant-baseline="middle">
				{timeFormatter(tick)}
			</text>
		</g>
	{/each}
</g>
