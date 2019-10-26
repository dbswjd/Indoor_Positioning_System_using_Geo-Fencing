var http = require('http');
var socketio = require('socket.io');
var connection = require('./db');
var findMAX = require('./findMAX');
var getLocation = require('./getLocation');
var fs = require('fs');
var whichRoom = require('./whichRoom');

var server = http.createServer(function(req, res){
     
}).listen(3335, function(){
    console.log('Server for socket is running on port 3335... ');
});

var io = socketio.listen(server);
io.sockets.on('connection',function(socket){
    console.log('socket connected');
    socket.on('beaconData',function(res){

        var parsedQuery = JSON.parse(res);
        var data = parsedQuery.data;

        if (data){
            if( data.length > 0 ){
                var sql = 'INSERT INTO measuredData(beaconID,RSSI,time) VALUES (?,?,?)';
                // var date = new Date();
                console.log("i>>>", data.length,data[0].time.toString());
                // var parameter = [data[0].id, data[0].rssi, date];
                var parameter = [data[0].id, data[0].rssi, new Date(data[0].time)];

                for (var i=1; i<data.length; i++){
                    //console.log(data[i]);
                    sql += ', (?,?,?)';
                    parameter.push(data[i].id);
                    parameter.push(data[i].rssi);
                    time = new Date(data[i].time);
                    // parameter.push(date);
                    parameter.push(time);
                }

                connection.query(sql, parameter,function(err,res){
                    if (err) { throw err;}
                });

            }
        }
    });

    socket.on('stepData',function(res){
        var parsedQuery = JSON.parse(res);
        var movement = parsedQuery.movement;
        if(movement){
            console.log(">>> step data" + movement.direction);
            getLocation(movement.direction);        
        }
    });

    var prevRoom = null;
    setInterval(function(){
        var context = fs.readFileSync('./data/location.json', 'utf-8', function(err){
            if (err)
                console.log('read error: ' + error);
        });
        if(context){
            var location = JSON.parse(context);
            var room = whichRoom(new Array(location.lat, location.lon), location.floor) + 1; 
            room = "C" + room.toString() + "_4F";
            
            if(prevRoom != room){
                var unable = fs.readFileSync('./data/unable.json', 'utf-8', function(err){
                    if (err)
                        console.log('read error: ' + error);
                });

                unableRooms = JSON.parse(unable);

                for(var i =0; i < unableRooms.unable.length; i++ ){
                    if(unableRooms.unable[i] == room){
                        socket.emit('alert',room);
                        console.log(">>> room number : " + room.toString() );
                    }
                }

                prevRoom = room;
         }

        }
     }, 3000);

});



module.exports = server;

