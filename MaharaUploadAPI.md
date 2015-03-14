The MaharaDroid API is made up of an HTTP(s) POST (multi-part mime) with a JSON response object.


The HTTP(s) POST contains all the user, token and artefact details as well as the file. The POST is multi-part mime encoded as per a standard HTTP form submit. On receiving the POST request the API (in order):
  1. checks to see if site configuration 'allowmobileuploads' is enabled
  1. validates the user with their token
  1. checks to see if the upload directory exists (if not it is created)
  1. if a blog id is provided creates a blog post with the title and description
  1. if a blog post id is provided load the blog post
  1. if a file has been uploaded creates the artefact (with the details, user and folder provided)
    1. checks to see if users file quota has been exceeded (or responds with failure)
    1. checks to see if file has been saved (or responds with failure)
    1. if a blog post has been loaded/created attach the file to the blog post
  1. generates a new token and response with success

For more in depth detail you are encouraged to view the API in it's complete form (it's not that complex) ..it can be found on the Mahara gitorious site here /htdocs/api/mobile/upload.php

## Parameters POSTed ##
  1. token
  1. username
  1. foldername
  1. blog / journal id
  1. blog post / journal entry id
  1. title
  1. description
  1. userfile (uploaded file)
  1. tags

## Example Output ##
```
Content-Type: application/json

{"fail":"Auth token cannot be blank"}
```

### A note on HTTPS certificates ###

Testing with untrusted certificates (e.g. default apache2 ssl set-up) gives a "No peer certificate" error. This is because Android by default expects certificates to be trusted.

```
...
04-20 12:06:49.517: W/System.err(31973): javax.net.ssl.SSLPeerUnverifiedException: No peer certificate
04-20 12:06:49.527: W/System.err(31973): 	at org.apache.harmony.xnet.provider.jsse.SSLSessionImpl.getPeerCertificates(SSLSessionImpl.java:258)
```

Based on discussions on StackOverflow I have created a debug SSL treatment which overrides this default behavior and ignores SSL cert errors allowing developers to test with untrusted certificates.

Testing has not yet confirmed whether or not a trusted certificate does work. If you have HTTPS we a working it would be good to hear from you.

# Problems #

## Web-server / Php configuration ##

If you have problems uploading files check your web-server error log but also your php.ini file for max upload filesize. If this is too small your upload will quietly fail (on the server side).

For example - my default for Php of 2M is too small for modern smart-phones with 4+ mega-pixel cameras:
```

;;;;;;;;;;;;;;;;
; File Uploads ;
;;;;;;;;;;;;;;;;

; Whether to allow HTTP file uploads.
; http://php.net/file-uploads
file_uploads = On

; Temporary directory for HTTP uploaded files (will use system default if not
; specified).
; http://php.net/upload-tmp-dir
;upload_tmp_dir =

; Maximum allowed size for uploaded files.
; http://php.net/upload-max-filesize
upload_max_filesize = 20M

; Maximum number of files that can be uploaded via a single request
max_file_uploads = 20
```