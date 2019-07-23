
// https://blog.teamtreehouse.com/create-ajax-contact-form
// https://www.journaldev.com/4742/jquery-ajax-jsp-servlet-java-example

$(function() {
    
    var form = $('#ajax-search');
    
    $(form).submit(function(event) {
        event.preventDefault(); // Stop the browser from submitting the form.

        var data = form.serialize();
        
        $('#search-box').val('');
        $('#result').empty();
        $('#result').append("<div div class='row justify-content-center align-items-center mt-4 h-100'><div class='col-12 mb-4 ' style='padding-top: 150px;'><p class='display-2 text-muted text-center'>...</p></div></div>");
        
        $.ajax({
            type: form.attr("method"),
            url: form.attr("action"),
            data: data
        }).done(function(response) {
            
            $('#result').empty();
            var links = response.split(/\s+/);
            
            if(links.length > 1) {
                
                $('#result').append("<div div class='row justify-content-start align-items-center mt-4 h-100' id ='result-row'>");
             
                for(a = 1; a < links.length; a++) {
                    
                    var text = links[a].split(",");
                    
                    var out = "";
                    for(b = 3; b < text.length; b++) {
                        out += text[b]+" ";
                    }
                    
                    if(out.length > 0) {
                        $('#result-row').append("<div class='col-12 mb-4'><div class='card h-100 shadow-sm'><div class='card-body'><h5 class='card-title mb-3'>Document #"+text[1]+"</h5><h6 class='card-subtitle text-muted mb-4'>"+text[2]+"</h6><p class='card-text mb-4'>"+out+"</p><a href='"+links[0]+"/"+text[0]+"' class='card-link'>"+text[0]+"</a></div></div></div>");
                    }
   
                }
                
                $('#result-row').append("</div>");
                
            } else {
                $('#result').append("<div div class='row justify-content-center align-items-center mt-4 h-100'><div class='col-12 mb-4 ' style='padding-top: 150px;'><p class='display-2 text-muted text-center'>"+response+"</p></div></div>");  
            }
       
        }).fail(function(data) {
            
            if (data.responseText !== '') {
                alert(data.responseText);
            } else {
                alert('Oops! An error occured and your query could not be sent.');
            }
            
        }); // Submit the form using AJAX.
        
    }); // Set up an event listener for the contact form.

});

