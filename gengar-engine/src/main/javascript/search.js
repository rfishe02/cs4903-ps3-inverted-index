
// https://blog.teamtreehouse.com/create-ajax-contact-form
// https://www.journaldev.com/4742/jquery-ajax-jsp-servlet-java-example

$(function() {
    
    var form = $('#ajax-search');
    
    $(form).submit(function(event) {
        event.preventDefault(); // Stop the browser from submitting the form.

        $.ajax({
            type: form.attr("method"),
            url: form.attr("action"),
            data: form.serialize()
        }).done(function(response) {
            
            $('#result').empty();
            
            //$('#result').append("<p>"+response+"</p><br>");
       
            var links = response.split(/\s+/);
            
            if(links.length > 1) {
             
                for(a = 1; a < links.length; a++) {
                    
                    var text = links[a].split(",");
                    $('#result').append("<a href='"+links[0]+"/"+text[0]+"'>"+text[0]+"</a><br>");
                    
                    var out = "";
                    for(b = 1; b < text.length; b++) {
                        out += text[b]+" ";
                    }
                    
                    if(text.length > 0) {
                        $('#result').append("<p>"+out+"</p><br>");   
                    }
                    
                }
                
            } else {
                $('#result').append("<p>"+response+"</p><br>");  
            }
            
            $('#search-box').val('');
            
        }).fail(function(data) {
            
            if (data.responseText !== '') {
                alert(data.responseText);
            } else {
                alert('Oops! An error occured and your query could not be sent.');
            }
            
        }); // Submit the form using AJAX.
        
    }); // Set up an event listener for the contact form.

});

