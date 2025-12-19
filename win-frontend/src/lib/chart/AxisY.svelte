<script>
	export let yScale;
	export let width;
	export let margin;

	$: yTicks = yScale.ticks(8);
</script>

<g class="y-axis" transform="translate({margin.left}, 0)">
	<!-- Y-Axis Label -->
	<text 
		y={-10} 
		x={0} 
		fill="currentColor"
		dominant-baseline="middle" 
		text-anchor="start">
		°C
	</text>

	{#each yTicks as tick}
		<g class='tick' transform="translate(0, {yScale(tick)})">
			<line x1={0} x2={width} y1={0} y2={0} stroke="hsla(212, 10%, 53%, 0.3)" />
			<text x={-5} y={0} fill="currentColor" dominant-baseline="middle" text-anchor="end">
				{tick}
			</text>
		</g>
	{/each}
</g>
