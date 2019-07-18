
// https://blog.teamtreehouse.com/create-ajax-contact-form

$(function() {
    
    var form = $('#ajax-search');
    var formMessages = $('#test');
    
    $(form).submit(function(event) {
        event.preventDefault(); // Stop the browser from submitting the form.
        var formData = $(form).serialize();
        
        $.ajax({
            type: 'POST',
            url: $(form).attr('action'),
            data: formData
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

