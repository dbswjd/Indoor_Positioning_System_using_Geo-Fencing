var inside = require('point-in-polygon');
var fs = require('fs');

var context = fs.readFileSync('./data/real_313_4F.json', 'utf-8', function(err){
    if (err)
        console.log('read error: ' + error);
});
context = JSON.parse(context);

var polygons = new Array();
for (var c=0; c<30; c++){
    var exterior = context.cellSpaceMember[c].exterior;
    var polygon = new Array();
    for (var i=0; i<exterior.length; i++){
        var coordinates = new Array();
        coordinates.push(exterior[i][0]);
        coordinates.push(exterior[i][1]);
        polygon.push(coordinates);
    }
    polygons.push(polygon);
}

var whichRoom = function(point, floor){
    var where = -1;
    
    if (floor == 2){
        var check = inside(point, polygons[13]);
        if (check == true)
            return 13;
        return 100;
    } else if (floor == 3){
        return 13;
    } 
    
    //floor == 4
    for (var c=0; c<30; c++){
        var check = inside(point, polygons[c]);
        if (check == true){
            where = c;
            return where;
        }
    }

    return where;
}

module.exports = whichRoom