#############################################################################
##
## Restart-GFServer.ps1
##
##############################################################################

<#

.SYNOPSIS

.EXAMPLE


#>


param(
    [Parameter (Mandatory = $true, Position=0)] 
    [String] $jobName,
	[Parameter(Mandatory=$true, Position=1)]
	[String] $jobPath
)

Get-ChildItem $jobPath"\workspace\"$jobName"\CustomizedReports\*" | Copy-Item  -Destination \\gh01fs01\Shares\ContinuesTestingReports\PostDeployment\$jobName"_"$(get-date -f MM-dd-yyyy)$(get-date -f HH_mm_ss)".html"  -recurse -Force

