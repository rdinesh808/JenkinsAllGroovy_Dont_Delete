import com.gsihealth.jenkins.Common
import com.gsihealth.jenkins.runner.GitBuildTask
import com.gsihealth.jenkins.utils.Logger
def call(body) {

    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    def failure = false
	def liquibaseStatus= false
	def buildEnvVars = []
	GitBuildTask task = new GitBuildTask(env)


    node {

        
		
		
            //Marks build start
            print "[BUILD START]"

            //Checkout the latest code
             dir("./Liquibase") {
			 stage('Liquibase code checkout'){
                def checkoutResult = git credentialsId: '60c19ca5-84bb-4508-a665-0c7895d3031b', url: "https://sadhasivim@bitbucket.org/gsihealth/Liquibase", changeLog:true
                task.checkoutResult = checkoutResult
				}
				
				try {
               
                    stage('Liquibase update excecution'){
                    if (config.java == 8) {
                        buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                    }
                  
							buildEnvVars.add("JAVA_HOME=${tool 'jdk8u121'}")
                            buildEnvVars.addAll(["PATH+MAVEN=${tool name: 'Maven 3.0.4', type: 'hudson.tasks.Maven$MavenInstallation'}/bin",
                                                 "MAVEN_OPTS=-XX:MaxPermSize=512m"])
												 
												
												 
												 
                          
						 
						   withEnv(buildEnvVars) {
                                                 def tagCmd="mvn liquibase:tag -Dliquibase.tag=\"Coordinator-5.4.2\""
												 def update="mvn clean install exec:java -Dspecification.operation=\"update\" -Dspecification.buildNo=\"${env.BUILD_NUMBER}\""	
												 def updateCmd="mvn liquibase:update"	
												
                            print "${tagCmd}"
							print "${update}"
						    print "${updateCmd}"
bat "${update}"	
                            	dir("D:\\Jenkins\\workspace\\${env.JOB_NAME}\\MySQL Utilities 1.6") {
                                //def sb = new StringBuilder()
						def rollbackScriptsPath="D:\\Jenkins\\workspace\\${env.JOB_NAME}\\liquibase\\scripts\\rollbackscripts\\drop${env.BUILD_NUMBER}.txt"
		                 File theInfoFiles = new File("${rollbackScriptsPath}")
                         if( !theInfoFiles.exists() ) {
                           println "File does not exist"
                         } else {
						 if(theInfoFiles.text){ 
                               def lines = theInfoFiles.readLines()
				for (String line : lines) {
                println(line)
                def str = line.split('\\.');
                for(def ele:str){
                print "${ele}"
                }
                
                //def compareCMD="mysqldbcompare --server1=liquibaseuser:Test123#@10.153.0.205 --server2=liquibaseuser:Test123#@10.153.0.205 ${schema}:${schema}copy --run-all-tests --changes-for=server1 --difftype=sql > ${env.BUILD_NUMBER}${schema}.txt 2>&1"                      
                def dumpCMD="D:\\temp\\mysqldump.exe -u liquibaseuser -h 10.153.0.205 -pTest123#  ${str[0]} ${str[1]} >> D:\\Jenkins\\workspace\\${env.JOB_NAME}\\liquibase\\scripts\\Rollback\\drop${env.BUILD_NUMBER}.sql"                      
              bat "${dumpCMD}"
               }
			   }else{
			   print "There is no drop queries in the given sql files"
			   }
               }	 
               }

                              
                            bat "${tagCmd}"							
							bat "${updateCmd}"
							
                         						
							liquibaseStatus = true
							}
			  }
                
            }catch (e) {
			   liquibaseStatus = false
			   echo "[BUILD END]"
			   print "liquibase execution failed"
			   print e
			}
          }  
        
		 if(liquibaseStatus){
		     print "***** DB SCRIPT EXECUTED SUCCESSFULLY *****"
			 dir("./Liquibase") {
			 bat 'git add -A'
		 def gitcmd="git commit -m \"update\""
		bat "${gitcmd}"
		  bat 'git push origin HEAD:master'
		  }
		 }else{
            print "***** DB SCRIPT EXECUTION FAILED *****"
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
		   if(buildLog && buildLog.length()!=0){
                DEFAULT_CONTENT="<br>=========CONSOLE LOG=========<br><br><pre>${buildLog}</pre>"
            }else{
			DEFAULT_CONTENT="liquibase execution failed for ${env.JOB_NAME}"
			}
			
   		   
		   emailext body: "${DEFAULT_CONTENT}", mimeType: 'text/html', subject: "${env.JOB_NAME} build failure", to: 'continuousdelivery@gsihealth.com,sadha.sivim@gsihealth.com,Murugesan.Nambi@gsihealth.com'
		   error("Build failed....")
		}
	} 
		
   }



@NonCPS
def notifyFailed(body) {
def bodyContent=body
mail (to: 'sadha.sivim@gsihealth.com,Murugesan.Nambi@gsihealth.com,prabhaharan.velu@gsihealth.com',
subject: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) has failed....",
body: "Job Failed ${bodyContent}.");

}
@NonCPS
def notifyPassed() {
mail (to: 'sadha.sivim@gsihealth.com,Murugesan.Nambi@gsihealth.com,prabhaharan.velu@gsihealth.com',
//mail (to: 'ContinuousDelivery@gsihealth.com',
 subject: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) has passed....",
 body: "Job passed ${env.BUILD_URL}.");
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
