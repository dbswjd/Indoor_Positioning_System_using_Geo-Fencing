var connection = require('./db');
var getLocation = require('./getLocation');

var findMAX = function(interval){
    var s_time = new Date();
    var e_time = new Date();
    // e_time.setSeconds(e_time.getSeconds() + 5);
    s_time.setSeconds(e_time.getSeconds() - interval);

    var sql = 'SELECT * FROM measuredData WHERE time BETWEEN ? AND ?';
    connection.query(sql, [s_time, e_time], function(err, result){
        if (err)
            throw err;
        else {
            //console.log(e_time.toString());
            console.log("select>> ", result.length, e_time.toString(), new Date());
            
            var avg = new Array(0, 0, 0, 0, 0, 0, 0, 0); //8
            var num = new Array(0, 0, 0, 0, 0, 0, 0, 0);
	     
            for (var i=0; i<result.length; i++){
                var id = result[i].beaconID - 5;
                avg[id] += result[i].RSSI;
                num[id]++;
            }

            for (var i=0; i<3; ++i){ //room check
                if (!num[i])
                    continue;
                avg[i] = avg[i] / num[i];
                if (avg[i] >= -65.0){
                    console.log("in room: ", avg[i]);
                    getLocation(i+5);
                    return;
                }	   
                else if (avg[i] >= -70.0){
                    console.log("near room: ", avg[i]);
                    getLocation(i);
                    return;
                }
                    
            }

            for (var i=5; i>=3; i--){ //corridor
                if (!num[i])
                   continue;
                avg[i] = avg[i] / num[i];
                if( avg[i] >= -65.0){ 
                    getLocation( i + 5 ) ;
                    return;
                }
            }

            for (var i=6; i< avg.length; i++){ //2F 3F
                if (!num[i])
                   continue;
                avg[i] = avg[i] / num[i];
                if( avg[i] >= -60.0){ 
                    getLocation( i + 5 ) ;
                    return;
                }
            }
        
        }
    });

}

module.exports = findMAX