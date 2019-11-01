function Redux-Undeploy{
	
	write-host "[INFO]Undeploying old redux..."
	stop-service node_redux
	Remove-Item "D:\Node_Apps\redux\*" -Recurse
    Remove-Item "D:\Node_Apps\redux.zip"

}

function Redux-Deploy{

	write-host "[INFO]Unzip"

	$shell = new-object -com shell.application
	$zip = $shell.NameSpace("D:\Node_Apps\redux.zip")
	foreach($item in $zip.items())
	{
		write-host "[info] unzipping item..."
		$shell.Namespace("D:\Node_Apps\redux").copyhere($item,1564)
	}

    start-service node_redux  
}

function commonLogin-Undeploy{
	
	write-host "[INFO]Undeploying old redux..."
	stop-service node_redux
	Remove-Item "D:\Node_Apps\commonLogin*" -Recurse
    Remove-Item "D:\Node_Apps\commonLogin.zip"

}

function commonLogin-Deploy{

	write-host "[INFO]Unzip"

	$shell = new-object -com shell.application
	$zip = $shell.NameSpace("D:\Node_Apps\commonLogin.zip")
	foreach($item in $zip.items())
	{
		write-host "[info] unzipping item..."
		$shell.Namespace("D:\Node_Apps\commonLogin").copyhere($item,1564)
	}

    start-service node_redux  
}

function commonLoginClient-Undeploy{
	
	write-host "[INFO]Undeploying old redux..."
	stop-service node_redux
	Remove-Item "D:\Node_Apps\commonLoginClient*" -Recurse
    Remove-Item "D:\Node_Apps\commonLoginClient.zip"

}

function commonLoginClient-Deploy{

	write-host "[INFO]Unzip"

	$shell = new-object -com shell.application
	$zip = $shell.NameSpace("D:\Node_Apps\commonLoginClient.zip")
	foreach($item in $zip.items())
	{
		write-host "[info] unzipping item..."
		$shell.Namespace("D:\Node_Apps\commonLoginClient").copyhere($item,1564)
	}

    start-service node_redux  
}