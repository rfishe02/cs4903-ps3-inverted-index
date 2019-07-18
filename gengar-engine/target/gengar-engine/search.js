
// https://blog.teamtreehouse.com/create-ajax-contact-form
// https://www.journaldev.com/4742/jquery-ajax-jsp-servlet-java-example

$(function() {
    
    var form = $('#ajax-search');
    var formMessages = $('#test');
    
    $(form).submit(function(event) {
        event.preventDefault(); // Stop the browser from submitting the form.

        $.ajax({
            type: form.attr("method"),
            url: form.attr("action"),
            data: form.serialize()
        }).done(function(response) {

            $(formMessages).text(response);
            $('#search-box').val('');
            
        }).fail(function(data) {
            
            if (data.responseText !== '') {
                $(formMessages).text(data.responseText);
            } else {
                $(formMessages).text('Oops! An error occured and your query could not be sent.');
            }
            
        }); // Submit the form using AJAX.
        
    }); // Set up an event listener for the contact form.

});

