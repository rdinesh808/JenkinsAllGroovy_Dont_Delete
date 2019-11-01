import com.gsihealth.jenkins.Common
import com.gsihealth.jenkins.runner.GitBuildTask
import com.gsihealth.jenkins.utils.Logger
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import jenkins.model.Jenkins;
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.PUT
import static groovyx.net.http.Method.GET
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.TEXT
import groovy.json.JsonSlurperClassic
import groovy.json.StringEscapeUtils
import groovy.util.XmlSlurper
import groovy.json.JsonOutput
import groovyx.net.http.*
import groovy.xml.MarkupBuilder 
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.DELETE
import static groovyx.net.http.ContentType.XML
import static groovyx.net.http.ContentType.JSON
import java.text.SimpleDateFormat




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

        def branch = config.branch
        def repo = config.repo
		
		
            //Marks build start
            print "[BUILD START]"

            //Checkout the latest code
           
			 stage('Liquibase code checkout'){
                def checkoutResult = git branch:"${branch}", credentialsId: '60c19ca5-84bb-4508-a665-0c7895d3031b', url: "https://sadhasivim@bitbucket.org/gsihealth/${repo}.git", changeLog:true
                task.checkoutResult = checkoutResult
				}
				stage('History version checkout'){
				dir("./sql-version-history") {	 
                def checkoutResult = git branch:"${branch}", credentialsId: '60c19ca5-84bb-4508-a665-0c7895d3031b', url: "https://sadhasivim@bitbucket.org/gsihealth/sql-version-history.git", changeLog:true
                task.checkoutResult = checkoutResult
				}
				}
				def urlUpdateStatus = ""
				try {
					def id = getRequestedId()
					def date = new Date()
                    sdf = new SimpleDateFormat("MMddyyyyHHmmss")
                    println sdf.format(date)
	
		
               
                    print "${id}"
                    def url = "http://10.168.54.72:8080/config/job/${id}"  
                    urlUpdateStatus = "http://10.168.54.72:8089/clientconfig/jobs/${id}"					
                    print "${url}"	
                    					
					
					def valueMap  = getService(url)
					sleep 1
					
					def connect = valueMap.get("connect")
					def crosscommunity = valueMap.get("crosscommunity")
					def filemanager = valueMap.get("filemanager")
					def profile = valueMap.get("profile")
					def status = valueMap.get("status")
					
					def queryMap = [ : ] 
					queryMap.put("connect","${connect}")
					queryMap.put("crosscommunity","${crosscommunity}")
					queryMap.put("filemanager","${filemanager}")
					stage("Liquibase Update"){
					
					
					queryMap.each { key, val ->
                    print "Schema = ${key}, Query = ${val}"
					  
					 File sqlFile = new File("D:\\Jenkins\\workspace\\${env.JOB_NAME}\\approved\\Query.sql")
	                if(sqlFile.exists()){
	                 sqlFile.delete()
	                 sqlFile = new File("D:\\Jenkins\\workspace\\${env.JOB_NAME}\\approved\\Query.sql")
					 sqlFile.write("${val}")
	                }else{
					 sqlFile.write("${val}")
					}
					
					//copy to history folder
					  def src = new File("D:\\Jenkins\\workspace\\${env.JOB_NAME}\\approved\\Query.sql")
                      def dst = new File("D:\\Jenkins\\workspace\\${env.JOB_NAME}\\sql-version-history\\sql_history\\${profile}_${key}_Query${sdf.format(date)}.sql")
                      dst << src.text
					
					//update database change log
					//stage("${key} - Update changelog xml"){
					def xmlFile = "workspace/${env.JOB_NAME}/qa-changeLog.xml"
                    def xml = new XmlParser().parse(xmlFile)
					xml.changeSet.each { 
                     it.@id = "${env.JOB_NAME}_${env.BUILD_NUMBER}_${key}"
                     it.@author = "${env.JOB_NAME}_${env.BUILD_NUMBER}_${key}"
                    }
                    new XmlNodePrinter(new PrintWriter(new FileWriter(xmlFile))).print(xml)
					//}
					
					def mvnUpdateCmd="mvn --settings settings.xml liquibase:update -Dliquibase.changeLogFile=qa-changeLog.xml -P${profile}_${key}"
					print "${mvnUpdateCmd}"	
					
					if("${status}".contains("Pending")){
                      bat "${mvnUpdateCmd}"
					} else {
					 print "Skipping liquibase execution since status is not pending"
					}
					
					  
					  
                    }
					}
					
							
					
					
			  
                liquibaseStatus = true
            }catch (e) {
			   print e
			   liquibaseStatus = false
			   echo "[BUILD END]"
			   error("liquibase execution failed")
			   print "liquibase execution failed"
			   
			}
           
        
		 if(liquibaseStatus){
		    print "Liquibase executed successfully"
			postMethod(urlUpdateStatus)  
			stage('Push executed query to nexus'){
				dir("./sql-version-history") {	
				bat 'git pull origin master'
                bat 'git add -A'
                def gitcmd="git commit -m \"update\""
                bat "${gitcmd}"				
                bat 'git push origin HEAD:master'
				}
				}
		  
		 }else{
		   error("Build failed....")
		}
		}
	} 
		
   

