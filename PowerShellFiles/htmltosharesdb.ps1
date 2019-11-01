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
#New-Item -ItemType directory -Path \\gh01fs01\Shares\DprodStatus\$(get-date -f MM-dd-yyyy)$(get-date -f HH_mm_ss)"qatester3"
Get-ChildItem $jobPath"\workspace\"$jobName"\CustomizedReports\DataBleed\*" | Copy-Item  -Destination \\gh01fs01\Shares\ContinuesTestingReports\DataBleed\$jobName"_"$(get-date -f MM-dd-yyyy)$(get-date -f HH_mm_ss)".html"  -recurse -Force

