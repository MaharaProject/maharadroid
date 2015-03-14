# QR Code Configuration #


It is possible to make a QR code for your Mahara site so that users can automatically configure MaharaDroid. This can be done in one of two ways.
  1. create a QR code of the XML configuration file
  1. create a QR code of the URL to the XML configuration file

https://mahara.org/artefact/file/download.php?file=164805&view=36871

# XML configuration file #
Common to both these method is the XML configuration file. MaharaDroid can read an XML configuration file and overwrite some it's default settings with those provided in the file. The configuration variables that can be overridden are upload variables such as:
  * upload.uri
  * upload.folder
  * upload.tags

So a XML configuration file may look like:

```
<config> 
 <upload.uri>http://master.dev.mahara.org/artefact/file/mobileupload.php</upload.uri> 
 <upload.folder>MobileUploads</upload.folder> 
 <upload.tags>Android</upload.tags> 
</config>
```

## Advanced ##

In fact it allows setting of any upload preferences based on key name. For example check the pref\_uload**key fields in the strings.xml file such as: ` <string name="pref_upload_url_key">upload.uri</string> ` https://www.gitorious.org/mahara-contrib/mahara-droid/blobs/master/res/values/strings.xml**

# Create a QR code of the XML configuration file #

To create a QR code of the XML configuration file use a tool to create a QR code from the contents of the XML configuration file.

`qrencode -O config.png < config.xml`

Or use an online tool such as http://qrcode.kaywa.com/

# Create a QR code of the URL to the XML configuration file #

By creating a QR code of the URL to the XML configuration file the QR code has a reference to the configuration and not the configuration itself. This enables you to dynamically change the configuration without changing the QR code (saved, cached and physical copies remain valid). That said, a data connection would be required for the user to download the XML configuration file.

To make a QR code containing a URL to the XML configuration file
> use a tool to create a QR code from the contents of the XML configuration file.

`qrencode -O config.png http://my.mahara.instance.com/config.xml`

Or use an online tool such as http://qrcode.kaywa.com/