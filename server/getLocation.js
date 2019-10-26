var fs = require('fs');
var whichRoom = require('./whichRoom');

s_point = [129.082798, 35.235084]
d_point = [129.082613, 35.235043]

var dx = s_point[0] - d_point[0];
var dy = s_point[1] - d_point[1];
var d = dy / dx;

function get2FLat(lon){
  lat = d * (lon - d_point[0]) + d_point[1];
  return lat;
}


function getLocation(newData){

    fs.readFile("./data/location.json", (err, res) => {
        if (err) throw err;

        var prevLoc = JSON.parse(res);
        var data = new Object();
        
        if ( newData == 0 ){ //422
            // data.lat = 35.235350;
            // data.lon = 129.082742;
            data.lat = 35.235162;
            data.lon = 129.082742;
            data.direction = prevLoc.direction;
            data.floor = 4;

            fs.writeFile('./data/location.json', JSON.stringify(data), 'utf8', function(err){
                if (err)
                    console.log('write error: ' + err);
            });

        } else if (newData == 1){ //410
            data.lat = 35.235162;
            data.lon = 129.082742;
            data.direction = prevLoc.direction;
            data.floor = 4;

            fs.writeFile('./data/location.json', JSON.stringify(data), 'utf8', function(err){
                if (err)
                    console.log('write error: ' + err);
            });

        } else if ( newData >= 5 && newData <=  12 ){ //비콘 숫자면//
            fs.readFile('./data/beacon.json',(err,result)=>{
                if(err) throw err;

                beacon = JSON.parse(result).beacon;
                var index = newData - 5;
	
                if( index == 6 ){
                    data.floor = 2;
                } else if ( index == 7 ){
                    data.floor = 3;
                } else{
                    data.floor = 4;
                }

                if ( newData >= 8 ){
                    if(prevLoc.direction == 'N' && prevLoc.lat >= beacon[index].lat ){
                        //getLocation(prevLoc.direction);
                        console.log("blockN");
                        return;
                    }
                    else if (prevLoc.direction == 'S' && prevLoc.lat <= beacon[index].lat){
                        //getLocation(prevLoc.direction);
                        console.log("blockS");
                        return;
                    }
                }
                
   
                data.lat = beacon[index].lat;
                data.lon = beacon[index].lon;
                data.direction = prevLoc.direction;

		        console.log("\n>>nearest beacon ID :" + (index+5) +"\n>>User at section 9\n");

                fs.writeFile('./data/location.json', JSON.stringify(data), 'utf8', function(err){
                    if (err)
                        console.log('write error: ' + err);
                });
            });
        } else if(typeof(newData) == 'string') { //방향값이면//
            var room = whichRoom(new Array(prevLoc.lat, prevLoc.lon), prevLoc.floor);
            console.log('step room: '+room);
            if (room == 0){ //복도
                
                var offset = 0 ; 

                if( newData == 'S'){
                    offset  = - 0.00007;
                } else if( newData == 'N'){
                    offset = 0.000007;
                }
                
                data.lat = prevLoc.lat + offset;
                data.lon = prevLoc.lon;
                data.direction = newData;
                data.floor = prevLoc.floor;

                fs.writeFile('./data/location.json', JSON.stringify(data), 'utf8', function(err){
                    if (err)
                        console.log('write error: ' + err);
                });
            } else if (room == 100){ //2F
            
                var offset = 0.000008; 
                
                // data.lat = prevLoc.lat + offset;
                // data.lon = get2FLat(prevLoc.lat);
                data.lon = prevLoc.lon + offset;
                data.lat = get2FLat(prevLoc.lon);
                data.direction = newData;
                data.floor = prevLoc.floor;

                fs.writeFile('./data/location.json', JSON.stringify(data), 'utf8', function(err){
                    if (err)
                        console.log('write error: ' + err);
                });
            }
        }
    });
}

module.exports = getLocation;

getLocation(9);