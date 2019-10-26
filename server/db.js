var mysql = require('mysql');

var connection = mysql.createConnection({
    host:'54.167.123.220',
    user:'user1',
    password:'pw1234',
    database:'myDB'
});

connection.connect();

connection.query('TRUNCATE TABLE measuredData;', function(err,res){
    if(err) throw err;
    else console.log('DB all cleared!');
} );

module.exports = connection