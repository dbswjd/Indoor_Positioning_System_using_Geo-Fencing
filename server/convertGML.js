var fs = require('fs');
var real = require('./gmlToReal');

var indoorGML = fs.readFileSync('./data/313-4F-3D-190612.gml', 'utf-8', function(err){
    if (err)
        console.log('read error: ' + error);
});
var DOMParser = require('xmldom').DOMParser;
var parser = new DOMParser();
var xmlDoc = parser.parseFromString(indoorGML,"test/xml");

var roomArray = new Array();
var cellSpaceMembers = xmlDoc.getElementsByTagName("core:cellSpaceMember");
for (var cellSpaceMember=0; cellSpaceMember<cellSpaceMembers.length; cellSpaceMember++){
    var roomInfo = new Object();
    //name
    var name = cellSpaceMembers[cellSpaceMember].getElementsByTagName("gml:name");
    roomInfo.name = name[0].textContent;

    //exterior
    var LinearRings = cellSpaceMembers[cellSpaceMember].getElementsByTagName("gml:LinearRing");
    var wallArray = new Array();
        var positions = LinearRings[0].getElementsByTagName("gml:pos");
        for (var pos=0; pos<positions.length; pos++){
            var obj = positions[pos].textContent.toString().split(' ');
            obj[1] = real.convertX(obj[1]);
            obj[0] = real.convertY(obj[0]);
            wallArray.push(obj);
        }
    roomInfo.exterior = wallArray;

    //boundary
    var doorArray = new Array();
    var partialboundedBy = cellSpaceMembers[cellSpaceMember].getElementsByTagName("core:partialboundedBy");
    for (var i=0; i<partialboundedBy.length; i++){
        var door = partialboundedBy[i].toString().split('"');
        door = door[3].replace('#', '');
        doorArray.push(door);
    }
    roomInfo.partialboundedBy = doorArray;
    
    roomArray.push(roomInfo);
}

//boundaries
var BoundaryArray = new Array();
var cellSpaceBoundaryMember = xmlDoc.getElementsByTagName('core:cellSpaceBoundaryMember');
for (var boundaryIndex = 0 ; boundaryIndex < cellSpaceBoundaryMember.length; boundaryIndex++){
    var BoundaryInfo = new Object();

    var name = cellSpaceBoundaryMember[boundaryIndex].getElementsByTagName('gml:name')[0].textContent;
    BoundaryInfo.name = name;

    var position = cellSpaceBoundaryMember[boundaryIndex].getElementsByTagName('gml:pos');
    var positionInfo = new Array();
    for (var pos = 0; pos < position.length; pos++){
        var data = position[pos].textContent.trim().split(' ');
        positionInfo.push(data);
    }
    BoundaryInfo.position = positionInfo;
    BoundaryArray.push(BoundaryInfo);
}

//merge & write
var totalInfo = new Object();
totalInfo.cellSpaceMember = roomArray;
totalInfo.cellSpaceBoundary = BoundaryArray;
var jsonInfo = JSON.stringify(totalInfo);
fs.writeFile('./data/real_313_4F.json', jsonInfo, 'utf8', function(err){
    if (err)
        console.log('write error: ' + err);
    else 
        console.log('converted!')
});