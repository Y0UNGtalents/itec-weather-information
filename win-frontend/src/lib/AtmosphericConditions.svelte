<script>
    let { humidity, pressure, windSpeed } = $props();
</script>

<div class="details">
    <div class="detail-item">
        <table>
            <thead>
                <tr>
                    <th><span class="label">Luftfeuchtigkeit:&nbsp;</span></th>
                    <th><span class="label">Luftdruck</span></th>
                    <th><span class="label">Windgeschwindigkeit</span></th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td><span class="value">{humidity}%</span></td>
                    <td><span class="value">{pressure}hPa</span></td>
                    <td><span class="value">{Math.round(windSpeed)}km/h</span></td>
                </tr>
            </tbody>
        </table>
    </div>
</div>

<style>
    .details {
        background: var(--glass-bg);
        border-radius: var(--glass-border-radius);
        box-shadow: var(--glass-shadow);
        backdrop-filter: var(--glass-blur);
        -webkit-backdrop-filter: var(--glass-blur);
        border: var(--glass-border);
        padding-top: 14px;
        width: var(--block-width);
        height: 12em;
        margin-left: var(--block-margin-left);
    }

    .detail-item {
        display: flex;
        text-align: center;
        justify-content: center;
        font-size: 2rem;
    }

    table {
        text-align: center;
        justify-content: center;
        font-size: 25px;
        color: white;
    }

    tr {
        height: 70px;
    }

    td {
        width: 250px;
    }

    .value {
        font-weight: bold;
        font-size: 1.8em;
    }
</style>
