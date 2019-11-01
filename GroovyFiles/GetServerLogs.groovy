import com.gsihealth.jenkins.Common
import com.gsihealth.jenkins.pojo.GsiJob
import com.gsihealth.jenkins.pojo.GsiRun
import com.gsihealth.jenkins.runner.GitBuildTask
import com.gsihealth.jenkins.utils.CommonUtils
import com.gsihealth.jenkins.utils.EmailListBuilder
import com.gsihealth.jenkins.utils.GitBuildSummary
import com.gsihealth.jenkins.utils.Logger

def call(body) {

    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    def failure = false
	def spring = config.Tests
     def date="";
	 def path="";
	 def time="";
	 def rollbackScriptsPath="";
	 def jobPaths="";
    node {
	print "[BUILD START]"
GitBuildTask task = new GitBuildTask(env)
        task.setConfig(config)
        task.startDate = new Date()
        try{
		
		
		//check job name
		def jobnameScriptPath="D:\\Sadha\\jobName.txt"
		                 File InfoFile = new File("${jobnameScriptPath}")
                         if( !InfoFile.exists() ) {
						  println "${jobnameScriptPath}"
                           println "File does not exist"
						   error("Build failed....")
                         } else {
						 if(InfoFile.text){ 
                              
                 def finalDate=InfoFile.text
				  jobPaths=finalDate.trim()
				   println "${jobPaths}"
				  
				
                }
             }
		
		
		
		//date File 
                def rollbackScriptPath="D:\\Jenkins\\workspace\\${jobPaths}\\daily_interface_scripts\\src\\output\\Outbound_notification\\TestCaseName\\TestName.txt"
		                 File theInfoFile = new File("${rollbackScriptPath}")
                         if( !theInfoFile.exists() ) {
						  println "${rollbackScriptPath}"
                           println "File does not exist"
						   error("Build failed....")
                         } else {
						 if(theInfoFile.text){ 
                              
                 def finalDate=theInfoFile.text
				  path=finalDate.trim()
				   println "${path}"
				  
				
                }
             }
		
		
		//date File
if("${path}"=="Outbound")	{

                 rollbackScriptsPath="D:\\Jenkins\\workspace\\${jobPaths}\\daily_interface_scripts\\src\\output\\Outbound_notification\\logChecktime\\Outbound_logChecktime.txt"
 println "${rollbackScriptsPath}"
				 }else if("${path}"=="Inbound"){
                  rollbackScriptsPath="D:\\Jenkins\\workspace\\${jobPaths}\\daily_interface_scripts\\src\\output\\Inbound_alerts\\logChecktime\\Inbound_logChecktime.txt"
println "${rollbackScriptsPath}"
				  }else{
				  rollbackScriptsPath="D:\\Jenkins\\workspace\\${jobPaths}\\daily_interface_scripts\\src\\output\\Inbound_xds\\logChecktime\\Inboundxds_logChecktime.txt"
println "${rollbackScriptsPath}"
				  }	
stage('Get Logs'){				  
				  File theInfoFiles = new File("${rollbackScriptsPath}")
				  
                         if( !theInfoFiles.exists() ) {
						 println "${rollbackScriptsPath}"
                           println "File does not exist"
						   error("Build failed....")
                         } else {
						  if(theInfoFiles.text){ 
                def lines = theInfoFiles.readLines()
				for (String finalDate : lines) {
                println(finalDate)
				 date=finalDate.trim()
				   println "${date}"
				   time=  date.split('-')
				  echo "*********Environment: ${time[0]}**************"	  
				def PS_deploy = "D:/jenkins_utils/deployment_v2/ps/GetServerLogs.ps1 -env ${time[0]} -environmentType ${config.environmentType} -server ${config.server} -date ${time[1]} -end ${time[2]}"
                bat script: "${getPSCmd(PS_deploy)}"  
                echo "[BUILD END]"				
               }
			   }else{
				 println "Skipping execution of Getting Logs since no date is present in file"
				 echo "[BUILD END]"
				 failure = true
				 error("Build failed....")
				}
             }
			 }
		
        }catch(e){
            print e
            echo "[BUILD END]"
            failure = true
        }
   
   		if(failure){
		
		     print "***** SCRIPT EXECUTION FAILED *****"
			 def jobPath = "${env.JENKINS_HOME}"
            jobPath = getJobPath(jobPath, env.JOB_NAME)
            def consolePath = jobPath+ "\\builds\\${env.BUILD_NUMBER}\\log"
			sleep 5
            echo "reading the console log..."
            def log = readFile(consolePath)
			def buildLog =  lastMatch(log, /\[BUILD START\]([\s|\S]*)\[BUILD END\]/)
			echo "build log is ${buildLog}"
            if(buildLog){
                buildLog=replaceAll(buildLog,/\[8mh.+\[Pipeline\](?:(?!\r\n)[\s|\S])*\r\n/,"")
                buildLog=replaceAll(buildLog,/\[8mh.+\[Pipeline\]/,"")
				
				}
				def DEFAULT_CONTENT
		   if(buildLog && buildLog.length()!=0){
                DEFAULT_CONTENT="<br>=========CONSOLE LOG=========<br><br><pre>${buildLog}</pre>"
            }else{
				
			DEFAULT_CONTENT="script execution failed for ${env.JOB_NAME}"
			}
			 bat"NUL>D:\\Jenkins\\workspace\\${env.JOB_NAME}\\jobName.txt"
			bat "echo ${DEFAULT_CONTENT}>D:\\Jenkins\\workspace\\${env.JOB_NAME}\\jobName.txt"
		   emailext body: "${DEFAULT_CONTENT}", mimeType: 'text/html', subject: "${env.JOB_NAME} build failure", to: 'dayanidhi.kasi@gsihealth.com,hemanth.kumar@gsihealth.com,sadha.sivim@gsihealth.com,Murugesan.Nambi@gsihealth.com', attachLog: true
		  
			error("Build failed....")
		 }else{
		 print "***** SCRIPT EXECUTION PASSED *****"
			
	        def jobPath1 = "${env.JENKINS_HOME}"
            jobPath1 = getJobPath(jobPath1, env.JOB_NAME)
            def consolePath1 = jobPath1+ "\\builds\\${env.BUILD_NUMBER}\\log"
			sleep 5
            def log1 = readFile(consolePath1)		
			def buildLog1 =  lastMatch(log1, /\[BUILD START\]([\s|\S]*)\[BUILD END\]/)			
            if(buildLog1){
                buildLog1=replaceAll(buildLog1,/\[8mh.+\[Pipeline\](?:(?!\r\n)[\s|\S])*\r\n/,"")
                buildLog1=replaceAll(buildLog1,/\[8mh.+\[Pipeline\]/,"")
				
				}
				def DEFAULT_CONTENT1
		   if(buildLog1 && buildLog1.length()!=0){
                DEFAULT_CONTENT1="<br>=========CONSOLE LOG=========<br><br><pre>${buildLog1}</pre>"
            }else{
				
			DEFAULT_CONTENT1="script execution failed for ${env.JOB_NAME}"
			}
			 
		   bat"NUL>D:\\Jenkins\\workspace\\${env.JOB_NAME}\\jobName.txt"
			//bat "echo ${buildLog1}>D:\\Jenkins\\workspace\\${env.JOB_NAME}\\jobName.txt"
		   emailext attachmentsPattern:"${consolePath1}" ,body: "${DEFAULT_CONTENT1}", mimeType: 'text/html', subject: "${env.JOB_NAME} build Passed", to: 'dayanidhi.kasi@gsihealth.com,hemanth.kumar@gsihealth.com,sadha.sivim@gsihealth.com,Murugesan.Nambi@gsihealth.com', attachLog: true
		  
		}			
    
		
    }
 

}
@NonCPS
def notifyFailed() {
mail (to: 'sadha.sivim@gsihealth.com,Murugesan.Nambi@gsihealth.com,prabhaharan.velu@gsihealth.com',
subject: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) has failed....",
body: "Job Failed ${env.BUILD_URL}.");

}
@NonCPS
def notifyPassed() {
mail (to: 'sadha.sivim@gsihealth.com,Murugesan.Nambi@gsihealth.com,prabhaharan.velu@gsihealth.com',
//mail (to: 'ContinuousDelivery@gsihealth.com',
 subject: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) has passed....",
 body: "Job passed ${env.BUILD_URL}.");
}

@NonCPS
def String getPSCmd(command){
    def prefix = "powershell.exe -command \""
    def suffix = "\";exit \$LASTEXITCODE;"
    return prefix+command+suffix
}

@NonCPS
def getJobPath(rootPath, job_name) {
    def arr = job_name.split("/")
    for (int i = 0; i < arr.length; i++) {
        rootPath += "\\jobs\\${arr[i]}"
    }
    return rootPath
}
//(Checking out[\s|\S]*)stage \(Archive\)
//Don't forget capture group...!
@NonCPS
def String lastMatch(text, regex) {
    def m;
    if ((m = text =~ regex)) {
        return m[0][-1]
    }
    return ''
}

@NonCPS
def String replaceUrl( text, text2) {
    return text.replaceAll("Jenkins01:8989", text2)
}
@NonCPS
def String replaceAll( text, regex, newText) {
    return text.replaceAll(regex, newText)
}
