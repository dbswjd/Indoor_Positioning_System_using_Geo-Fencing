var path = require('path')
var fs = require('fs')
var whichRoom = require('./whichRoom')
var express = require('express')
var bodyParser = require('body-parser')

var app = express();
app.use(express.static(path.join(__dirname, '/html')));
app.use(express.json());
app.use(bodyParser.urlencoded({
    extended: true
}));
app.use(bodyParser.json());

app.get('/', function (req, res) {
    res.sendFile(path.join(__dirname, '/html', 'index.html'));
    fs.writeFile('./data/unable.json', "{\"unable\":[]}", function(err){
        console.log('Init write error: ' + err);
    });
});


app.use('/data', express.static(path.join(__dirname, '/data')));
app.use('/glb', express.static(path.join(__dirname, '/glb')));
app.use('/js', express.static(path.join(__dirname, '/js')));

app.post('/where', function (req, res){
    var body = req.body;
    var point = new Array(body.lat, body.lon);
    var room = whichRoom(point, body.floor);
    res.set('Content-Type', 'text/plain');
    res.send(room.toString());
});

app.post('/enable', function (req, res){
    var body = req.body;
    var context = fs.readFileSync('./data/unable.json', 'utf-8', function(err){
        console.log('read error: ' + error);
    });
    context = JSON.parse(context);
    var arr = context.unable;
    if (body.unable == "true"){
        arr.push(body.name);
    } else {
        for (var i in arr.len){
            if (arr[i] == body.name){
                arr.splice(i, i);
            }
        }
    }
    context.unable = arr;
    fs.writeFile('./data/unable.json', JSON.stringify(context), function(err){
        console.log('write error: ' + err);
    });

    res.send("complete");
});

app.post('/exist', function (req, res){
    var body = req.body;
    var context = fs.readFileSync('./data/unable.json', 'utf-8', function(err){
        console.log('read error: ' + error);
    });

    context = JSON.parse(context);
    var arr = context.unable;
    var exist = false;
    for (var i=0; i<arr.length; ++i){
        if (arr[i] == body.name){
            exist = true;
            break;
        }
    }

    res.send(exist);
});

app.use('/', function (req, res) {
    res.send('Hello Cesium');
});

app.listen(3334, function () {
    console.log('Server for Cesium is running on port 3334 ...');
});

module.exports = app

