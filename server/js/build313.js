var degreeArray = new Array();

var coordinatesX = building.lat;
var coordinatesY = building.lon;

for (var i=0; i<24; i++){
    degreeArray.push(coordinatesY[i], coordinatesX[i], 30.0);
}
degreeArray.push(129.0828243, 35.235455, 30.0);

viewer.entities.add({
    // wall : {
    //     positions : Cesium.Cartesian3.fromDegreesArrayHeights(degreeArray),
    //     //height : 0,
    //     material : Cesium.Color.BLUE.withAlpha(0.1),
    //     outline : true,
    //     outlineColor : Cesium.Color.BLACK
    // }
    polygon : {
          hierarchy : Cesium.Cartesian3.fromDegreesArrayHeights(degreeArray),
          extrudedHeight: 0,
          perPositionHeight : true,
          material : Cesium.Color.BLUE.withAlpha(0.1),
          outline : true,
          outlineColor : Cesium.Color.BLACK
        }
});

viewer.zoomTo(viewer.entities);