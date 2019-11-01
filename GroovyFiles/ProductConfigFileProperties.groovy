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
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.DELETE
import static groovyx.net.http.ContentType.XML
import static groovyx.net.http.ContentType.JSON


def call(body) {


    

    //evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
	
	
    node {
		bat()
	}
							
}
		
@NonCPS
def bat(){
	def jobPath = "${env.JENKINS_HOME}"
	println "${jobPath}"
	//def pythonPath="\"${jobPath}\\workspace\\HIPAA_Security_Assessment_18\\hipaa_authentication_7.py\""
	//bat "python $pythonPath"
	bat "run_python.bat"
}
@NonCPS
def getService(urlPassed,path){
def url=urlPassed+path
    def result 
    def http = new HTTPBuilder()
    http.request( url, GET, JSON) { req ->
        response.success = { resp, data ->
            //result.error = false
            //result.data = data
			result = data
        }
        response.'404' = {
            println 'url not found'
            println "the attempted url is " + url
            println "manual intervention may be required to fix this issue"
            //result.data = resp.statusLine
			result = resp.statusLine
        }
        response.failure = { resp, data ->
            println "HTTP GET failed"
            print resp
            println data
            println "manual intervention may be required to fix this issue"
            //result.data = resp.statusLine
			result = resp.statusLine
        }
    }
    def hashMapData = convertLazyMap2HashMap(result)
    //result.data = hashMapData
	result = hashMapData
    return result
}

@NonCPS
def getMethod(urlPassed,path1,query1){
	  def http = new HTTPBuilder(urlPassed) 
      println http.get(path: path1, query: query1 )

}

@NonCPS
def postMethod(urlPassed,path,query){
   	  def http = new HTTPBuilder(urlPassed) 
  def paths=path
  def queries=query
            http.request(Method.POST,ContentType.URLENC) {
                uri.path = paths
                uri.query = queries
                headers['Content-Type']= "application/x-www-form-urlencoded" 
                response.success = {resp-> println resp.statusLine }
            }

}

@NonCPS
def putService(url,path, json){  
def urlFullPath=url+path
    def result
    def http = new HTTPBuilder()
    def jsonObj = new JsonSlurperClassic().parseText(json)
    println jsonObj 
    http.request(urlFullPath, PUT, JSON ) { req ->
        body = jsonObj 
        response.success = { resp, data -> 
		    println "data"
            println data
            result = data    
        }
        response.failure = { resp, data ->
            println 'fail'
            println data
        }
    } 
   // return result
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