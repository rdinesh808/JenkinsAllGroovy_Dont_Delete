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

Get-ChildItem $jobPath"\workspace\"$jobName"\screenshots\*" | Copy-Item  -Destination \\gh01fs01\Shares\ContinuesTestingReports\SpringBoot\screenshots  -recurse -Force

