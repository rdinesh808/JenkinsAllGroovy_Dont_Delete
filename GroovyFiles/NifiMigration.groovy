import com.gsihealth.jenkins.Common
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import jenkins.model.Jenkins;
import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic
import com.gsihealth.jenkins.pojo.GsiJob
import com.gsihealth.jenkins.pojo.GsiRun
import org.json.JSONArray;
import org.json.JSONObject;
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
	def bucketname = config.bucketname
	def flowName = config.flowName
	def version = config.version


    node {

        GitBuildTask task = new GitBuildTask(env)
        task.setConfig(config)
        task.startDate = new Date()
        def pathAppend = config.path?:""
      
	    bat "D:\\Sadha\\nifi-toolkit-1.9.2-bin\\nifi-toolkit-1.9.2\\bin\\cli.bat registry list-buckets -u http://devzk01.madc.local:61080 -ot json > test.json"
		
		def jsonTextval=getFileText("${env.JOB_NAME}")
		def json = readFromJson(jsonTextval)

        def bucketID=iterate(json, bucketname)
		print "${bucketID}"
		bat "D:\\Sadha\\nifi-toolkit-1.9.2-bin\\nifi-toolkit-1.9.2\\bin\\cli.bat registry list-flows -u http://devzk01.madc.local:61080 --bucketIdentifier ${bucketID} -ot json > test.json"
		
		def jsonFlowval=getFileText("${env.JOB_NAME}")
		def jsonFlow = readFromJson(jsonFlowval)
		
		def flowID=iterate(jsonFlow, flowName)
		print "${flowID}"
		
		bat "D:\\Sadha\\nifi-toolkit-1.9.2-bin\\nifi-toolkit-1.9.2\\bin\\cli.bat nifi pg-import --bucketIdentifier ${bucketID} --flowIdentifier ${flowID} -fv ${version} -u http://10.168.54.68:9090"
		
		


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
def String getFileText(folder){

File f = new File("D:\\Jenkins\\workspace\\${folder}\\test.json")
        def slurper = new JsonSlurperClassic()
        def jsonText = f.getText()
 return jsonText
}
		
		
@NonCPS
def String getPSCmd(command){
    def prefix = "powershell.exe -command \""
    def suffix = "\";exit \$LASTEXITCODE;"
    return prefix+command+suffix
}

@NonCPS
def String iterate(data,bucketName){
def bucketId
 data.each {
 println it.name 
 if(it.name == "${bucketName}"){
  bucketId=it.identifier
 }
 
 }
 return bucketId
}

@NonCPS
def readFromJson(data){
    def jsonSlurper = new JsonSlurperClassic()
    return jsonSlurper.parseText(data)
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
