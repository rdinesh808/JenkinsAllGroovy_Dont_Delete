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
	def liquibaseStatus= false
	def dailyVer = ""
			def resultsPath=""
	def url="http://jenkins01:8989/job/${env.JOB_NAME}/test_results_analyzer/"
	def textexistingXMLFile = ""
	def soapAlert=config.soapAlert
	def resultPath=""
	def trigger=config.triggerJob
	def buildEnvVars = []
	GitBuildTask task = new GitBuildTask(env)


    node {

        
		
		
            //Marks build start
            print "[BUILD START]"


if(soapAlert){	
  try{
    File file = new File("C:\\KatalonDataFiles\\DemoPerfection\\Data Files\\SeverityAlerts Interoperability.txt")
    File existingXMLFile = new File("D:\\Sadha\\soapProjectFiles\\Alerts-soapui-project Interoperability.xml")
    textexistingXMLFile=existingXMLFile.text
    String dataFromFile=file.text
    String[] descriptiondatas = dataFromFile.split("\\n")
	for(int loopCount=0;loopCount<descriptiondatas.length;loopCount++){
    String descriptionWithPatientID=descriptiondatas[loopCount]
    String[] splitDescriptionWithPatientID=descriptionWithPatientID.split("&&")
    String endPointURL = splitDescriptionWithPatientID[0].trim()
    String alertName = splitDescriptionWithPatientID[1].trim()
	String AlertId = splitDescriptionWithPatientID[2].trim()
    String alertDescription = splitDescriptionWithPatientID[3].trim() 
    String patientID = splitDescriptionWithPatientID[4].trim()
    String xmlFile=textexistingXMLFile.replaceAll("<<endPointURL>>",endPointURL)
    xmlFile=xmlFile.replaceAll("<<alertDescription>>",alertDescription)
    xmlFile=xmlFile.replaceAll("<<AlertName>>",alertName)
	xmlFile=xmlFile.replaceAll("<<AlertId>>",AlertId)
    xmlFile=xmlFile.replaceAll("<<patientID>>",patientID)
   // println xmlFile
    File filenew = new File("D:\\Jenkins\\workspace\\${env.JOB_NAME}\\newlyUpdatedfile.xml")
	if(filenew.exists()){
	filenew.delete()
	filenew = new File("D:\\Jenkins\\workspace\\${env.JOB_NAME}\\newlyUpdatedfile.xml")
	}
    filenew << xmlFile
    String batCmd="SmartBear\\SoapUI-5.4.0\\bin\\testrunner.bat -sAlertNotification \"D:\\Jenkins\\workspace\\${env.JOB_NAME}\\newlyUpdatedfile.xml\""
	//println "${batCmd}"
	bat "${batCmd}"
	filenew.delete()
	status = true   
     }

	
	
	 }catch(Exception e){
	 print e
	 status = false
	 echo "[BUILD END]"
	 error("soap execution failed")
	 }
  
}		 
		            //Checkout the latest code
             dir("./daily_interface_scripts") {	 
			 stage('selenium code checkout'){                                                                                        
                def checkoutResult = git branch: "${config.gitBranch}", credentialsId: '60c19ca5-84bb-4508-a665-0c7895d3031b', url: "https://sadhasivim@bitbucket.org/gsihealth/daily_interface_scripts.git", changeLog:true
                task.checkoutResult = checkoutResult
				}
				
				try {
               
                    stage('Script excecution'){
                    if (config.java == 8) {
                        buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                    }
                  
							buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                            buildEnvVars.addAll(["PATH+MAVEN=${tool name: 'Maven 3.0.4', type: 'hudson.tasks.Maven$MavenInstallation'}/bin",
                                                 "MAVEN_OPTS=-XX:MaxPermSize=512m"])        						 
						   withEnv(buildEnvVars) {
                             print "*****RUNNING THE TESTCASES*****"
                            String goal = config.goal?:"clean install -e"
                            def mvnTestCmd="mvn ${goal}"	
					        print "${mvnTestCmd}"
                            bat "${mvnTestCmd}"                 						                         						
							liquibaseStatus = true
							}
			  }
                
            }catch (e) {
			   liquibaseStatus = false
			   echo "[BUILD END]"
			   print "Script execution failed"
			   print e
			}
          }  
        
		 if(liquibaseStatus){
		 //def fileJobName = new File("D:/Sadha/test.txt")
         //fileJobName.text = "${env.JOB_NAME}"
		    bat'NUL>D:\\Sadha\\jobName.txt'
			bat "echo ${env.JOB_NAME}>D:\\Sadha\\jobName.txt"
		     print "***** SCRIPT EXECUTED SUCCESSFULLY *****"
			 step([$class: 'JUnitResultArchiver', testResults: '**/daily_interface_scripts/target/surefire-reports/junitreports/*.xml'])
			 publishHTML (target: [
        allowMissing: false,
        alwaysLinkToLastBuild: false,
        keepAll: true,
        reportDir: 'daily_interface_scripts//report',
        reportFiles: '*.html',
        reportName: "RCov Report"
        ])
	 try{
             echo "Starting log extraction"
     if("${trigger}"=="NM"){
			 dailyVer="Interoperability-GetServerLogsNM"
			 }else if("${trigger}"=="XDS"){
			 dailyVer="Interoperability-GetServerLogsXDS"
			 }else{
			 echo "***************Skipping getServerLogs jobs************"
			 }
			echo "${dailyVer}"
            //build job: "${dailyVer}",parameters: [[$class: 'StringParameterValue', name: 'resourceJob', value: "${env.JOB_NAME}" ]]
			//adding jenkins report api call
		
			if("${env.JOB_NAME}".contains("eMedNy")){
			  resultPath="D:\\Jenkins\\workspace\\${env.JOB_NAME}\\daily_interface_scripts\\src\\output\\Emedny\\aggregateReport\\reportlog.txt"
			 }else if("${env.JOB_NAME}".contains("InboundXDS")){
			  resultPath="D:\\Jenkins\\workspace\\${env.JOB_NAME}\\daily_interface_scripts\\src\\output\\Inbound_xds\\aggregateReport\\reportlog.txt"
			 }else if("${env.JOB_NAME}".contains("Inboundalerts")){
			  resultPath="D:\\Jenkins\\workspace\\${env.JOB_NAME}\\daily_interface_scripts\\src\\output\\Inbound_alerts\\aggregateReport\\reportlog.txt"
			 }else if("${env.JOB_NAME}".contains("OutboundNotification")){
			  resultPath="D:\\Jenkins\\workspace\\${env.JOB_NAME}\\daily_interface_scripts\\src\\output\\Outbound_notification\\aggregateReport\\reportlog.txt"
			 }else{
			  resultPath=null
			 echo "*************** Skipping api hit ************"
			 }
			 if("${resultPath}"!=null){
			 echo "${resultPath}"
			 File theReportFiles = new File("${resultPath}")
                         if( !theReportFiles.exists() ) {
                           println "report File does not exist .please check"
						   error("Build failed....")
                         } else {
						 if(theReportFiles.text){ 
                               if(theReportFiles.text){
bat 'cd D:\\Jenkins\\workspace\\Interoperability-Inboundalerts_DailyVerification_Prod\\xmlmodify'
                  //def mvnJsonReportCmd="mvn exec:java -Dspecification.BuildName=\"${env.JOB_NAME}\" -Dspecification.SourceFile=\"${resultPath}\" -Dspecification.BuildNo=\"${env.BUILD_NUMBER}\" -Dspecification.TeamName=\"${config.teamName}\" -Dspecification.CategoryName=\"${config.categoryName}\" -Dspecification.ProjectName=\"${config.projectName}\" -Dspecification.BuildDate=\"${env.BUILD_TIMESTAMP}\" -Dspecification.Environment=\"${config.environment}\""
				 stage('Script excecution'){
                    if (config.java == 8) {
                        buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                    }
                  
							buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                            buildEnvVars.addAll(["PATH+MAVEN=${tool name: 'Maven 3.0.4', type: 'hudson.tasks.Maven$MavenInstallation'}/bin",
                                                 "MAVEN_OPTS=-XX:MaxPermSize=512m"])        						 
						   withEnv(buildEnvVars) {
                            def mvnJsonReportCmd="mvn exec:java -f D:/Sadha/xmlmodify/pom.xml -Dspecification.BuildName=\"${env.JOB_NAME}\" -Dspecification.SourceFile=\"${resultPath}\" -Dspecification.BuildNo=\"${env.BUILD_NUMBER}\" -Dspecification.TeamName=\"${config.teamName}\" -Dspecification.CategoryName=\"${config.categoryName}\" -Dspecification.ProjectName=\"${config.projectName}\" -Dspecification.BuildDate=\"${env.BUILD_TIMESTAMP}\" -Dspecification.Url=\"${url}\" -Dspecification.Type=\"Child\" -Dspecification.Environment=\"${config.environment}\" -Dspecification.InterfaceType=\"${config.jobType}\""
				 // print "${mvnJsonReportCmd}"
                  bat "${mvnJsonReportCmd}" 
							}
			  }  
                   
				
				} else {
				println "report File is empty.please check"
				error("Build failed....")
				}
                }
                }
				}
			step([$class: 'Publisher', reportFilenamePattern: '**/testng-results.xml'])
						 stage('Script excecution'){
                    if (config.java == 8) {
                        buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                    }
                  
							buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                            buildEnvVars.addAll(["PATH+MAVEN=${tool name: 'Maven 3.0.4', type: 'hudson.tasks.Maven$MavenInstallation'}/bin",
                                                 "MAVEN_OPTS=-XX:MaxPermSize=512m"])        						 
						   withEnv(buildEnvVars) {
                            def mvnJsonReportCmd="mvn exec:java -f D:/Sadha/xmlmodify/pom.xml -Dspecification.BuildName=\"${env.JOB_NAME}\" -Dspecification.BuildNo=\"${env.BUILD_NUMBER}\" -Dspecification.TeamName=\"${config.teamName}\" -Dspecification.CategoryName=\"${config.categoryName}\" -Dspecification.ProjectName=\"${config.projectName}\" -Dspecification.BuildStatus=\"Pass\" -Dspecification.Url=\"${url}\" -Dspecification.Type=\"Parent\" -Dspecification.Environment=\"${config.environment}\" -Dspecification.InterfaceType=\"${config.jobType}\""
				 // print "${mvnJsonReportCmd}"
                  bat "${mvnJsonReportCmd}" 
							}
							echo "Deleting the file ${resultPath}"
							   File fileDelete = new File("${resultPath}")
                 fileDelete.delete()  
			  }  
			 }catch(e){
            print e
            echo "There are errors in test"
			step([$class: 'Publisher', reportFilenamePattern: '**/testng-results.xml'])
            				 stage('Script excecution'){
                    if (config.java == 8) {
                        buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                    }
                  
							buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                            buildEnvVars.addAll(["PATH+MAVEN=${tool name: 'Maven 3.0.4', type: 'hudson.tasks.Maven$MavenInstallation'}/bin",
                                                 "MAVEN_OPTS=-XX:MaxPermSize=512m"])        						 
						   withEnv(buildEnvVars) {
                            def mvnJsonReportCmd="mvn exec:java -f D:/Sadha/xmlmodify/pom.xml -Dspecification.BuildName=\"${env.JOB_NAME}\" -Dspecification.BuildNo=\"${env.BUILD_NUMBER}\" -Dspecification.TeamName=\"${config.teamName}\" -Dspecification.CategoryName=\"${config.categoryName}\" -Dspecification.ProjectName=\"${config.projectName}\" -Dspecification.BuildStatus=\"Pass\" -Dspecification.Url=\"${url}\" -Dspecification.Type=\"Parent\" -Dspecification.Environment=\"${config.environment}\" -Dspecification.InterfaceType=\"${config.jobType}\""
				 // print "${mvnJsonReportCmd}"
                  bat "${mvnJsonReportCmd}" 

							}
							echo "Deleting the file ${resultPath}"
				   File fileDelete = new File("${resultPath}")
                   fileDelete.delete()  
			  }  
        }			 
		 }else{
		  publishHTML (target: [
        allowMissing: false,
        alwaysLinkToLastBuild: false,
        keepAll: true,
        reportDir: 'daily_interface_scripts//report',
        reportFiles: '*.html',
        reportName: "RCov Report"
        ])
		 //def fileJobName = new File("D://Sadha//test.txt")
         //fileJobName.write = "${env.JOB_NAME}"
            print "***** SCRIPT EXECUTION FAILED *****"
			//bat'cd D:\\Sadha'
			bat'NUL>D:\\Sadha\\jobName.txt'
			bat "echo ${env.JOB_NAME}>D:\\Sadha\\jobName.txt"
				 try{
             echo "Starting log extraction"
   
			  if("${trigger}"=="NM"){
			 dailyVer="Interoperability-GetServerLogsNM"
			 }else if("${trigger}"=="XDS"){
			 dailyVer="Interoperability-GetServerLogsXDS"
			 }else{
			 echo "***************Skipping getServerLogs jobs************"
			 }
			echo "${dailyVer}"
            //build job: "${dailyVer}"
			
			//adding jenkins report api call
	
			if("${env.JOB_NAME}".contains("eMedNy")){
			  resultsPath="D:\\Jenkins\\workspace\\${env.JOB_NAME}\\daily_interface_scripts\\src\\output\\Emedny\\aggregateReport\\reportlog.txt"
			 }else if("${env.JOB_NAME}".contains("InboundXDS")){
			  resultsPath="D:\\Jenkins\\workspace\\${env.JOB_NAME}\\daily_interface_scripts\\src\\output\\Inbound_xds\\aggregateReport\\reportlog.txt"
			 }else if("${env.JOB_NAME}".contains("Inboundalerts")){
			  resultsPath="D:\\Jenkins\\workspace\\${env.JOB_NAME}\\daily_interface_scripts\\src\\output\\Inbound_alerts\\aggregateReport\\reportlog.txt"
			 }else if("${env.JOB_NAME}".contains("OutboundNotification")){
			  resultsPath="D:\\Jenkins\\workspace\\${env.JOB_NAME}\\daily_interface_scripts\\src\\output\\Outbound_notification\\aggregateReport\\reportlog.txt"
			 }else{
			  resultsPath=null
			 echo "*************** Skipping api hit ************"
			 }
			 if("${resultsPath}"!=null){
			  echo "${resultsPath}"
			 File theReportFiles = new File("${resultsPath}")
                         if( !theReportFiles.exists() ) {
                           println "report File does not exist .please check"
						   error("Build failed....")
                         } else {
						 if(theReportFiles.text){ 
                               if(theReportFiles.text){
							    if (config.java == 8) {
                        buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                    }
                  
							buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                            buildEnvVars.addAll(["PATH+MAVEN=${tool name: 'Maven 3.0.4', type: 'hudson.tasks.Maven$MavenInstallation'}/bin",
                                                 "MAVEN_OPTS=-XX:MaxPermSize=512m"])        						 
						   withEnv(buildEnvVars) {
                               def mvnJsonReportCmd="mvn exec:java -f D:/Sadha/xmlmodify/pom.xml -Dspecification.BuildName=\"${env.JOB_NAME}\" -Dspecification.SourceFile=\"${resultsPath}\" -Dspecification.BuildNo=\"${env.BUILD_NUMBER}\" -Dspecification.TeamName=\"${config.teamName}\" -Dspecification.CategoryName=\"${config.categoryName}\" -Dspecification.ProjectName=\"${config.projectName}\" -Dspecification.BuildDate=\"${env.BUILD_TIMESTAMP}\" -Dspecification.Url=\"${url}\" -Dspecification.Type=\"Child\" -Dspecification.Environment=\"${config.environment}\" -Dspecification.InterfaceType=\"${config.jobType}\""
				  //print "${mvnJsonReportCmd}"
                  bat "${mvnJsonReportCmd}"  
							}
                 //bat 'cd D:\\Jenkins\\workspace\\Interoperability-Inboundalerts_DailyVerification_Prod\\xmlmodify'
               
				
				} else {
				println "report File is empty.please check"
				error("Build failed....")
				}
                }
                }
				}
			step([$class: 'Publisher', reportFilenamePattern: '**/testng-results.xml'])
						 stage('Script excecution'){
                    if (config.java == 8) {
                        buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                    }
                  
							buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                            buildEnvVars.addAll(["PATH+MAVEN=${tool name: 'Maven 3.0.4', type: 'hudson.tasks.Maven$MavenInstallation'}/bin",
                                                 "MAVEN_OPTS=-XX:MaxPermSize=512m"])        						 
						   withEnv(buildEnvVars) {
                            def mvnJsonReportCmd="mvn exec:java -f D:/Sadha/xmlmodify/pom.xml -Dspecification.BuildName=\"${env.JOB_NAME}\" -Dspecification.BuildNo=\"${env.BUILD_NUMBER}\" -Dspecification.TeamName=\"${config.teamName}\" -Dspecification.CategoryName=\"${config.categoryName}\" -Dspecification.ProjectName=\"${config.projectName}\" -Dspecification.BuildStatus=\"Fail\" -Dspecification.Url=\"${url}\" -Dspecification.Type=\"Parent\" -Dspecification.Environment=\"${config.environment}\" -Dspecification.InterfaceType=\"${config.jobType}\""
				  //print "${mvnJsonReportCmd}"
                  bat "${mvnJsonReportCmd}" 
							}
							     	echo "Deleting the file ${resultsPath}"
            File fileDelete = new File("${resultsPath}")
              fileDelete.delete()   
			  }  
			 }catch(e){
            print e
            echo "There are errors in test"
			step([$class: 'Publisher', reportFilenamePattern: '**/testng-results.xml'])
            //failure = true
            echo "Deleting the file ${resultsPath}"
            File fileDelete = new File("${resultsPath}")
           //fileDelete.delete()  
        }
	        def jobPath = "${env.JENKINS_HOME}"
            jobPath = getJobPath(jobPath, env.JOB_NAME)
            def consolePath = jobPath+ "\\builds\\${env.BUILD_NUMBER}\\log"
			sleep 5
            echo "reading the console log..."
            def log = readFile(consolePath)
			def buildLog =  lastMatch(log, /\[BUILD START\]([\s|\S]*)\[BUILD END\]/)
            if(buildLog){
                buildLog=replaceAll(buildLog,/\[8mh.+\[Pipeline\](?:(?!\r\n)[\s|\S])*\r\n/,"")
                buildLog=replaceAll(buildLog,/\[8mh.+\[Pipeline\]/,"")
				
				}
				def DEFAULT_CONTENT
		   if(buildLog && buildLog.length()!=0){
                DEFAULT_CONTENT="<br>=========CONSOLE LOG=========<br><br><pre>${buildLog}</pre>"
            }else{
			step([$class: 'JUnitResultArchiver', testResults: '**/daily-monitoring-interface-scripts/target/surefire-reports/junitreports/*.xml'])	
			DEFAULT_CONTENT="script execution failed for ${env.JOB_NAME}"
			}
			 def project = Jenkins.instance.getItem("${dailyVer}")
     def lastSuc=project.getLastSuccessfulBuild()
	 def lastSucNum = lastSuc.getNumber()
   		echo "D:\\Jenkins\\jobs\\${dailyVer}\\builds\\${lastSucNum}\\log"   
		   //emailext attachmentsPattern: "D:\\Jenkins\\jobs\\${dailyVer}\\builds\\${lastSucNum}\\log.txt",body: "${DEFAULT_CONTENT}", mimeType: 'text/html', subject: "${env.JOB_NAME} build failure", to: 'dayanidhi.kasi@gsihealth.com,hemanth.kumar@gsihealth.com,sadha.sivim@gsihealth.com,Murugesan.Nambi@gsihealth.com'
		  error("Build failed....")
		  			 
                    if (config.java == 8) {
                        buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                    }
                  
							buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                            buildEnvVars.addAll(["PATH+MAVEN=${tool name: 'Maven 3.0.4', type: 'hudson.tasks.Maven$MavenInstallation'}/bin",
                                                 "MAVEN_OPTS=-XX:MaxPermSize=512m"])        						 
						   withEnv(buildEnvVars) {
						   
                            def mvnJsonReportCmd="mvn exec:java -f D:/Sadha/xmlmodify/pom.xml -Dspecification.BuildName=\"${env.JOB_NAME}\" -Dspecification.BuildNo=\"${env.BUILD_NUMBER}\" -Dspecification.TeamName=\"${config.teamName}\" -Dspecification.CategoryName=\"${config.categoryName}\" -Dspecification.ProjectName=\"${config.projectName}\" -Dspecification.BuildStatus=\"Fail\" -Dspecification.Url=\"${url}\" -Dspecification.Type=\"Parent\" -Dspecification.Environment=\"${config.environment}\" -Dspecification.InterfaceType=\"${config.jobType}\""
				  //print "${mvnJsonReportCmd}"
                  bat "${mvnJsonReportCmd}" 
							}
							echo "Deleting the file ${resultsPath}"
				 File fileDelete = new File("${resultsPath}")
                   fileDelete.bytes = new byte[0]
			  
		}
	} 
		
   }



@NonCPS
def notifyFailed(body) {
def bodyContent=body
mail (to: 'sadha.sivim@gsihealth.com,Murugesan.Nambi@gsihealth.com,prabhaharan.velu@gsihealth.com',
subject: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) has failed....",
body: "Job Failed ${bodyContent}.");

}
@NonCPS
def notifyPassed() {
mail (to: 'sadha.sivim@gsihealth.com,Murugesan.Nambi@gsihealth.com,prabhaharan.velu@gsihealth.com',
//mail (to: 'ContinuousDelivery@gsihealth.com',
 subject: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) has passed....",
 body: "Job passed ${env.BUILD_URL}.");
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

