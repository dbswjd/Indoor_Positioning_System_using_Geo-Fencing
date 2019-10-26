var http = require('http');
var fs = require('fs');

var server = http.createServer(function(request,response){
    var postdata = '';
    
    request.on('data', function (data) {
        postdata = postdata + data;
    });

    request.on('end', function () {
        var parsedQuery = JSON.parse(postdata);

        var Initinfo = parsedQuery.Initinfo;
        if(Initinfo){
            console.log(Initinfo);
            fs.writeFile('./data/person_data.json', JSON.stringify(Initinfo), 'utf8', function(err){
                if (err)
                    console.log('write error: ' + err);
                else 
                    console.log('initial person info registered!');
            });

        }
        
        response.writeHead(200, {'Content-Type':'text/html'});
        response.write("registered");
        response.end();
    });
});

server.listen(3333, function(){
    console.log('Server is running on port 3333 ...');
});


module.exports = server
