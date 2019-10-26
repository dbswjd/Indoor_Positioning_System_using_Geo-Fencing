function convertX(x){
    return 129.082419 + x/1000000*1.36;
}

function convertY(y){
    return 35.23546 - y/1000000*1.665;
}

module.exports = {
    convertX: convertX,
    convertY: convertY
}