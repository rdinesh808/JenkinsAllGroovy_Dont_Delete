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
	def apps =  ""
	def javaVersion = config.javaVersion?:"null"
	def serviceName = config.serviceName?:"null"
	def displayName = config.displayName?:"null"
	def appDir = config.appDirectory?:"null"
	def appPar = config.appPararameter?:"null"
	def start = config.start?:"null"
	def errlog = config.errlog?:"null"
	def outlog = config.outlog?:"null"
	def appAffinity = config.appAffinity?:"null"
	def AppRotateFiles = config.AppRotateFiles?:"null"
	def AppRotateOnline = config.AppRotateOnline?:"null"
	def AppRotateBytes = config.AppRotateBytes?:"null"
	def application = config.application?:"null"
	

    node {
GitBuildTask task = new GitBuildTask(env)
        task.setConfig(config)
        task.startDate = new Date()
        try{
		 stage('nexus download'){  
         echo "checking out executables from npm"
        if(("${config.component}"=="java")){
         def nexusMaven7 = "mvn org.apache.maven.plugins:maven-dependency-plugin:2.4:get -DremoteRepositories=nexus::::http://nexus:8081/repository/gsihealth-raw/ -Dartifact=com.gsihealth:jdk7:7:exe -Dtransitive=false -Ddest=jdk-7u80-windows-x64.exe -s D:\\jenkins_utils\\settings.xml -U"
         bat "${nexusMaven7}"
	     print "${nexusMaven7}"
	     def nexusMaven8 = "mvn org.apache.maven.plugins:maven-dependency-plugin:2.4:get -DremoteRepositories=nexus::::http://nexus:8081/repository/gsihealth-raw/ -Dartifact=com.gsihealth:jdk8:8:exe -Dtransitive=false -Ddest=jdk-8u181-windows-x64.exe -s D:\\jenkins_utils\\settings.xml -U"
         bat "${nexusMaven8}"
	     print "${nexusMaven8}"
		 }else if(("${config.component}"=="glassfishInstall")){
         def glassFish = "mvn org.apache.maven.plugins:maven-dependency-plugin:2.4:get -DremoteRepositories=nexus::::http://nexus:8081/repository/gsihealth-raw/ -Dartifact=com.gsihealth:glassfish:4.1.2:zip -Dtransitive=false -Ddest=glassfish4.zip -s D:\\jenkins_utils\\settings.xml -U"
         bat "${glassFish}"
		 def domain = "mvn org.apache.maven.plugins:maven-dependency-plugin:2.4:get -DremoteRepositories=nexus::::http://nexus:8081/repository/gsihealth-raw/ -Dartifact=com.gsihealth:domain:latest:xml -Dtransitive=false -Ddest=domain.xml -s D:\\jenkins_utils\\settings.xml -U"
         bat "${domain}"
	     print "${glassFish}"
        }else if(("${config.component}"=="mavenInstall")){
         def maven = "mvn org.apache.maven.plugins:maven-dependency-plugin:2.4:get -DremoteRepositories=nexus::::http://nexus:8081/repository/gsihealth-raw/ -Dartifact=com.gsihealth:apache-maven:3.6.0:zip -Dtransitive=false -Ddest=apache-maven-3.6.0-bin.zip -s D:\\jenkins_utils\\settings.xml -U"
         bat "${maven}"
	     print "${maven}"
        }else if(("${config.component}"=="glassfishUninstall")){
	     print "*****Java Uninstall starting******"
        }else if(("${config.component}"=="javaUninstall")){
	     print "*****GlassFish Uninstall starting******"
        }else if(("${config.component}"=="NodeUninstall")){
	     print "*****Nodejs Uninstall starting******"
        }else if(("${config.component}"=="mavenUninstall")){ 
	     print "*****Maven Uninstall starting******"
        }else if(("${config.component}"=="serviceDeletion")){ 
	     print "*****Service Deletion starting******"
        }else if(("${config.component}"=="serviceCreation")){
	     print "*****service creation starting******"
		 def nssm = "mvn org.apache.maven.plugins:maven-dependency-plugin:2.4:get -DremoteRepositories=nexus::::http://nexus:8081/repository/gsihealth-raw/ -Dartifact=com.gsihealth:nssm:2.22:exe -Dtransitive=false -Ddest=nssm.exe -s D:\\jenkins_utils\\settings.xml -U"
         bat "${nssm}"
        }else{
		def nodejs = "mvn org.apache.maven.plugins:maven-dependency-plugin:2.4:get -DremoteRepositories=nexus::::http://nexus:8081/repository/gsihealth-raw/ -Dartifact=com.gsihealth:nodejs8:8.11.1:msi -Dtransitive=false -Ddest=node-v8.11.1-x64.msi -s D:\\jenkins_utils\\settings.xml -U"
         bat "${nodejs}"
		}
       
       
		 
		}
          
             stage('Build'){

            //calling restart groovy
           def PS_deploy = "D:/jenkins_utils/deployment_v2/ps/AutomatedEnvironmentSetup.ps1 -env ${config.env} -environmentType ${config.environmentType} -javaVersion ${javaVersion} -component ${config.component} -serviceName ${serviceName} -displayName ${displayName} -appDir ${appDir} -appPar '${appPar}' -start ${start} -errlog ${errlog} -outlog ${outlog} -appAffinity ${appAffinity} -AppRotateFiles ${AppRotateFiles} -AppRotateOnline ${AppRotateOnline} -AppRotateBytes ${AppRotateBytes} -application ${application} -jobName ${env.JOB_NAME}"
                bat script: "${getPSCmd(PS_deploy)}"
				}
        }catch(e){
            print e
            echo "[BUILD END]"
            failure = true
        }

        //  if (failure) {
		//echo "sending mail for failure....."
		//notifyFailed()
		//}else{
       // echo "sending mail....."
		//notifyPassed()
		//}

       
		
    }
	//echo "Test results can be views from http://jenkins01:8989/job/${env.JOB_NAME}/test_results_analyzer/"
 

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
@NonCPS
def List<String> getDeployApps(){
    def deployApps = []
    def myBuildParams = currentBuild.rawBuild.getAction(ParametersAction.class)
    for(ParameterValue p in myBuildParams) {
        if(p.name.startsWith("Restart")){
            if(p.value==true){
                deployApps.add(p.name.split(" ")[1])
            }
        }
    }
    return deployApps
}

@NonCPS
def String replaceUrl( text, text2) {
    return text.replaceAll("Jenkins01:8989", text2)
}
@NonCPS
def String replaceAll( text, regex, newText) {
    return text.replaceAll(regex, newText)
}