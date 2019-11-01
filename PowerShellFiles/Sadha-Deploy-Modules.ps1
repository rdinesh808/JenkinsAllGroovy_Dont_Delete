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
. "$PSScriptRoot/remote/GFUtils.ps1" 
. "$PSScriptRoot/remote/NodeUtils.ps1" 
. "$PSScriptRoot/remote/RemoteUtils.ps1" 
. "$PSScriptRoot/remote/Extract-ErrorLogs.ps1" 
. "$PSScriptRoot/remote/Delete-OldArchives.ps1" 
. "$PSScriptRoot/ConfigUtils.ps1" 

function Deploy-Module ($session, $serverConfig, $appConfig, $sourceDir, $targetDir){

    $status = $false

    $GF_ASADMIN = $serverConfig.gfAsadmin
    $GF_DIR = $serverConfig.gfDir
    $GF_ADMIN = $serverConfig.gfUsername
    $GF_PASS = $serverConfig.gfPassword
    $GF_SERVICE = $serverConfig.gfService

    $appName = $appConfig.appName
    $modName = $appConfig.modName
    $fileName = $appConfig.fileName
    $destFileName = if($appConfig.destFileName){$appConfig.destFileName}else{$appConfig.fileName}
    $check_url = $appConfig.url
    $deploymentOrder = $appConfig.deploymentOrder

    $targetPath = "$targetDir/$destFileName"
    $sourcePath = "$sourceDir/$fileName"

    ReportInfo "Sending application war..."
    ReportInfo "Source: $sourcePath => Dest: $targetPath"
    . "$PSScriptRoot/utils/Send-File.ps1" $sourcePath $targetPath $session

    $deploymentStart = Remote-GetTime $session

    ReportInfo "Starting deployment..."

    $deploymentCode = Run-Remote -session $session -ExitOnRemoteFail $false -returnExitCode $true -script ${function:GF-Deploy} -arguments $GF_ASADMIN, $GF_PASS, $targetPath, $deploymentOrder

    ReportInfo "Parsing the server.log..."
    $logDir = $GF_DIR+"/domains/domain1/logs"
    $serverLog = Run-Remote -session $session -ExitOnRemoteFail $false -returnOutput $true -script ${function:Extract-ErrorLogs} -arguments $logDir, $deploymentStart 
    
    $msec = $null
    $message = ""

    if($deploymentCode -eq 0){
        # First check
        $deploymentKeyword = "$modName was successfully deployed in "
       
        ReportInfo "Searching for ""$deploymentKeyword"" from server log."
        $status = $serverLog -match ([System.Text.RegularExpressions.Regex]::Escape($deploymentKeyword)+"(.+) milliseconds")
        if($matches -and $matches.count -gt 1){
            $msec = $matches[1]
        }
        if(!$status){
            ReportWarn """$deploymentKeyword"" was not found from the server log."

            # Second check
            $remoteAppList = Run-Remote -session $session -exitOnRemoteFail $False -returnOutput $True -script ${function:GF-ListApps} -arguments $GF_ASADMIN, $GF_PASS
    
            if($remoteAppList -match "$modName "){
                ReportInfo """$modName"" is in deployed application list."
                $status = $True
            }

            # Third check
            if($check_url -ne -1){ $status = CheckWebStatus($check_url) }else{ ReportInfo "Skipping URL check..." }

        }

        if($status -and ($appName -eq "dashboard")){
    
            write-host "This is dashboard deployment. Deploying Event Router..."
            
            try {
                Run-Remote -session $session -script ${function:Redux-Undeploy}
                . "$PSScriptRoot/utils/Send-File.ps1" "$sourceDir\redux.zip" "D:\Node_Apps\redux.zip" $session
                Run-Remote -session $session -script ${function:Redux-Deploy}
            }catch{
                Write-Warning $_
                $message+="Event Router deployment failed."
                $status = $False
            }
        }

		if($status -and ($appName -eq "commonLogin")){
    
            write-host "This is dashboard deployment. Deploying commonLogin Router..."
            
            try {
                Run-Remote -session $session -script ${function:commonLogin-Undeploy}
                . "$PSScriptRoot/utils/Send-File.ps1" "$sourceDir\commonLogin.zip" "D:\Node_Apps\commonLogin.zip" $session
                Run-Remote -session $session -script ${function:commonLogin-Deploy}
            }catch{
                Write-Warning $_
                $message+="commonLogin deployment failed."
                $status = $False
            }
        }
		
		if($status -and ($appName -eq "commonLoginClient")){
    
            write-host "This is dashboard deployment. Deploying commonLoginClient Router..."
            
            try {
                Run-Remote -session $session -script ${function:commonLoginClient-Undeploy}
                . "$PSScriptRoot/utils/Send-File.ps1" "$sourceDir\commonLoginClient.zip" "D:\Node_Apps\commonLoginClient.zip" $session
                Run-Remote -session $session -script ${function:commonLoginClient-Deploy}
            }catch{
                Write-Warning $_
                $message+="commonLoginClient deployment failed."
                $status = $False
            }
        }
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

    $releaseDir = $serverConfig.archiveFolder+"/"+$release
    $targetDir = $releaseDir+"/"+$buildName

    ReportInfo "Ensuring remote target directory exists at ""$targetDir""... "
    Remote-CreateDir $session $targetDir

    $appConfigs.PSObject.Properties|foreach-object{
        $appConfig = $_.Value
        $result = Deploy-Module $session $serverConfig $appConfig $sourceDir $targetDir
        $results += $result
    }

    Run-Remote -session $session -ExitOnRemoteFail $false -script ${function:Delete-OldArchives} -arguments $releaseDir,20 

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






