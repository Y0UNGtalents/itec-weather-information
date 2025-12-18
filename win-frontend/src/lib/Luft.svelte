<script>
    export let humidity; // Prop, die den Wert erhält
</script>
<main>


    <div class="details">
            <div class="detail-item">
                <table>
                    <thead class="table">
                        <tr>
                            <th><span class="label">Luftfeuchtigkeit:&nbsp;</span></th>
                            <th><span class="label">Luftdruck</span></th>
                            <th><span class="label">Windgeschwindigkeit </span></th>
                        </tr>
                    
                        <tr>
                            <td><span class="value">{humidity}%</span></td>
                            <td><span class="value"> 1013 Pha</span></td>
                            <td><span class="value"> 10 km/h</span></td>
                        </tr>
                    </thead>
                    
                </table>
                
                
            </div>
        </div>
</main>

<style>
    

    .details {
            background: rgba(255, 255, 255, 0.06);
            border-radius: 60px;
            box-shadow: 0 4px 30px rgba(0, 0, 0, 0.1);
            backdrop-filter: blur(5px);
            -webkit-backdrop-filter: blur(5px);
            border: 1px solid rgba(255, 255, 255, 0.3);
            padding-top: 14px;
            
            width: 90%;
            height: 12em;
            margin-left: 2.5em;
            
            
        }

        .detail-item {
            display: flex;
            text-align: center;
            justify-content: center;
            font-size: 2rem;
        }

        

        table{
            text-align: center;
            justify-content: center;
            font-size: 25px;
            color: white;
        }

        tr{
            height: 70px;
        }

        td{
            width: 250px;
        }

        .value{
            font-weight: bold;
            font-size: 1.8em;
        }

        
</style>
