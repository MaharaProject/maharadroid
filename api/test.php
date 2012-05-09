<html>
<body onload="javascript: document.forms[0].lastsync.value=Math.round(+new Date() / 1000) - 12*60*60;">
<form action="sync.php" method="post">
Username: <input name="username" value="" /><br />
Token: <input name="token" value="" /><br />
Last sync: <input name="lastsync" value="" /><br />
<input type="submit" value="Submit" />
<input type="reset" value="Reset" />
</form>
</body>
</html>
