# MaharaDroid Supported File Types #


The currently supported file types are content that has the following mime-type:

  1. `image/*`
  1. `video/*`
  1. `audio/*`
  1. `text/csv`
  1. `text/rtf`
  1. `text/html`
  1. `text/xml`

This could be extended by request - please ask in the Mahara forums or raise an issue on this site. The reason for restricting the file types is simple related to whether or not support has been tested.

## Advanced ##

Interested in testing some new mime-type? Grab the MaharaDroid source and build in the change. The file types supported by MaharaDroid are controlled by the Intent filter in the Android Manaifest XML file.
```
<intent-filter>
   <action android:name="android.intent.action.SEND" />
   <action android:name="android.intent.action.SEND_MULTIPLE" />
   <category android:name="android.intent.category.DEFAULT" />
   <data android:mimeType="image/*" />
   <data android:mimeType="video/*" />
   <data android:mimeType="audio/*" />			
   <data android:mimeType="application/*" />
   <data android:mimeType="text/csv" />
   <data android:mimeType="text/rtf" />
   <data android:mimeType="text/html" />
   <data android:mimeType="text/xml" />
 </intent-filter>
```