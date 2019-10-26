$(document).ready(function() {
    $.getJSON('/data/313.json', function(data){
        var building = data;
    });
    
    $.getJSON('/data/313-4F.json', function(data){
        var jsonData_313_4F = data;
    });
    
    // $('#target').click(function() {
    //   $.getJSON('../data/ex1.json', function(data) {
    //     var html = '';
    //     $.each(data, function(entryIndex, entry) {
    //         html += '<div class="entry">';
    //         html += '<h3 class="term">' + entry.term + '</h3>';
    //         html += '<div class="part">' + entry.part + '</div>';
    //         html += '<div class="definition">';
    //         html += entry.definition;
    //         html += '</div>';
    //         html += '</div>';
    //     });
    //     console.log(html);
    //     $('#dictionary').html(html);
    //   });
    //   return false;
    // });
});