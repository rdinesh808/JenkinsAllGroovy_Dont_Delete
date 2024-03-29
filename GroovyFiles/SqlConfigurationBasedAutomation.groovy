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

    def environment = config.environment
	def cleanUpEnvironment = config.cleanUpEnvironment
	def cleanUp=config.cleanUp

    node {

        
		
		
            //Marks build start
            print "[BUILD START]"

            //Checkout the latest code
             dir("./configurationautomation") {
			 stage('Client Config Import'){
                def checkoutResult = git credentialsId: '60c19ca5-84bb-4508-a665-0c7895d3031b', url: "https://sadhasivim@bitbucket.org/gsihealth/configurationautomation.git", changeLog:true
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
                                                 def mvnJsonCmd="mvn exec:java -Dspecification.operation=\"excelToJson\""
												 def mvnSqlCmd="mvn exec:java -Dspecification.operation=\"jsonToSql\""
												 def mvnupdateSettingsJsonCmd="mvn exec:java -Dspecification.operation=\"updateSettingsJson\""
												 //def mvnUpdateCmd="mvn liquibase:update -Dliquibase.PropertyFile=\"src/main/resources/${environment}.properties\""
												 def mvnUpdateCmd="mvn --settings settings.xml liquibase:update -Dliquibase.changeLogFile=db.changelog.xml -P${environment}"
												 
												 //def mvnCleanupCmd="mvn --settings settings.xml liquibase:update -Dliquibase.PropertyFile=\"src/main/resources/${cleanUpEnvironment}.properties\""
												 def mvnCleanupCmd="mvn --settings settings.xml liquibase:update -Dliquibase.changeLogFile=db.changelog.xml -P${cleanUpEnvironment}"
												 def mvnDbVerificationCmd="mvn exec:java -Dspecification.operation=\"dbVerification\" -Dspecification.environment=\"${environment}\""
												 def mvnXmlModificationCmd="mvn exec:java -Dspecification.operation=\"xmlModification\" -Dspecification.environment=\"${environment}\" -Dspecification.buildNo=\"${env.BUILD_NUMBER}\" -Dspecification.author=\"Transformers\""
												 def mvnXmlCleanupModificationCmd="mvn exec:java -Dspecification.operation=\"xmlModification\" -Dspecification.environment=\"CleanupScript\\${cleanUpEnvironment}_Cleanup\" -Dspecification.buildNo=\"${env.BUILD_NUMBER}\" -Dspecification.author=\"Transformer\""   
											
											
                                             
												
                            print "${mvnJsonCmd}"
                            print "${mvnSqlCmd}"	
                            print "${mvnUpdateCmd}"	
							print "${mvnCleanupCmd}"	
							print "${mvnupdateSettingsJsonCmd}"	
							print "${mvnDbVerificationCmd}"	
							print "${mvnXmlModificationCmd}"	
							print "${mvnXmlCleanupModificationCmd}"
						    bat "${clean}"	
							//exection of maven commands 
                            stage('Excel To Json'){							
                            bat "${mvnJsonCmd}"
							}
							stage('Json To Sql'){
                            bat "${mvnSqlCmd}"	
							}
							stage('Update Setting Json'){
                            bat "${mvnupdateSettingsJsonCmd}"	
							}
							
							
							stage('Liquibase cleanup'){
							if(cleanUp){
							 bat "${mvnXmlCleanupModificationCmd}"	
                            bat "${mvnCleanupCmd}"
							}else{
							print "Skipping cleanup stage"
							}
                            }
							 
                            stage('Liquibase Update'){
							bat "${mvnXmlModificationCmd}"	
                            bat "${mvnUpdateCmd}"
                            }					
							sleep 30
                            stage('Config Verification in DB'){
                            bat "${mvnDbVerificationCmd}"
                            }
						 //File Verification
                         def rollbackScriptsPath="D:\\Jenkins\\workspace\\${env.JOB_NAME}\\ConfigurationAutomation\\VerificationResult\\VerifyResult.txt"
		                 File theInfoFiles = new File("${rollbackScriptsPath}")
                         if( !theInfoFiles.exists() ) {
                           println "File does not exist"
						   error("Build failed....")
                         } else {
						 if(theInfoFiles.text){ 
                               if(theInfoFiles.text){
                  def lines = theInfoFiles.readLines()
				for (def line : lines) {
                println(line)
				if(line.length()>10){
				error("Build failed....")
				}
				}
				}
                }
                
                
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
		  bat 'git push origin HEAD:master'
		  }
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
