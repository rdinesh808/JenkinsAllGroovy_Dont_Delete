import com.gsihealth.jenkins.Common
import com.gsihealth.jenkins.runner.GitBuildTask
import com.gsihealth.jenkins.utils.Logger
def call(body) {

    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    def failure = false
	def liquibaseStatus= false
	def buildEnvVars = []
	GitBuildTask task = new GitBuildTask(env)


	def cleanUp=config.cleanUp
	def instanceName=config.instanceName
	def community=config.community

    node {

        
		
		
            //Marks build start
            print "[BUILD START]"

            //Checkout the latest code
             dir("./configurationautomation") {
			 stage('Client Config Import'){
                def checkoutResult = git branch: "${config.gitBranch}",credentialsId: '60c19ca5-84bb-4508-a665-0c7895d3031b', url: "https://sadhasivim@bitbucket.org/gsihealth/configurationautomation.git", changeLog:true
                task.checkoutResult = checkoutResult
				}
				
				try {
               
                   // stage('code excecution'){
                    if (config.java == 8) {
                        buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                    }
                  
							buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                            buildEnvVars.addAll(["PATH+MAVEN=${tool name: 'Maven 3.0.4', type: 'hudson.tasks.Maven$MavenInstallation'}/bin",
                                                 "MAVEN_OPTS=-XX:MaxPermSize=512m"])
												 
												
												 
												 
                          
						 
						   withEnv(buildEnvVars) {
						    def clean="mvn clean install"
                                                 //def mvnJsonCmd="mvn exec:java -Dspecification.operation=\"excelToJson\""
												 def mvnJsonFormatChangeCmd="mvn exec:java -Dspecification.operation=\"createRequiredJsonFormat\" -Dspecification.instanceName=\"${instanceName}\""
												 def mvnSqlCmd="mvn exec:java -Dspecification.operation=\"jsonToSql\" -Dspecification.environment=\"${community}\""
												 def mvnupdateSettingsJsonCmd="mvn exec:java -Dspecification.operation=\"updateSettingsJson\""
   
											
											
                                             
												
                           // print "${mvnJsonCmd}"
                            print "${mvnJsonFormatChangeCmd}"	
                            print "${mvnSqlCmd}"
							//print "${mvnupdateSettingsJsonCmd}"	
							
						    bat "${clean}"	
							//exection of maven commands 
                           stage('Json format conversion'){
                            bat "${mvnJsonFormatChangeCmd}"	
							}
							stage('Json To Sql'){
                            bat "${mvnSqlCmd}"	
							}
							stage('Update Setting Json'){
                           // bat "${mvnupdateSettingsJsonCmd}"	
							}
						
			       
                        





						
							liquibaseStatus = true
							}
			 // }
                
            }catch (e) {
			   liquibaseStatus = false
			   echo "[BUILD END]"
			   print "script execution failed"
			   print e
			}
          }  
        
		 if(liquibaseStatus){

		     print "*****SCRIPT EXECUTED SUCCESSFULLY *****"
			 publishHTML (target: [
        allowMissing: false,
        alwaysLinkToLastBuild: false,
        keepAll: true,
        reportDir: 'configurationautomation\\VerificationResult',
        reportFiles: 'index.html',
        reportName: "RCov Report"
        ])
			 dir("./configurationautomation") {
			 bat 'git add -A'
		 def gitcmd="git commit -m \"update\""
		bat "${gitcmd}"
		bat 'git branch --set-upstream-to=origin/6.0.4 6.0.4'

		 bat 'git pull'
		  bat 'git push origin HEAD:6.0.4'
		  }
		  	stage('Liquibase Execution'){
				echo "Starting ClientConfigurationSqlExecutionVerification job"
				def dailyVer="ClientConfigurationSqlExecutionVerification"
				echo "${dailyVer}"
				build job: "${dailyVer}"
			}
			//stage('UI Validation'){
			//	echo "Starting ProgramConfigVerification job..."
			//	def dailyVer="ProgramConfigVerification"
			//	echo "${dailyVer}"
			//	build job: "${dailyVer}"
			//}
			
		 }else{
            print "***** SCRIPT EXECUTION FAILED *****"
			 publishHTML (target: [
        allowMissing: false,
        alwaysLinkToLastBuild: false,
        keepAll: true,
        reportDir: 'coverage',
        reportFiles: '**/VerificationResult/index.html',
        reportName: "RCov Report"
        ])
			 dir("./configurationautomation") {
			 bat 'git add -A'
		 def gitcmd="git commit -m \"update\""
		bat "${gitcmd}"
		  bat 'git push origin HEAD:master'
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
			DEFAULT_CONTENT="script execution failed for ${env.JOB_NAME}"
			}
			
   		   
		   emailext body: "${DEFAULT_CONTENT}", mimeType: 'text/html', subject: "${env.JOB_NAME} build failure", to: 'continuousdelivery@gsihealth.com,sadha.sivim@gsihealth.com,Murugesan.Nambi@gsihealth.com'
		   error("Build failed....")
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