@NonCPS
def String getRequestedId(){
    def currJobid = ""
    def myBuildParams = currentBuild.rawBuild.getAction(ParametersAction.class)
    for(ParameterValue p in myBuildParams) {
            if(p.name.contains("jobId")){
                currJobid=p.value
            }
        
    }
    return currJobid
}

@NonCPS


def Map<String,String> getService(url){
    def mp = [ : ] 
    def result = [error: true, data: null]
    def http = new HTTPBuilder(url)
    http.request( Method.GET) { req ->
	//uri.query = [ filter:"{\"jobId\":99d163de-4f1f-4084-b7c1-0489b968b0f0}"]
	  headers.'Authorization' =
        "Basic YTph"
        response.success = { resp, data ->
            result.error = false
            result.data = data
        }
        response.'404' = {
            println 'url not found'
            println "the attempted url is " + url
            println "manual intervention may be required to fix this issue"
            result.data = resp.statusLine
        }
        response.failure = { resp, data ->
            println "HTTP GET failed"
            print resp
            println data
            println "manual intervention may be required to fix this issue"
            result.data = resp.statusLine
        }
    }
    
	def query = result.data.body.query
	def profile = result.data.tenant.name
	def status = result.data.status
    mp.put("connect",result.data.body.connect);
	mp.put("crosscommunity",result.data.body.crosscommunity);
	mp.put("filemanager",result.data.body.filemanager);
	mp.put("profile",result.data.tenant.name);
	mp.put("status",result.data.status);
    println(mp); 
    return mp
}

def List<String> getRequestedAppsForDeployment(){
    def deployApps = []
    def myBuildParams = currentBuild.rawBuild.getAction(ParametersAction.class)
    for(ParameterValue p in myBuildParams) {
        if(p.name.startsWith("Deploy")){
            if(p.value==true){
                deployApps.add(p.name.split(" ")[1])
            }
        }
    }
    return deployApps
}

@NonCPS
def postMethod(urlPassed){
   	  def http = new HTTPBuilder(urlPassed) 

   http.request(PUT, JSON ) {
    requestContentType = ContentType.JSON
    body = [status: 'closed']

    response.success = { resp ->
        println "Success! ${resp.status}"
    }

    response.failure = { resp ->
        println "Request failed with status ${resp.status}"
    }
}

}


@NonCPS
def convertLazyMap2HashMap(data) {
    if (data.getClass().getName() != 'org.apache.groovy.json.internal.LazyMap' && data.getClass().getName() != 'groovy.util.slurpersupport.NodeChild') {
        return data
    }

    data = new HashMap<>(data)
    lazyMapKeys = []
    for (def k in data.keySet()) {
        keyValue = data.get(k)
        if (keyValue.getClass().getName() == 'org.apache.groovy.json.internal.LazyMap' || keyValue.getClass().getName() == 'groovy.util.slurpersupport.NodeChild') {
            lazyMapKeys.add(k)
        }
    }
    lazyMapKeys.each {
        def keyValue = data.get(it)
        def newMap = convertLazyMap2HashMap(keyValue)
        data.remove(it)
        data.put(it, newMap)
    }
    return data
}

@NonCPS
def isSerializeable(cl) {
    if (getInterfaces(cl).contains(Serializable.class)) {
        return true
    }
    return false
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
