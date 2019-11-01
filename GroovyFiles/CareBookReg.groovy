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
    def failure = false
	def status = false
	def katalonToolPath=config.katalonToolPath
	def runSoap = config.runSoap
	def testSuitePathLower
	def reportConfigPathPath=config.reportConfigPathPath
	def testSuiteCollectionPath	=config.testSuiteCollectionPath
	def testSuiteReportConfigPath = config.testSuitePath
	def testSuitePath = config.testSuitePath
	def KatalonRep=config.KatalonRep
	def testResults=config.testResults
	def project=config.prjPathFromRoot
	def customizedReportFolder=config.customizedReportFolderName
	 print "${KatalonRep}"
	 print "${testSuiteCollectionPath}".length()
	
	 print "${testSuitePath}".length()
	 def runAt=config.runAt
	 if(runAt.equals("local")){
	  testSuitePathLower = "Chrome (headless)"
	 }else{
	  testSuitePathLower=runAt.toLowerCase()
	 }
	 
    print "${testSuitePathLower}"
    node {
			GitBuildTask task = new GitBuildTask(env)
			task.setConfig(config)
			task.startDate = new Date()
		stage('Checkout from BitBucket'){
				//Marks build start
				logger.info("[BUILD START]")
				//Checkout the latest script
				dir("./${config.KatalonRep}") {
					def checkoutResult = git credentialsId: '35201f62-0720-4a44-8069-8b90aa4d287e', url: "https://Prabhaharan@bitbucket.org/gsihealth/${config.KatalonRep}.git", changeLog:true
					task.checkoutResult = checkoutResult 
					sleep(10)
					bat 'cd..'
					if((!"${project}".contains("DailyNonProdVerification"))){					
						try{
						dir("./Reports") {
						bat 'del /S /q *.*'
						bat 'for /f "usebackq delims=" %%d in (`"dir /ad/b/s | sort /R"`) do rd "%%d"'
						}
						
						dir("C:\\Users\\pvelu\\.jenkins\\workspace\\Reports\\AutomationJunitReports\\Reports") {
						bat 'if exist *.* del /S /q *.*'
						bat 'if exist *.* for /f "usebackq delims=" %%d in (`"dir /ad/b/s | sort /R"`) do rd "%%d"'
						}
						}catch(e){
						print "Existing files are deleted"
						}
					}
		}
}
  
		stage('CutomizedReportFolder creation'){
				  def katalonCmd
				 catchError {
				  try{
						   dir("${katalonToolPath}") {
									   if("${reportConfigPathPath}".length() >0){
											 katalonCmd = "katalon -runMode=console -projectPath=\"C:/Users/pvelu/.jenkins/workspace/${config.prjPathFromRoot}\" -retry=0 -testSuitePath=\"Test Suites/${reportConfigPathPath}\" -browserType=\"${testSuitePathLower}\" -email=prabhaharan.velu@gsihealth.com -password=Test123#"
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
											katalonCmd = "katalon -runMode=console -projectPath=\"C:/Users/pvelu/.jenkins/workspace/${config.prjPathFromRoot}\" -reportFileName=\"report\" -retry=0 -testSuiteCollectionPath=\"Test Suites/${config.testSuiteCollectionPath}\" -email=prabhaharan.velu@gsihealth.com -password=Test123#"
									    }else if("${testSuitePath}".length() >0){
											   katalonCmd = "katalon -runMode=console -projectPath=\"C:/Users/pvelu/.jenkins/workspace/${config.prjPathFromRoot}\" -retry=0 -testSuitePath=\"Test Suites/${config.testSuitePath}\" -browserType=\"${testSuitePathLower}\" -email=prabhaharan.velu@gsihealth.com -password=Test123#"							 
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
			
			stage('Post Build-Publish Junit Report'){
						logger.info("Customized Report Results Path: file:///${katalonToolPath}/CustomizedReports/")	
						logger.info("Email stage...")
						sleep(20)	
						step([$class: 'JUnitResultArchiver', testResults: '**/*/Reports/*/*/*/*/*.xml'])
						
						print "${testResults}"	
						logger.info("Test results can be viewed from http://jenkins01:8989/job/${env.JOB_NAME}/test_results_analyzer/")						
						print "Test results can be viewed from http://jenkins01:8989/job/${env.JOB_NAME}/test_results_analyzer/"

			}
			
			stage('Publish HTML Report'){
			
									//Copy Customized report file
							def mkDir="mkdir C:\\Users\\pvelu\\.jenkins\\workspace\\DailyNonProdVerification\\automationproject\\CustomizedReports"
							try{
							bat "${mkDir}"
							}catch(e){
							customizedReportFolder
							
							dir("C:\\Users\\pvelu\\.jenkins\\workspace\\DailyNonProdVerification\\automationproject\\CustomizedReports") {
								bat 'if exist *.* del /S /q *.*'
								bat 'if exist *.* for /f "usebackq delims=" %%d in (`"dir /ad/b/s | sort /R"`) do rd "%%d"'
							}
							}
							def customizedReportFolderPath="${katalonToolPath}\\CustomizedReports\\${customizedReportFolder}"
							
							dir("${katalonToolPath}") {
							def copyFile="xcopy /s \"CustomizedReports\\*.*\" \"C:\\Users\\pvelu\\.jenkins\\workspace\\DailyNonProdVerification\\automationproject\\CustomizedReports\""
							bat "${copyFile}"
							}
							
							
			//	System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "")
						archive (includes: 'pkg/*.gem')
						
						//	FileReader fr =new FileReader("${katalonToolPath}\\configuration\\${customizedReportFolder}\\reportConfig.txt");
						//	def reportfolderPath=fr.text
						//	reportfolderPath=reportfolderPath.replace(":", "_").replace(" ", "_")
						//fr.close
						
						File file = new File("${katalonToolPath}\\configuration\\${customizedReportFolder}\\reportConfig.txt")
				 String dataFromFile=file.text
				 logger.info("${dataFromFile}")
				 
				 dataFromFile=dataFromFile.replace(":", "_").replace(" ", "_")
							logger.info("${dataFromFile}")
						publishHTML(target: [
									allowMissing: false,
									alwaysLinkToLastBuild: false,
									keepAll: true,
									reportDir: "automationproject/CustomizedReports/${customizedReportFolder}/${dataFromFile}",
									reportFiles: 'FinalReport.html',
									reportName: "HTML Report"
									])
									//reportFiles: '**/*/FinalReport.html',
						
						logger.info("Email stage...")
						sleep(20)				

			}
			
			
			
			
			
			
			
			stage('Push reports to git'){
						logger.info("Copying Latest report")
						 dir("./${config.KatalonRep}") {
						 def mkDir="mkdir C:\\Users\\pvelu\\.jenkins\\workspace\\Reports\\AutomationJunitReports\\Reports\\${config.KatalonRep}"
						 bat "${mkDir}"
						 sleep(4)
						 def copyFile="xcopy /s \"Reports\\*.*\" \"C:\\Users\\pvelu\\.jenkins\\workspace\\Reports\\AutomationJunitReports\\Reports\\${config.KatalonRep}\""
						 bat "${copyFile}"
						}
						
						//Fetch updated reports details
						logger.info("Fetching report from git")
							dir("C:\\Users\\pvelu\\.jenkins\\workspace\\Reports\\AutomationJunitReports") {
								def checkoutResult = git credentialsId: '35201f62-0720-4a44-8069-8b90aa4d287e', url: "https://Prabhaharan@bitbucket.org/gsihealth/automationjunitreports.git", changeLog:true
								task.checkoutResult = checkoutResult 
								sleep(10)
								bat 'cd..'							
							}
							logger.info("Commit report with new file")

						dir("C:\\Users\\pvelu\\.jenkins\\workspace\\Reports\\AutomationJunitReports") {
							bat 'git add -A'
							def gitcmd="git commit -m \"update\""
							bat "${gitcmd}"
							bat 'git push origin HEAD:master'
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