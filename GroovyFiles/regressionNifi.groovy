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
	def environment=config.env
	def single = config.SinglerunName
	def gitRepoURL=config.gitRepoURL
	def reportConfigPathPath=config.reportConfigPathPath
	def testSuiteCollectionPath =config.testSuiteCollectionPath
	def testSuiteReportConfigPath = config.testSuitePath
	def testSuitePath = config.testSuitePath
	def KatalonRep=config.KatalonRep
	def testResults=config.testResults
	def project=config.prjPath
	def clientConfigPath=config.clientConfigPath
	def regressionType=config.regressionType
	def utilityAppDIR=config.dataUtilityAppPath
	//Date d=new Date();
	//def runHTMLReportName=${environment}+d.toString().replace(":", "_").replace(" ", "_")+".html";
	print "${regressionType}"
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
					def checkoutResult = git credentialsId: '3a1f3383-33f7-44e2-acac-7a02c80a0c61',branch: "${config.branch}", url: "${config.gitRepoURL}", changeLog:true
					task.checkoutResult = checkoutResult 
					sleep(10)
					bat 'cd..'
				} 
			}
                    
			

			stage('Test Execution'){
			  def katalonCmd
				catchError {
						dir("${KatalonRep}/Reports") {
							deleteDir()
						  }
						dir("${projectWorkspace}/${regressionType}") {
							deleteDir()
						}
						dir("${projectWorkspace}/CustomizedReports") {
							deleteDir()
						}
						dir("${katalonToolPath}/screenshots") {
							deleteDir()
						}
						try{
							dir("${projectWorkspace}") {
								if("${testSuiteCollectionPath}".length() >0){
									katalonCmd = "${katalonToolPath}\\katalon.exe -runMode=console -projectPath=\"${project}\" -reportFolder=\"${projectWorkspace}/Reports\" -reportFileName=\"report\" -retry=0 -testSuiteCollectionPath=\"Test Suites/${config.testSuiteCollectionPath}\" -email=prabhaharan.velu@gsihealth.com -password=Test123#"
								}else if("${testSuitePath}".length() >0){
									katalonCmd = "${katalonToolPath}\\katalon.exe -runMode=console -projectPath=\"${project}\" -reportFolder=\"${projectWorkspace}/Reports\" -reportFileName=\"report\" -retry=0 -testSuitePath=\"Test Suites/${config.testSuitePath}\" -browserType=\"${testSuitePathLower}\" -email=prabhaharan.velu@gsihealth.com -password=Test123#"
							 
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
			
			/*stage('Publish Junit Report'){
				logger.info("Post Build-TestResult...")
				sleep(20)
				step([$class: 'JUnitResultArchiver', testResults: "${testResults}"])
				print "${testResults}"
			*/
			
			//echo runHTMLReportName
			stage('Publish Report'){
			
				/*//publishHTML(target: [
				//allowMissing: false,
				//alwaysLinkToLastBuild: true,
				//keepAll: false,
				//reportDir: "${projectWorkspace}/CustomizedReports",
				//reportFiles: "*.html",
				//reportName: "HTML Report"
				])*/
							
			   
						
			   
						
			if(!disableEmail){
				try{
                    logger.info("build completed.")
					if(status){
								emailext attachmentsPattern: "**/*.html", body: "${env.JOB_NAME} - Post Deployment Succeed", mimeType: 'text/html', subject: "${env.JOB_NAME} - Post Deployment Succeed", to: 'prabhaharan.velu@gsihealth.com,dinesh.netaji@gsihealth.com,mohan.raj@gsihealth.com,vigneshwar.thirumal@gsihealth.com,shiva.kumar@gsihealth.com,dayanidhi.kasi@gsihealth.com,balakumar.thirumurugan@gsihealth.com,renu.rajan@gsihealth.com,sundar.raj@gsihealth.com,raghu.nedumpurath@gsihealth.com,ranga.mannar@gsihealth.com,arul.selvar@gsihealth.com'
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
							emailext attachmentsPattern: "**/*.html", body: "${DEFAULT_CONTENT}", mimeType: 'text/html', subject: "${env.JOB_NAME} - Post Deployment failed", to: 'prabhaharan.velu@gsihealth.com,dinesh.netaji@gsihealth.com,mohan.raj@gsihealth.com,vigneshwar.thirumal@gsihealth.com,shiva.kumar@gsihealth.com,dayanidhi.kasi@gsihealth.com,balakumar.thirumurugan@gsihealth.com,renu.rajan@gsihealth.com,sundar.raj@gsihealth.com,raghu.nedumpurath@gsihealth.com,ranga.mannar@gsihealth.com,arul.selvar@gsihealth.com'
					}
                }catch(e){
					echo "[BUILD END]"
                    print e
                    emailext body: "Script has some error! ${e}", subject: "Jenkins warning:${env.JOB_NAME} Build ${currentBuild.displayName}", to: 'prabhaharan.velu@gsihealth.com,dinesh.netaji@gsihealth.com,mohan.raj@gsihealth.com,vigneshwar.thirumal@gsihealth.com,shiva.kumar@gsihealth.com,dayanidhi.kasi@gsihealth.com,balakumar.thirumurugan@gsihealth.com,renu.rajan@gsihealth.com,sundar.raj@gsihealth.com,raghu.nedumpurath@gsihealth.com,ranga.mannar@gsihealth.com,arul.selvar@gsihealth.com'
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


@NonCPS
def String getPSCmd(command){
    def prefix = "powershell.exe -command \""
    def suffix = "\";exit \$LASTEXITCODE;"
    return prefix+command+suffix
}

