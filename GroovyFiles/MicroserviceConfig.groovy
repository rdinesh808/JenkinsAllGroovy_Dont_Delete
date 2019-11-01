import com.gsihealth.jenkins.Common
import com.gsihealth.jenkins.pojo.GsiJob
import com.gsihealth.jenkins.pojo.GsiRun
import com.gsihealth.jenkins.runner.GitBuildTask
import com.gsihealth.jenkins.utils.CommonUtils
import com.gsihealth.jenkins.utils.EmailListBuilder
import com.gsihealth.jenkins.utils.GitBuildSummary
import com.gsihealth.jenkins.utils.Logger
import java.text.SimpleDateFormat


def call(body) {


    Logger logger = new Logger()

    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    def failure = false
	def svnBranch = config.svnBranch
	def timestamp = config.timestamp?:""


    node {

        GitBuildTask task = new GitBuildTask(env)
        task.setConfig(config)
        task.startDate = new Date()
       def pathAppend = config.path?:""
	   def appName = config.appName?:""
	   def pomVersion = config.pomVersion?:""
	   def packageType = config.packageType?:""
	   def date = new Date()
       sdf = new SimpleDateFormat("yyyyMMddHHmm")
	   def currTime=sdf.format(date)
       println sdf.format(date)
        stage('Code checkout'){
            //Marks build start
            logger.info("[BUILD START]")

            //Checkout the latest code
            dir("./${config.name}") {
               // def checkoutResult = git credentialsId: '60c19ca5-84bb-4508-a665-0c7895d3031b', url: "https://sadhasivim@bitbucket.org/gsihealth/${config.name}", changeLog:true
				//def checkoutResult = git credentialsId: '60c19ca5-84bb-4508-a665-0c7895d3031b', url: "https://sadhasivim@bitbucket.org/gsihealth/${config.name}/${branch}", changeLog:true
				
				
				def checkoutResult = git branch: "${svnBranch}", credentialsId: 'cf7a0040-05b2-4835-b49e-8e5814141f5b', url: "https://sadhasivim@bitbucket.org/gsihealth/${config.name}", changeLog:true

				
				//def repo_url = config.repository_url?:"https://subversion.assembla.com/svn/gsihealth.umbrella/${config.svnBranch}/${config.repository}"
				
                print "${pathAppend}"
                task.checkoutResult = checkoutResult
            }
        }

        stage('Build') {

            catchError {
                dir("./${config.name}/${pathAppend}") {

                    if (!config.type) {
                        logger.info("WARNING: The build type is not specified. Using Maven...")
                        config.type = "maven"
                    }

                    def buildEnvVars = []
                    if (config.java == 8) {
                        buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                    } else if (config.java == 7) {
                        buildEnvVars.add("JAVA_HOME=${tool 'jdk7u71'}")
                    }
                    switch (config.type) {
                        case 'ant':
                            String goal = config.goal ?: "clean war"
                            buildEnvVars.add("PATH+ANT=${tool name: 'ANT 1.8.1', type: 'hudson.tasks.Ant$AntInstallation'}/bin")
                            withEnv(buildEnvVars) {
                                bat "ant ${goal}"
                            }
                            break
                        case 'maven':
						def nexusMavenCmd=""
                            String goal = config.goal ?: "clean install -s D:\\jenkins_utils\\settings.xml -U"
                            buildEnvVars.addAll(["PATH+MAVEN=${tool name: 'Maven 3.0.4', type: 'hudson.tasks.Maven$MavenInstallation'}/bin",
                                                 "MAVEN_OPTS=-XX:MaxPermSize=512m"])
                            withEnv(buildEnvVars) {
                                bat "mvn ${goal} -Dmaven.repo.local=${config.mvnRepo}"
                            if (config.timestamp == "true") {
							
                                nexusMavenCmd = "mvn deploy:deploy-file -DgroupId=com.gsihealth -DartifactId=${appName} -Dversion=${pomVersion} -DgeneratePom=true -DupdateReleaseInfo=true -Dpackaging=${currTime}.${packageType}  -DrepositoryId=nexus -Durl=http://nexus:8081/repository/gsihealth-maven-releases/ -Dfile=target\\${appName}-${pomVersion}.${packageType} -s D:\\jenkins_utils\\settings.xml"
                                bat "${nexusMavenCmd}"
                                print "${nexusMavenCmd}"
                            } 
                            }
                            break
                        case 'Maven 3.3.9':
                            String goal = config.goal ?: "clean install -s D:\\jenkins_utils\\settings.xml -U"
                            buildEnvVars.addAll(["PATH+MAVEN=${tool name: 'Maven 3.3.9', type: 'hudson.tasks.Maven$MavenInstallation'}/bin",
                                                 "MAVEN_OPTS=-XX:MaxPermSize=512m"])
                            withEnv(buildEnvVars) {
                                bat "mvn ${goal} -Dmaven.repo.local=${config.mvnRepo}"
								
                            }
                            break                            
                        case 'node':
                            String goal = config.goal ?: "app c"
                            bat "npm install"
                            bat "node ${goal}"
                            break

                    }

                }

                //TODO: Could be enhanced. Temporary patch for Message app which requires post-build ANT call to rename the war. just for msg app
                if (config.post_build) {

                    stage 'Post-Build'
                    dir("./${config.name}") {
                        withEnv(["PATH+ANT=${tool name: 'ANT 1.8.1', type: 'hudson.tasks.Ant$AntInstallation'}/bin"]) {
                            bat "${config.post_build}"
                        }
                    }

                }

                logger.info("Archive stage...")
                switch (config.type) {
                    case 'ant':
                        archive '*/dist/*.war, */dist/*.zip, **/target/*.war'
                        break
                    case 'maven':
                        archive '**/target/*.war, **/target/*.zip, **/target/*.jar,**/target/*.nar'
                        break
                    case 'maven1':
                        archive '**/target/*.war, **/target/*.zip, **/target/*.jar'
                        break                       
                        
                    case 'node':
                        archive '**/*.zip'
                        break
					case 'test':
					archive '/**/**/**/*.*'
					//archive '.properties, .bat'
				
					
                }
            }
        }
		

		
 
        stage('Post Build'){
                logger.info("Email stage...")
//
                try{
                    logger.info("build completed.")
                    task.endDate = new Date()
                    task.duration = CommonUtils.getTimeDelta(task.endDate, task.startDate)
                    task.result = currentBuild.result?:"SUCCESS"
//
                    currentBuild.displayName = "#${env.BUILD_NUMBER}_${config.release}"
                    currentBuild.description = "Revision: ${task.checkoutResult.GIT_COMMIT}"
//
                    def requested = CommonUtils.getRequestedUser(steps)
//                    def culprits = CommonUtils.getCulprits(steps)
                    def developers = CommonUtils.getDevelopers(steps)
                    def to = new EmailListBuilder(steps)
//                        .requestedUser()
//                        .culprits()
//                        .developers()
                        .CDTeam()
                        .DevLead()
                        .build()
                    task.setDevelopers(null)
                    task.setRequestedUser(requested)
                    task.setCulprits(null)
//
                    logger.info("Started by: ${requested}")
//                    logger.info("Responsible Developers: ${culprits}")
                    logger.info("Developers: ${developers}")
//
                    logger.info("to: ${to}")
//
                    task.setRun(new GsiRun(
                            parent: new GsiJob(name: env.JOB_NAME),
                            displayName: currentBuild.displayName,
                            number: currentBuild.number
                        )
                    )
//
                    GitBuildSummary buildSummary = new GitBuildSummary(steps, env, task)
//
                    logger.info(task)

                    if(!config.disableEmail){
                        buildSummary.sendEmail(to, true)
                    }
//
                }catch(e){
                    print e
                    emailext body: "Script has some error! ${e}", subject: "Jenkins warning:${env.JOB_NAME} Build ${currentBuild.displayName}", to: 'ContinuousDelivery@gsihealth.com'
                }

            }

    }


}

