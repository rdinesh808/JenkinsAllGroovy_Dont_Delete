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


        dir("./gsiflow-params-${env.environment}") {
			 stage('nifi Config Import'){
                def checkoutResult = git credentialsId: '60c19ca5-84bb-4508-a665-0c7895d3031b', url: "https://sadhasivim@bitbucket.org/gsihealth/gsiflow-params-${env.JOB_NAME}.git", changeLog:true
                task.checkoutResult = checkoutResult
				}
		}
        stage('Deploying nifi process group properties'){
        try{
		if(("${environment}"=="Dev")){
        	String batCmdDev="D:\\Jenkins\\workspace\\${env.JOB_NAME}\\gsiflow-params-${env.environment}\\devconfig.bat"
	        println "${batCmdDev}"
	        bat "${batCmdDev}"
        } else if(("${environment}"=="Test")){
        	String batCmdTest="D:\\Jenkins\\workspace\\${env.JOB_NAME}\\gsiflow-params-${env.environment}\\qaconfig.bat"
	        println "${batCmdTest}"
	        bat "${batCmdTest}"
        }else if(("${environment}"=="Prod")){
		
		   String batCmdProd="D:\\Jenkins\\workspace\\${env.JOB_NAME}\\gsiflow-params-${env.environment}\\prodconfig.bat"
	       println "${batCmdProd}"
	       bat "${batCmdProd}" 
		
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
