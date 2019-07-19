
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
            
            var spl = response.split(/\s+/);

            for(a = 1; a < spl.length; a++) {
                $('#result').append("<a href='"+spl[0]+"/"+spl[a]+"'>"+spl[a]+"</a><br>");
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

