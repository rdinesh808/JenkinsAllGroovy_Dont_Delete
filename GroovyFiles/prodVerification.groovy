import com.gsihealth.jenkins.Common
import com.gsihealth.jenkins.pojo.GsiJob
import com.gsihealth.jenkins.pojo.GsiRun
import com.gsihealth.jenkins.runner.GitBuildTask
import com.gsihealth.jenkins.utils.CommonUtils
import com.gsihealth.jenkins.utils.EmailListBuilder
import com.gsihealth.jenkins.utils.GitBuildSummary
import com.gsihealth.jenkins.utils.Logger

def call(body) {


    Logger logger = new Logger()

    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
	def disableEmail=config.disableEmail
    def failure = false
	def status = false
	def katalonToolPath=config.katalonToolPath
	def testSuitePathLower
	def gitRepoURL=config.gitRepoURL
	def reportConfigPathPath=config.reportConfigPathPath
	def testSuiteCollectionPath	=config.testSuiteCollectionPath
	def testSuiteReportConfigPath = config.testSuitePath
	def testSuitePath = config.testSuitePath
	def KatalonRep=config.KatalonRep
	def testResults=config.testResults
	def project=config.prjPath
	def clientConfigPath=config.clientConfigPath
	def customizedReportFolder=config.customizedReportFolderName
	def utilityAppDIR=config.dataUtilityAppPath
	 print "${KatalonRep}"
	 print "${testSuiteCollectionPath}".length()
	
	 print "${testSuitePath}".length()
	 def runAt=config.runAt
	 if(runAt.equals("local")){
	  testSuitePathLower = "Chrome (headless)"
	 }else{
	  testSuitePathLower=runAt.toLowerCase()
	 }
	 System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "")
    print "${testSuitePathLower}"	
	def projectWorkspace = new File( "." ).getCanonicalPath()
	
    node {
	projectWorkspace = pwd()
			GitBuildTask task = new GitBuildTask(env)
			task.setConfig(config)
			task.startDate = new Date()
		stage('Checkout from BitBucket'){
				//Marks build start
				logger.info("[BUILD START]")
				//Checkout the latest script
				dir("${config.KatalonRep}") {
					def checkoutResult = git credentialsId: '35201f62-0720-4a44-8069-8b90aa4d287e', url: "${gitRepoURL}", changeLog:true
					task.checkoutResult = checkoutResult 
					sleep(10)
					bat 'cd..'
				}	
				dir("${projectWorkspace}"){
					try{
						deleteDir()	
					}catch(e){
						print "Existing files are deleted"
					}		
				}
				
				if("${utilityAppDIR}".length() >0){
					//Checkout the latest utilityapp
					dir("${utilityAppDIR}") {
						def checkoutResult = git credentialsId: '60c19ca5-84bb-4508-a665-0c7895d3031b', url: "https://sadhasivim@bitbucket.org/gsihealth/datautilityapp.git", changeLog:true
						task.checkoutResult = checkoutResult
						sleep(10)
						bat 'cd..'
					}
					
					dir("${clientConfigPath}") {
						def checkoutResult = git credentialsId: '60c19ca5-84bb-4508-a665-0c7895d3031b', url: "https://sadhasivim@bitbucket.org/gsihealth/configurationautomation.git", changeLog:true
						task.checkoutResult = checkoutResult
						sleep(10)
						bat 'cd..'
					}
					
				}
}

			if("${utilityAppDIR}".length() >0){
					stage('Data Utility and Test Data Provision') {
						try {
						 def buildEnvVars = []
							if (config.java == 8) {
								buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
							}
							
                            String goal = config.goal ?: "clean install"
							buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                            buildEnvVars.addAll(["PATH+MAVEN=${tool name: 'Maven 3.0.4', type: 'hudson.tasks.Maven$MavenInstallation'}/bin",
                                                 "MAVEN_OPTS=-XX:MaxPermSize=512m"])
												 
							dir("${utilityAppDIR}") {
								def mvnCmd="mvn clean install exec:java -Dspecification.path=\"C:\\Automation_KatalonProjects\\automationproject\\DataSpecifications\" -Dspecification.name=\"ParentName.json\" -Dspecification.batchVal=\"17\""
								withEnv(buildEnvVars) {
								bat "${mvnCmd}"
								}
								status = true
							}                     
						}catch(Exception e){
							print e
							echo "[BUILD END]"
							error("utility app execution failed")
							status = false
						}
			}
		}
  
		stage('CutomizedReportFolder creation'){
				  def katalonCmd
				 catchError {
				  try{
						   dir("${katalonToolPath}") {
									   if("${reportConfigPathPath}".length() >0){
											 katalonCmd = "katalon -runMode=console -projectPath=\"${project}\" -retry=0 -testSuitePath=\"Test Suites/${reportConfigPathPath}\" -browserType=\"${testSuitePathLower}\" -email=prabhaharan.velu@gsihealth.com -password=Test123#"
									    }else{
											logger.info("configuration error. please check the configuration")
											error("Build failed....")
											status = false
										}		   
											
										logger.info("${katalonCmd}")
										bat "${katalonCmd}"
										status = true
							 }
					}catch(e){
							  print "The Katalon error is ${e}"
							  echo "[BUILD END]"
							  status = false
							  error("Build failed....")
					}
				} 
			}
			stage('Test Execution'){
				  def katalonCmd
				  catchError {
				  try{
						   dir("${katalonToolPath}") {
									   if("${testSuiteCollectionPath}".length() >0){
											katalonCmd = "katalon -runMode=console -projectPath=\"${project}\" -reportFolder=\"${projectWorkspace}/Reports\" -reportFileName=\"report\" -retry=0 -testSuiteCollectionPath=\"Test Suites/${config.testSuiteCollectionPath}\" -email=prabhaharan.velu@gsihealth.com -password=Test123#"
									    }else if("${testSuitePath}".length() >0){
											   katalonCmd = "katalon -runMode=console -projectPath=\"${project}\" -reportFolder=\"Reports\" -reportFileName=\"report\" -retry=0 -testSuitePath=\"Test Suites/${config.testSuitePath}\" -browserType=\"${testSuitePathLower}\" -email=prabhaharan.velu@gsihealth.com -password=Test123#"							 
									    }else{
											logger.info("configuration error. please check the configuration")
											error("Build failed....")
											status = false
										}		   
											
										logger.info("${katalonCmd}")
										bat "${katalonCmd}"
										status = true
							 }
					}catch(e){
							  print "The Katalon error is ${e}"
							  echo "[BUILD END]"
							  status = false
							  error("Build failed....")
					}
				} 
			}
			
			stage('Publish Junit Report'){
						logger.info("Post Build-TestResult...")	
						sleep(20)	
						step([$class: 'JUnitResultArchiver', testResults: "${testResults}"])
						print "${testResults}"	
					//	logger.info("Test results can be viewed from http://jenkins01:8989/job/${env.JOB_NAME}/test_results_analyzer/")						
					//	print "Test results can be viewed from http://jenkins01:8989/job/${env.JOB_NAME}/test_results_analyzer/"
			}

			stage('Publish HTML Report'){
	
				 File file = new File("${katalonToolPath}\\configuration\\${customizedReportFolder}\\reportConfig.txt")
				 String dataFromFile=file.text
				 logger.info("${dataFromFile}")
				 dataFromFile=dataFromFile.replace(":", "_").replace(" ", "_")
				 logger.info("${dataFromFile}")
							
							
				dir("${katalonToolPath}") {
					def copyFile="xcopy /s \"CustomizedReports\\${customizedReportFolder}\\${dataFromFile}\" \"${projectWorkspace}\""
					bat "${copyFile}"
				}
						
				publishHTML(target: [
							allowMissing: false,
							alwaysLinkToLastBuild: false,
							keepAll: true,
							reportDir: "",
							reportFiles: 'FinalReport.html',
							reportName: "HTML Report"
				])
				
				//Email Notification
			if(disableEmail){
				try{
                    logger.info("build completed.")
					if(status){
								emailext attachmentsPattern: "**/FinalReport.html", body: "${env.JOB_NAME} - Post Deployment Succeed", mimeType: 'text/html', subject: "${env.JOB_NAME} - Post Deployment Succeed", to: 'continuousdelivery@gsihealth.com,mohan.raj@gsihealth.com,dayanidhi.kasi@gsihealth.com'
					}else{
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
							echo "${buildLog}"
							if(buildLog && buildLog.length()!=0){
								DEFAULT_CONTENT="<br>Please find the attached FinalReport of Post deployment verification test results<br><br>=========CONSOLE LOG=========<br><br><pre>${buildLog}</pre>"
							}else{
									DEFAULT_CONTENT="liquibase execution failed for ${env.JOB_NAME}"
							}	   
							emailext attachmentsPattern: "**/FinalReport.html", body: "${DEFAULT_CONTENT}", mimeType: 'text/html', subject: "${env.JOB_NAME} - Post Deployment failed", to: 'continuousdelivery@gsihealth.com,mohan.raj@gsihealth.com,dayanidhi.kasi@gsihealth.com'
					}
                }catch(e){
					echo "[BUILD END]"
                    print e
                    emailext body: "Script has some error! ${e}", subject: "Jenkins warning:${env.JOB_NAME} Build ${currentBuild.displayName}", to: 'prabhaharan.velu@gsihealth.com'
                }
			}	
				

			}
						
							
		}


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