The MaharaDroid sync API is made up of an HTTP(s) POST (multi-part mime) with a JSON response object.


The HTTP(s) POST contains the user, token and last-sync details. The POST is multi-part mime encoded as per a standard HTTP(s) form submit. On receiving the POST request the API (in order):
  1. checks to see if site configuration 'allowmobileuploads' is enabled
  1. validates the user with their token
  1. retrieves user details such as:
    1. blogs (Journals)
    1. blogposts (Journal posts)
    1. folders
    1. tags
    1. activity
    1. version (Mahara version)
  1. generates a new token and response with success and the server time (as lastsync as EPOCH)

For more in depth detail you are encouraged to view the API in it's complete form (it's not that complex) ..it can be found on the Mahara gitorious site here https://www.gitorious.org/mahara/mahara/blobs/master/htdocs/api/mobile/sync.php

There is also a test utility (web form) that cane be found here
https://www.gitorious.org/mahara/mahara/blobs/master/htdocs/api/mobile/test.php

## Parameters POSTed ##
  1. token
  1. username
  1. lastsync
  1. notifications

### Example raw HTTP Post ###
```
POST /~alanm/mahara/htdocs/api/mobile/sync.php HTTP/1.1
Content-Length: 760
Content-Type: multipart/form-data; boundary=K1X-SOmSHUE9niJ4MnTMlRluCgYej2YCKMb
Host: 10.0.0.103
Connection: Keep-Alive

--K1X-SOmSHUE9niJ4MnTMlRluCgYej2YCKMb
Content-Disposition: form-data; name="lastsync"
Content-Type: text/plain; charset=UTF-8
Content-Transfer-Encoding: 8bit

0
--K1X-SOmSHUE9niJ4MnTMlRluCgYej2YCKMb
Content-Disposition: form-data; name="notifications"
Content-Type: text/plain; charset=UTF-8
Content-Transfer-Encoding: 8bit

maharamessage,feedback,newpost,usermessage
--K1X-SOmSHUE9niJ4MnTMlRluCgYej2YCKMb
Content-Disposition: form-data; name="token"
Content-Type: text/plain; charset=UTF-8
Content-Transfer-Encoding: 8bit

foobar
--K1X-SOmSHUE9niJ4MnTMlRluCgYej2YCKMb
Content-Disposition: form-data; name="username"
Content-Type: text/plain; charset=UTF-8
Content-Transfer-Encoding: 8bit

alan
--K1X-SOmSHUE9niJ4MnTMlRluCgYej2YCKMb--
```

## Example Output ##
```
Content-Type: application/json

{
   "success":"909632858af6da5c73eb290dac63e02f",
   "lastsync":"1343241003",
   "version":"20120712",
   "sync":{
      "activity":[
         {
            "id":"1",
            "subject":"New friend request",
            "message":"Admin User has requested that you add them as a friend. You can either do this from the link below or from your friends list page. Their reason was:\n    Hey do you want to be my friend"
         },
         {
            "id":"2",
            "subject":"New message from Admin User (admin)",
            "message":"Test message"
         }
      ],
      "tags":[
         {
            "id":"Android",
            "tag":"Android"
         },
         {
            "id":"second",
            "tag":"second"
         },
         {
            "id":"third",
            "tag":"third"
         }
      ],
      "blogs":[
         {
            "id":"8",
            "blog":"Alan McNatty's Blog"
         }
      ],
      "blogposts":[
         {
            "id":"1",
            "blog":"2012-07-26: COMP trip to Auckland"
         }
      ],
      "folders":[
         {
            "id":189,
            "folder":"MobileUploads"
         }
      ]
   }
}
```