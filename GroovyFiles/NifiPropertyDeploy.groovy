import com.gsihealth.jenkins.Common
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import jenkins.model.Jenkins;
import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic
import com.gsihealth.jenkins.pojo.GsiJob
import com.gsihealth.jenkins.pojo.GsiRun
import com.gsihealth.jenkins.runner.GitBuildTask
import com.gsihealth.jenkins.utils.CommonUtils
import com.gsihealth.jenkins.utils.EmailListBuilder
import com.gsihealth.jenkins.utils.GitBuildSummary
import com.gsihealth.jenkins.utils.Logger
import static groovy.io.FileType.FILES


def call(body) {


    Logger logger = new Logger()

    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    def failure = false
	def ip = config.ip
	def nifiVersion = config.nifiVersion
	def environment = config.env


    node {

        GitBuildTask task = new GitBuildTask(env)
        task.setConfig(config)
        task.startDate = new Date()
        def pathAppend = config.path?:""


        dir("./gsiflow-properties") {
			 stage('nifi Config Import'){
                def checkoutResult = git credentialsId: '60c19ca5-84bb-4508-a665-0c7895d3031b', url: "https://sadhasivim@bitbucket.org/gsihealth/gsiflow-properties.git", changeLog:true
                task.checkoutResult = checkoutResult
				}
		}
        stage('Deploying nifi properties'){
        try{
		if(("${environment}"=="Dev")){
        	String setaccessCommandDev = "echo y | plink cdnonprod@Devzk01 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\ChangeFileUser.sh"
            bat "${setaccessCommandDev}"
			String deletePropsCommandDev = "echo y | plink cdnonprod@Devzk01 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\DeleteNifiProps.sh"
            bat "${deletePropsCommandDev}"
        	String remoteGetCommandDev = "echo y | D:\\PuTTY\\pscp.exe -pw P@ssw0rd1 -r D:\\Jenkins\\workspace\\${env.JOB_NAME}\\gsiflow-properties\\nifi-dev.properties cdnonprod@Devzk01:/usr/hdf/${nifiVersion}/nifi/conf"
            bat "${remoteGetCommandDev}"  
        	String setaccessCommand01Dev = "echo y | plink cdnonprod@Devzk02 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\ChangeFileUser.sh"
            bat "${setaccessCommand01Dev}"
			String deletePropsCommand01Dev = "echo y | plink cdnonprod@Devzk02 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\DeleteNifiProps.sh"
            bat "${deletePropsCommand01Dev}"
        	String remoteGetCommand01Dev = "echo y | D:\\PuTTY\\pscp.exe -pw P@ssw0rd1 -r D:\\Jenkins\\workspace\\${env.JOB_NAME}\\gsiflow-properties\\nifi-dev.properties cdnonprod@Devzk02:/usr/hdf/${nifiVersion}/nifi/conf"
            bat "${remoteGetCommand01Dev}"  
        	String setaccessCommand02Dev = "echo y | plink cdnonprod@Devzk03 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\ChangeFileUser.sh"
            bat "${setaccessCommand02Dev}"
			String deletePropsCommand02Dev = "echo y | plink cdnonprod@Devzk03 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\DeleteNifiProps.sh"
            bat "${deletePropsCommand02Dev}"
        	String remoteGetCommand02Dev = "echo y | D:\\PuTTY\\pscp.exe -pw P@ssw0rd1 -r D:\\Jenkins\\workspace\\${env.JOB_NAME}\\gsiflow-properties\\nifi-dev.properties cdnonprod@Devzk03:/usr/hdf/${nifiVersion}/nifi/conf"
            bat "${remoteGetCommand02Dev}" 
        }else 	{
		
		    String setaccessCommandQa = "echo y | plink cdnonprod@10.168.54.67 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\ChangeFileUser.sh"
            bat "${setaccessCommandQa}"
			String deletePropsCommandQa = "echo y | plink cdnonprod@10.168.54.67 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\DeleteNifiProps.sh"
            bat "${deletePropsCommandQa}"
        	String remoteGetCommandQa = "echo y | D:\\PuTTY\\pscp.exe -pw P@ssw0rd1 -r D:\\Jenkins\\workspace\\${env.JOB_NAME}\\gsiflow-properties\\nifi-qa.properties cdnonprod@10.168.54.67:/usr/hdf/${nifiVersion}/nifi/conf"
            bat "${remoteGetCommandQa}"  
        	String setaccessCommand01Qa = "echo y | plink cdnonprod@10.168.54.68 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\ChangeFileUser.sh"
            bat "${setaccessCommand01Qa}"
			String deletePropsCommand01Qa = "echo y | plink cdnonprod@10.168.54.68 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\DeleteNifiProps.sh"
            bat "${deletePropsCommand01Qa}"
        	String remoteGetCommand01Qa = "echo y | D:\\PuTTY\\pscp.exe -pw P@ssw0rd1 -r D:\\Jenkins\\workspace\\${env.JOB_NAME}\\gsiflow-properties\\nifi-qa.properties cdnonprod@10.168.54.68:/usr/hdf/${nifiVersion}/nifi/conf"
            bat "${remoteGetCommand01Qa}"  
        	String setaccessCommand02Qa = "echo y | plink cdnonprod@10.168.54.69 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\ChangeFileUser.sh"
            bat "${setaccessCommand02Qa}"
			String deletePropsCommand02Qa = "echo y | plink cdnonprod@10.168.54.69 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\DeleteNifiProps.sh"
            bat "${deletePropsCommand02Qa}"
        	String remoteGetCommand02Qa = "echo y | D:\\PuTTY\\pscp.exe -pw P@ssw0rd1 -r D:\\Jenkins\\workspace\\${env.JOB_NAME}\\gsiflow-properties\\nifi-qa.properties cdnonprod@10.168.54.69:/usr/hdf/${nifiVersion}/nifi/conf"
            bat "${remoteGetCommand02Qa}" 
		
		}		
        }
        catch(e){
        print e
        }
        }
		stage('Restarting Nifi'){
        try{
		if(("${environment}"=="Dev")){
        	String restartCommandDev = "echo y | plink cdnonprod@Devzk01 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\NifiRestart.sh"
            bat "${restartCommandDev}"  
        	String restartCommand01Dev = "echo y | plink cdnonprod@Devzk02 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\NifiRestart.sh"
            bat "${restartCommand01Dev}" 
        	String restartCommand02Dev = "echo y | plink cdnonprod@Devzk03 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\NifiRestart.sh"
            bat "${restartCommand02Dev}" 
        }else {
		
		    String restartCommandQa = "echo y | plink cdnonprod@10.168.54.67 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\NifiRestart.sh"
            bat "${restartCommandQa}"  
        	String restartCommand01Qa = "echo y | plink cdnonprod@10.168.54.68 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\NifiRestart.sh"
            bat "${restartCommand01Qa}" 
        	String restartCommand02Qa = "echo y | plink cdnonprod@10.168.54.69 -pw P@ssw0rd1 -m D:\\jenkins_utils\\deployment_v2\\bash\\NifiRestart.sh"
            bat "${restartCommand02Qa}" 
		}
		
        }
        catch(e){
        print e
        }
        }
		/*
        stage('Post Build'){
               
			   logger.info("Email stage...")

                try{
                    logger.info("build completed.")
                    task.endDate = new Date()
                    task.duration = CommonUtils.getTimeDelta(task.endDate, task.startDate)
                    task.result = currentBuild.result?:"SUCCESS"

                    currentBuild.displayName = "#${env.BUILD_NUMBER}_${config.release}"
                    currentBuild.description = "Revision: ${task.checkoutResult.GIT_COMMIT}"

                    def requested = CommonUtils.getRequestedUser(steps)
                    def culprits = CommonUtils.getCulprits(steps)
                    def developers = CommonUtils.getDevelopers(steps)
                    def to = new EmailListBuilder(steps)
                        .requestedUser()
                        .culprits()
                        .developers()
                        .CDTeam()
                        .DevLead()
                        .build()
                    task.setDevelopers(null)
                    task.setRequestedUser(requested)
                    task.setCulprits(null)

                    logger.info("Started by: ${requested}")
                    logger.info("Responsible Developers: ${culprits}")
                    logger.info("Developers: ${developers}")

                    logger.info("to: ${to}")

                    task.setRun(new GsiRun(
                            parent: new GsiJob(name: env.JOB_NAME),
                            displayName: currentBuild.displayName,
                            number: currentBuild.number
                        )
                    )

                    GitBuildSummary buildSummary = new GitBuildSummary(steps, env, task)

                    logger.info(task)

                    if(!config.disableEmail){
                        buildSummary.sendEmail(to, true)
                    }

                }catch(e){
                    print e
                    emailext body: "Script has some error! ${e}", subject: "Jenkins warning:${env.JOB_NAME} Build ${currentBuild.displayName}", to: 'ContinuousDelivery@gsihealth.com'
                }

            }
			*/

    }


}


@NonCPS
def String getPSCmd(command){
    def prefix = "powershell.exe -command \""
    def suffix = "\";exit \$LASTEXITCODE;"
    return prefix+command+suffix
}


@NonCPS
String runAndWait(Object cmd){
echo "$cmd" 
    def process = cmd.execute()
	echo "$process"
    def output = new StringWriter()
    def error = new StringWriter()
    //wait for process ended and catch stderr and stdout 
	echo "comment of india"
    process.waitForProcessOutput(output, error)
    //check there is no error
    //assert error.toString().trim().size()==0: "$error"
    //assert procss.exitValue()==0 //we can do check with error code
    //return stdout from closure 
	echo "$error"
	echo output.toString()
    return output.toString()
}
