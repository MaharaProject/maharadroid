## Introduction ##

&lt;wiki:gadget url="http://www.ohloh.net/p/486236/widgets/project\_users\_logo.xml" height="43" border="0"/&gt;

MaharaDroid enables Android phones to _share_ or upload content to a [Mahara](http://www.mahara.org) instance. Note: requires Mahara >= 1.4.

  * Configure the Mahara site you want to post to (must have mobileuploads enabled)
  * Select a image and 'Share' to MaharaDroid
  * MaharaDroid enable by users using (auto renewing) token based authentication
  * Log-on to Mahara web-site and further manage your uploaded artefact
  * QR code configurable

| ![https://lh5.googleusercontent.com/_ZQm5YtCKEqM/TdH6PW3GW-I/AAAAAAAAALE/91R4FIew17E/s400/n1.png](https://lh5.googleusercontent.com/_ZQm5YtCKEqM/TdH6PW3GW-I/AAAAAAAAALE/91R4FIew17E/s400/n1.png) | ![https://lh4.googleusercontent.com/_ZQm5YtCKEqM/TdH6PTaZ-4I/AAAAAAAAALA/5kOHaRG2KbU/s400/n2.png](https://lh4.googleusercontent.com/_ZQm5YtCKEqM/TdH6PTaZ-4I/AAAAAAAAALA/5kOHaRG2KbU/s400/n2.png) |
|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

## Source Code ##

Since Mahara 1.4 MaharaDroid has become a recognized Mahara contribution. Consequently the source has moved to gitorious

Browse the repository http://www.gitorious.org/mahara-contrib/mahara-droid or check out the code:

` git co http://git.gitorious.org/mahara-contrib/mahara-droid.git `

## Changelog ##

1.8
  * French language support (thanks Pascale Hyboud-Peron).
  * Japanese language support (thanks Mitsuhiro Yoshida).
  * Fixes for issues 16, 17 and 18.

1.7
  * Ability to 'save' artefacts for later upload. Including ability to delete all / upload all and view a saved artefact on the device
  * adjusted supported for additional content-types (removed text/plain as text v file is not easily distinguished)
  * improved language support (see credits below for details)
  * also includes some bug fixes ([issue 15](https://code.google.com/p/maharadroid/issues/detail?id=15))
  * QR configuration added

1.6 Note: compatibility breaker - now requires Android 1.6 (API level 4).
  * multiple file upload support
  * changed the back-end response handling to JSON as opposed to HTTP header responses to improve error handling. Requires matching Mahara functionality (see commit http://www.gitorious.org/mahara/mahara/commit/3023307c951628e8f2b9ecbf3c5ff48f0b53d646)
  * improved language support (see credits below for details)
  * added specific supported for additional content-types (beyond images, having reduced the scope after some reports of problems)
  * also includes some bug fixes ([issue 8](https://code.google.com/p/maharadroid/issues/detail?id=8), [issue 12](https://code.google.com/p/maharadroid/issues/detail?id=12), [issue 13](https://code.google.com/p/maharadroid/issues/detail?id=13) and [issue 14](https://code.google.com/p/maharadroid/issues/detail?id=14))

1.5 Fixed bug crashing the app when no preferences were set. Reduced upload capability to images only as Mahara doesn't support mobile video formats as yet. More forum discussion required to scope this further.

1.4 Enabled upload of 'any' file

1.3 Added support of global and per item artefact tags and content description - disabled stripping of file extension on upload

1.2 First release including title name and image support

1.1 Proof of concept - basic token authentication and content upload

## Howto ##

See <a href='http://www.youtube.com/watch?feature=player_embedded&v=D2lPwH4HWYA' target='_blank'><img src='http://img.youtube.com/vi/D2lPwH4HWYA/0.jpg' width='425' height=344 /></a>

Currently the 'token' based authentication is performed by a user setting a token in their settings on Mahara. This same token is then entered into the MaharaDroid preferences. Note that the token is re-keyed automatically for each upload. If you have any issues / problems .. simple reset the token both on Mahara and in MaharaDroid.

If there is no option to set your 'mobile upload' token then mobile uploads have not been enabled for your Mahara instance. Mobile uploads are enabled by a Mahara site administrator under system setting in versions >= 1.4. For demonstration purposes see http://master.dev.mahara.org.

### Wishlist ###

  1. Support for journals
  1. Support for auto-creating a public view perhaps (a-la http://master.dev.mahara.org/view/view.php?id=428).
  1. Support for OAuth and future Mahara web API

See 'issues' tab for more.

## Screenshots ##

| ![http://lh4.ggpht.com/_ZQm5YtCKEqM/TMZCX1DeRSI/AAAAAAAAAEY/uLWsCMT0Lks/s400/m2.png](http://lh4.ggpht.com/_ZQm5YtCKEqM/TMZCX1DeRSI/AAAAAAAAAEY/uLWsCMT0Lks/s400/m2.png) | ![http://lh3.ggpht.com/_ZQm5YtCKEqM/TK5_6_StmFI/AAAAAAAAADE/6T48sdfidFU/s400/m3.png](http://lh3.ggpht.com/_ZQm5YtCKEqM/TK5_6_StmFI/AAAAAAAAADE/6T48sdfidFU/s400/m3.png) |
|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

![http://lh5.ggpht.com/_ZQm5YtCKEqM/TK5_7WfpLjI/AAAAAAAAADM/NSPNi2z58rA/s800/m7.png](http://lh5.ggpht.com/_ZQm5YtCKEqM/TK5_7WfpLjI/AAAAAAAAADM/NSPNi2z58rA/s800/m7.png)

## Credits ##

The base concept of MaharaDroid, as an artefact uploader came from  [Flickr Free](http://code.google.com/p/flickrfree/) written by [Russel Stewart](mailto://rnstewart@gmail.com). Thanks to Russel for giving us the nod to start by using his FlickrFree upload code.

### Translations ###

| **Translator** | **Language** | **Version** |
|:---------------|:-------------|:------------|
| Kristina Hoeppner | German | 1.6 |
| Gregor Anželj | Slovenian | 1.6 |
| Inaki Arenaza | Spanish | 1.6 |
| Joan Queralt | Catalan | 1.7 |
| Mitsuhiro Yoshida | Japanese | 1.8 |
| Pascale Hyboud-Peron | French | 1.8 |