<?php

    if ($_SERVER["REQUEST_METHOD"] == "POST") {
        
        // Get the form fields and remove whitespace.
        //$name = strip_tags(trim($_POST["name"]));
        //$name = str_replace(array("\r","\n"),array(" "," "),$name);
        //$email = filter_var(trim($_POST["email"]), FILTER_SANITIZE_EMAIL);
        
        $text = trim($_POST["search-box"]);

        if ( empty($text) ) {
            http_response_code(400); // Set a 400 (bad request) response code and exit.
            echo "Oops! There was a problem with your submission.";
            exit;
        } // Check that data was sent to the engine.

        //fopen('./output/dict.raf', 'r');
        
        echo "Success!";
       
        // Send the search.
        /*
        if (  ) {
            http_response_code(200); // Set a 200 (okay) response code.
            echo "Thank You! Your message has been sent.";
        } else {
            http_response_code(500); // Set a 500 (internal server error) response code.
            echo "Oops! Something went wrong and we couldn't send your message.";
        }*/

    } else {
        http_response_code(403); // Not a POST request, set a 403 (forbidden) response code.
        echo "Oops! There was a problem with your submission.";
    }

?>
