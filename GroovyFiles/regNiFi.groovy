import com.gsihealth.jenkins.Common
import com.gsihealth.jenkins.pojo.GsiJob
import com.gsihealth.jenkins.pojo.GsiRun
import com.gsihealth.jenkins.runner.GitBuildTask
import com.gsihealth.jenkins.utils.CommonUtils
import com.gsihealth.jenkins.utils.EmailListBuilder
import com.gsihealth.jenkins.utils.GitBuildSummary
import com.gsihealth.jenkins.utils.Logger

def call(body) {
def config = [:]
body.resolveStrategy = Closure.DELEGATE_FIRST
body.delegate = config
def Repo = config.Repo
Logger logger = new Logger()

body()
node{
//projectWorkspace = pwd()
GitBuildTask task = new GitBuildTask(env)
task.setConfig(config)
task.startDate = new Date()
//1st Stage
stage('Checkout from BitBucket'){
logger.info("[BUILD START]")
dir("${config.pytestRep}") {
def checkoutResult = git credentialsId: '3a1f3383-33f7-44e2-acac-7a02c80a0c61',branch: "${config.branch}", url: "${config.gitRepoURL}", changeLog:true
task.checkoutResult = checkoutResult 
sleep(10)
bat 'cd..'
}
}
//2nd Stage
stage('Python Test Execution'){
try{
dir("${config.pytestRep}/GSIPython_Framework"){
bat "python -m pytest"
currentBuild.result="SUCCESS"
status = true
bat 'cd..'
}
}
catch(Exception ex){
currentBuild.result="SUCCESS"
status = false
bat 'cd..'
}


}

//3rd Stage
stage('Publish Report'){
if("${config.disableEmail}"){
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
if("${buildLog} && ${buildLog}.length()!=0"){
DEFAULT_CONTENT="<br>=========CONSOLE LOG=========<br><br><pre>${buildLog}</pre>"
}else{
DEFAULT_CONTENT="PostMan execution failed for ${env.JOB_NAME}"
}    
if (status){    
emailext attachLog: true, body: "${currentBuild.result}: ${env.BUILD_URL}", compressLog: false, 
replyTo: 'prabhaharan.velu@gsihealth.com', subject: "Build Notification: ${env.JOB_NAME}-Build# ${env.BUILD_NUMBER} ${currentBuild.result}", to: 'prabhaharan.velu@gsihealth.com,Dinesh.Netaji@gsihealth.com'           
}else{
emailext attachLog: true, body: "${currentBuild.result}: ${env.BUILD_URL}", compressLog: false, 
replyTo: 'prabhaharan.velu@gsihealth.com', subject: "Build Notification: ${env.JOB_NAME}-Build# ${env.BUILD_NUMBER} ${currentBuild.result}", to: 'prabhaharan.velu@gsihealth.com,Dinesh.Netaji@gsihealth.com'
}
}
logger.info("[BUILD END]")
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

