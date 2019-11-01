##############################################################################
##
## Deploy-Modules.ps1
##
##############################################################################

<#

.SYNOPSIS

.EXAMPLE


#>

param(
    [Parameter (Mandatory = $true)] 
    [String] $env,

    [Parameter (Mandatory = $true)] 
    [String[]] $server,

    [Parameter (Mandatory = $true)] 
    [String[]] $appList,

    [Parameter (Mandatory = $true)] 
    [String] $sourceDir,

    [Parameter (Mandatory = $true)] 
    [String] $release,

    [Parameter (Mandatory = $true)] 
    [String] $buildName,

    [Parameter (Mandatory = $false)] 
    [String] $retryCount = 1  
)

if(!$PSVersionTable.PSVersion -lt 3){$PSScriptRoot = Split-Path $MyInvocation.MyCommand.Path -Parent}

. "$PSScriptRoot/utils/LogUtils.ps1" 
. "$PSScriptRoot/utils/CommonUtils.ps1" 
. "$PSScriptRoot/remote/SpringUtils.ps1" 
. "$PSScriptRoot/remote/RemoteUtils.ps1" 
. "$PSScriptRoot/remote/Extract-ErrorLogs.ps1" 
. "$PSScriptRoot/remote/Delete-OldArchives.ps1" 
. "$PSScriptRoot/ConfigUtils.ps1" 

function Deploy-SpringModule ($session, $serverConfig, $appConfig, $sourceDir){

    $status = $false

    $GF_ASADMIN = $serverConfig.gfAsadmin
    $GF_DIR = $serverConfig.gfDir
    $GF_ADMIN = $serverConfig.gfUsername
    $GF_PASS = $serverConfig.gfPassword
    $GF_SERVICE = $serverConfig.gfService

    $appName = $appConfig.appName
    $modName = $appConfig.modName
    $fileName = $appConfig.fileName
    $targetDir = $appConfig.targetDir
    $serviceName = $appConfig.serviceName
    $destFileName = if($appConfig.destFileName){$appConfig.destFileName}else{$appConfig.fileName}
    $check_url = $appConfig.url
    $deploymentOrder = $appConfig.deploymentOrder
	
	if($appConfig.additionalFile){
		$additionalFile = $appConfig.additionalFile
		$additionalTarget = $appConfig.additionalTarget
		$additionalSourcePath = "$sourceDir/$additionalFile"
		$additionalTargetPath = "$additionalTarget/$additionalFile"
	}

    $targetPath = "$targetDir/$destFileName"
    $sourcePath = "$sourceDir/$fileName"

    Remote-CreateDir $session $targetDir

    ReportInfo "Stopping service..."
    Run-Remote -session $session -ExitOnRemoteFail $false -returnExitCode $true -script ${function:Stop-Service} -arguments $serviceName
    
    ReportInfo "Removing old file..."
    Run-Remote -session $session -ExitOnRemoteFail $false -returnExitCode $true -script {param($targetPath) remove-item $targetPath} -arguments $targetPath


    ReportInfo "Sending application war..."
    ReportInfo "Source: $sourcePath => Dest: $targetPath"
    . "$PSScriptRoot/utils/Send-File.ps1" $sourcePath $targetPath $session

	if($appConfig.additionalFile){
		ReportInfo "Source: $additionalSourcePath => Dest: $additionalTargetPath"
		. "$PSScriptRoot/utils/Send-File.ps1" $additionalSourcePath $additionalTargetPath $session
	}
	
    $deploymentStart = Remote-GetTime $session

    ReportInfo "Starting service..."

    $deploymentCode = Run-Remote -session $session -ExitOnRemoteFail $false -returnExitCode $true -script ${function:Start-Service} -arguments $serviceName

    #ReportInfo "Parsing the server.log..."
    #$logDir = $GF_DIR+"/domains/domain1/logs"
    #$serverLog = Run-Remote -session $session -ExitOnRemoteFail $false -returnOutput $true -script ${function:Extract-ErrorLogs} -arguments $logDir, $deploymentStart 
    
    $serverLog = ""
    $msec = $null
    $message = ""

    if($deploymentCode -eq 0){

    $status = $True

        # First check
       
#        if(!$status){
#            ReportWarn """$deploymentKeyword"" was not found from the server log."

            # Second check
#            $remoteAppList = Run-Remote -session $session -exitOnRemoteFail $False -returnOutput $True -script ${function:GF-ListApps} -arguments $GF_ASADMIN, $GF_PASS
    
#            if($remoteAppList -match "$modName "){
#                ReportInfo """$modName"" is in deployed application list."
#                $status = $True
#            }

            # Third check
#            if($check_url -ne -1){ $status = CheckWebStatus($check_url) }else{ ReportInfo "Skipping URL check..." }

#        }

    }

    $deploymentEnd = Remote-GetTime $session
    if($status){    
        ReportInfo "Completed Deployment of $modName."
    }else{
        ReportWarn "Deployment of $modName was unsuccessful."  
    } 

    $result = @{
        name = $appName
        file = $destName
        status = $status
        deploymentStart= $deploymentStart
        deploymentEnd= $deploymentEnd
        archivePath = $targetPath
    }

    write-host "==========================="
    write-host "[Application Name] $($result.name)"
    write-host "[File name] $($result.file)"
    write-host "-----------"
    write-host "[Result] $(if($result.status){"SUCCESS"}else{"FAILED"})"
    write-host "-----------"
    write-host "[Start] $($result.deploymentStart)"
    write-host "[End] $($result.deploymentEnd)"
    write-host "==========================="

    $output = @{
        name = $appName
        war = $destName
        status = $(if($result.status){"success"}else{"failure"})
        message = $message
        console = ($serverLog | out-string)
        start = (Get-Date $deploymentStart -Format "yyyy-MM-dd HH:mm:ss")
        end = (Get-Date $deploymentEnd -Format "yyyy-MM-dd HH:mm:ss")
        msec = $msec
    }
    
    Export-DeploymentResult $sourceDir $output $appName
    
    return $result

}


$serverInfo = GetConfig-ByServer -Env $env -Server $server -AppList $appList
$results = @()

# $config.PSObject.Properties|foreach-object{

$serverConfig = $serverInfo.config
$appConfigs = $serverInfo.applications

ReportInfo "Starting deployment for: "
Print-Server $serverInfo

ReportInfo "Creating Session... "
$session = Create-Session $serverConfig

try{

#    $releaseDir = $serverConfig.archiveFolder+"/"+$release
#    $targetDir = $releaseDir+"/"+$buildName

#    ReportInfo "Ensuring remote target directory exists at ""$targetDir""... "
#    Remote-CreateDir $session $targetDir

    $appConfigs.PSObject.Properties|foreach-object{
        $appConfig = $_.Value
        $result = Deploy-SpringModule $session $serverConfig $appConfig $sourceDir $targetDir
        $results += $result
    }

#    Run-Remote -session $session -ExitOnRemoteFail $false -script ${function:Delete-OldArchives} -arguments $releaseDir,20 

    ReportInfo "Closing connection..."
    Close-Session $session
    ReportInfo "Successfully closed connection."


}catch{
    Write-Error $_
    Close-Session $session
    exit 1
}   

# }

ReportInfo "Completed all deployments."
write-host "==========================="
$totalResult = $true
$results | foreach-object {
    if($_.archivePath){
        write-host "[$($_.name)]...$(if($_.status){"SUCCESS"}else{"FAILED"})"
        if(!$_.status){
            $totalResult = $false
        }
    }else{
        write-host "[unknown data] $($_.name)"
    }
}


if(!$totalResult){
    exit 1
}






