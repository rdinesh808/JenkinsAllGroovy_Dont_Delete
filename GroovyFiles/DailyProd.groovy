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
def config = [:]
body.resolveStrategy = Closure.DELEGATE_FIRST
body.delegate = config
body()

def disableEmail=config.disableEmail
def currentjobPath
def failure = false
def status = false
def katalonToolPath=config.katalonToolPath
def testSuitePathLower
def gitRepoURL=config.gitRepoURL
def testSuiteCollectionPath	=config.testSuiteCollectionPath
def testSuitePath = config.testSuitePath
def KatalonRep = config.KatalonRep
def project=config.prjPath
def reportpath= config.reportpath
print "${testSuiteCollectionPath}".length()
print "${testSuitePath}".length()
def envList = getEnvNames()
def envName
def runAt=config.runAt
if(runAt.equals("local")){
testSuitePathLower = "Chrome (headless)"
}else{
testSuitePathLower=runAt.toLowerCase()
}
System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "")
print "${testSuitePathLower}"	
def projectWorkspace

node {
projectWorkspace = pwd()
GitBuildTask task = new GitBuildTask(env)
task.setConfig(config)
task.startDate = new Date()

//1st Stage
stage('Checkout from BitBucket'){
logger.info("[BUILD START]")
dir("${config.KatalonRep}") {
def checkoutResult = git credentialsId: '3a1f3383-33f7-44e2-acac-7a02c80a0c61',branch: "${config.branch}", url: "${config.gitRepoURL}", changeLog:true
task.checkoutResult = checkoutResult 
sleep(10)
bat 'cd..'
}
dir("${reportpath}") {
deleteDir()
}
dir("${reportpath}") {
def checkoutResult = git credentialsId: '3a1f3383-33f7-44e2-acac-7a02c80a0c61',branch: "master", url: "https://Prabhaharan@bitbucket.org/gsihealth/monitoring_test_results.git", changeLog:true
task.checkoutResult = checkoutResult 
sleep(10)
bat 'cd..'
}
dir("${KatalonRep}/Reports") {
deleteDir()	
}
dir("${projectWorkspace}/CustomizedReports") {
deleteDir()	
}
dir("${katalonToolPath}/screenshots") {
deleteDir()	
}
}
//2nd Stage
for (def i = 0; i < envList.size(); i++) {
//def data = [:]
envName = envList.get(i)
echo "${envName}"
stage("Test Execution For : ${envName} and Push the reports to BitBucket"){
def katalonCmd
catchError {
try{
dir("${projectWorkspace}") {
if("${testSuiteCollectionPath}".length() >0){
katalonCmd = "${katalonToolPath}\\katalon.exe -runMode=console -projectPath=\"${project}\" -reportFolder=\"${projectWorkspace}/Reports\" -reportFileName=\"report\" -retry=0 -testSuiteCollectionPath=\"Test Suites/${config.testSuiteCollectionPath}\""
}else if("${envName}".length() >0){
katalonCmd = "${katalonToolPath}\\katalon.exe -runMode=console -projectPath=\"${project}\" -reportFolder=\"${projectWorkspace}/Reports\" -reportFileName=\"report\" -retry=0 -testSuitePath=\"Test Suites/Environments/${envName}\" -browserType=\"${testSuitePathLower}\""							 
}else{
logger.info("configuration error. please check the configuration")
error("Build failed....")
status = false
}		                   
logger.info("${katalonCmd}")
bat "${katalonCmd}"
status = true
}
currentjobPath = "${env.JENKINS_HOME}"
PS_COPY  = "D:/ps/htmltosharesdailyprod.ps1" + " -jobPath '${currentjobPath}' -jobName '${env.JOB_NAME}' -envName '${envName}'"
bat script: "${getPSCmd(PS_COPY)}"
}
catch(e){
print "The Katalon error is ${e}"
echo "[BUILD END]"
status = false
currentjobPath = "${env.JENKINS_HOME}"
PS_COPY  = "D:/ps/htmltosharesdailyprod.ps1" + " -jobPath '${currentjobPath}' -jobName '${env.JOB_NAME}' -envName '${envName}'"
bat script: "${getPSCmd(PS_COPY)}"
if(!status){
try{
ScreenShot_COPY  = "D:/ps/screenshotdailyprod.ps1" + " -jobPath '${currentjobPath}' -jobName '${env.JOB_NAME}'"
bat script: "${getPSCmd(ScreenShot_COPY)}"
}catch (msg){
echo "msg"
}
}
}
}

}
//push to bitbucket
def fileDir = new File("${projectWorkspace}/CustomizedReports").listFiles().first()
def folderName="${fileDir}"
def reportFile="xcopy /s /Q /F /Y \"${projectWorkspace}/CustomizedReports/*.html\" \"${reportpath}/DailProd/\""
bat "${reportFile}"
print "fileDir : ${fileDir}"
print "folderName : ${folderName}"
dir("${reportpath}") {    
bat 'git config --global user.name "Prabhaharan Velu"'
bat 'git config --global user.email prabhaharan.velu@gsihealth.com'
bat 'git pull origin master'
bat 'git add -A'
def gitcmd="git commit -m ${envName}_DailyProd_Result_Update"
bat "${gitcmd}"                
bat 'git push -f origin HEAD:master'
}
}
//3rd Satge
stage('Publish Report'){
def fileDir = new File("${projectWorkspace}/CustomizedReports").listFiles().first()
def folderName="${fileDir}"
def reportFile="xcopy /s /Q /F /Y \"${projectWorkspace}/CustomizedReports/*.html\" \"${reportpath}/DailProd/\""
//bat "${reportFile}"
print "fileDir : ${fileDir}"
print "folderName : ${folderName}"
publishHTML(target: [
allowMissing: false,
alwaysLinkToLastBuild: false,
keepAll: true,
reportDir: "${projectWorkspace}/CustomizedReports",
reportFiles: "*.html",
reportName: "HTML Report"
])	   

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
//5th Stage
/*stage('Push the reports to BitBucket'){
dir("${reportpath}") {    
bat 'git config --global user.name "Prabhaharan Velu"'
bat 'git config --global user.email prabhaharan.velu@gsihealth.com'
bat 'git pull origin master'
bat 'git add -A'
def gitcmd="git commit -m DailyProd_Result_Update"
bat "${gitcmd}"                
bat 'git push -f origin HEAD:master'
}
}*/

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

@NonCPS
def String lastMatch(text, regex) {
def m;
if ((m = text =~ regex)) {
return m[0][-1]
}
return ''
}

@NonCPS
def String replaceAll( text, regex, newText) {
return text.replaceAll(regex, newText)
}
@NonCPS
def List<String> getEnvNames(){
    def envNames = []
    def myBuildParams = currentBuild.rawBuild.getAction(ParametersAction.class)
    for(ParameterValue p in myBuildParams) {
        if(p.value==true){
                envNames.add((p.name))
            }  
    }
    return envNames
}

@NonCPS
def String getPSCmd(command){
def prefix = "powershell.exe -command \""
def suffix = "\";exit \$LASTEXITCODE;"
return prefix+command+suffix
}