//@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.2')
//@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.3.5')
//@Grab(group='org.apache.httpcomponents', module='httpmime', version='4.3.5')

import groovy.json.JsonSlurperClassic
import groovy.json.StringEscapeUtils
import groovy.util.XmlSlurper
import groovy.json.JsonOutput
import groovyx.net.http.*
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.PUT
import static groovyx.net.http.Method.DELETE
import static groovyx.net.http.ContentType.XML
import static groovyx.net.http.ContentType.JSON
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.ByteArrayBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.entity.mime.content.FileBody
import org.apache.commons.io.FileUtils
import groovyx.net.http.RESTClient
import hudson.model.ParameterValue
import hudson.model.ParametersAction
import jenkins.model.Jenkins


def call(body) {
	// evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
	def Branch = config.Branch
	def script_error_return_value = 1
	def script_success_return_value = 0
	/*
	sample config.json
			{
				"template_file_path": "C:\\Users\\rnedumpurath\\Documents\\development\\datableed\\NiFi-DataFlows\\test-flow-1.xml",
				"nifi_base_url": "http://localhost:8080/nifi-api",
				"max_run_time": 60000,
				"db_url": "jdbc:mysql://10.128.65.81:3306/connect",
				"db_user": "r.nedumpurath",
				"db_password": "",
				"db_driver_loc": "C:\\software_installations\\nifi-1.8.0\\lib",
				"target_directory": "C:\\Users\\rnedumpurath\\Documents\\development\\datableed\\bleeds"
			}
	*/


	
	

	//def template_file_path= config.template_file_path
	def base_url= config.nifi_base_url
	def max_run_time= config.max_run_time
	def db_url= config.db_url
	def db_user= config.db_user
	def db_password= config.db_password
	def db_driver_loc= config.db_driver_loc
	def target_directory= config.target_directory
	
	String oldDate = '20150702'
    Date date = new Date().format( 'MM-dd-yyyy' )
    def currentDate = date
	println "Todays date: " + currentDate
	def target_directory_with_date=target_directory.replace(":date:", currentDate)
	
    sleep 10

	node {
	 File file = new File("D:\\Archive\\out.txt")
       file. write "${config.env}\n"
       
	 
	 
	 	        stage('Code checkout'){
            //Marks build start
             
            //Checkout the latest code
            dir("./${config.name}") {	
			git credentialsId: 'cf7a0040-05b2-4835-b49e-8e5814141f5b', url: "https://sadhasivim@bitbucket.org/gsihealth/${config.name}"
            }
        }
	      //iterating the templates
		  def xmlPath="D:\\Jenkins\\workspace\\${env.JOB_NAME}\\${config.name}\\"
		  def templateList=getFileList(xmlPath)
		  stage('Nifi deploy'){
		  for ( int i = 0; i < templateList.size(); i++ ) {
		     try{
			  target_directory=target_directory_with_date
			  if((templateList.get(i)).contains("datableed-scenario-Patient-3-patient_demog-ethnic")){
          println templateList.get(i)
		  if((templateList.get(i)).contains("datableed-scenario-Patient-1-patient_demog-gender")){
		  target_directory=target_directory+"Data Bleed Verification Scenario-3"
		  }else if((templateList.get(i)).contains("datableed-scenario-Patient-2-patient_demog-county")){
		  target_directory=target_directory+"Data Bleed Verification Scenario-4"
		  }else if((templateList.get(i)).contains("datableed-scenario-Patient-3-patient_demog-ethnic")){
		  target_directory=target_directory+"Data Bleed Verification Scenario-5"
		  }else if((templateList.get(i)).contains("datableed-scenario-Patient-4-patient_list_tags")){
		  target_directory=target_directory+"Data Bleed Verification Scenario-6"
		  }else if((templateList.get(i)).contains("datableed-scenario-Patient-5-user_patient_list_tags")){
		  target_directory=target_directory+"Data Bleed Verification Scenario-7"
		  }else if((templateList.get(i)).contains("datableed-scenario-Patient-6-non_health_home_provider")){
		  target_directory=target_directory+"Data Bleed Verification Scenario-8"
		  }else if((templateList.get(i)).contains("datableed-scenario-Patient-7-consent_history")){
		  target_directory=target_directory+"Data Bleed Verification Scenario-9"
		  }else if((templateList.get(i)).contains("datableed-scenario-Program-1-program_link")){
		  target_directory=target_directory+"Data Bleed Verification Scenario-1"
		  }else if((templateList.get(i)).contains("datableed-scenario-Program-2-patient_program_consent_program_id")){
		  target_directory=target_directory+"Data Bleed Verification Scenario-10"
		  }else if((templateList.get(i)).contains("datableed-scenario-Program-3-patient_program_consent_program_level_id")){
		  target_directory=target_directory+"Data Bleed Verification Scenario-11"
		  }else if((templateList.get(i)).contains("datableed-scenario-Program-4-program_status")){
		  target_directory=target_directory+"Data Bleed Verification Scenario-12"
		  }else if((templateList.get(i)).contains("datableed-scenario-Careteam-1-patient_careteam")){
		  target_directory=target_directory+"Data Bleed Verification Scenario-2"
		  }else if((templateList.get(i)).contains("datableed-scenario-Careteam-2-community_careteam")){
		  target_directory=target_directory+"Data Bleed Verification Scenario-14"
		  }else if((templateList.get(i)).contains("datableed-scenario-Organization-1-group_org_consent")){
		  target_directory=target_directory+"Data Bleed Verification Scenario-15"
		  }else if((templateList.get(i)).contains("datableed-scenario-Organization-2-community_organization_facility_type")){
		  target_directory=target_directory+"Data Bleed Verification Scenario-16"
		  }else if((templateList.get(i)).contains("datableed-scenario-Users-1-access_level")){
		  target_directory=target_directory+"Data Bleed Verification Scenario-13"
		  }
          
		println "This script will load, instantiate and run the NiFi template ${templateList.get(i)}"
		println "The data bleed messages will be available in the folder ${target_directory}"
		//println "nifi base url: " + base_url
		//println "template file path: " + templateList.get(i)
		def url
		def response
		def info



		url = base_url + "/process-groups/root"
		response = getService(url)
		if (response.error) {
			println "Error in getting NiFi root information"
			println info.data
			println "stopping the script"
			return script_error_return_value
		}
		info = response.data
		println info
		String group_id = info.id
		println group_id

		println "uploading the template"
		url= base_url + "/process-groups/" + group_id  + "/templates/upload"
		response = fileUpload(url, templateList.get(i))
		if (response.error) {
			println "Error while uploading NiFi template"
			println response.data
			println "stopping the script"
			return script_error_return_value
		} else {
			println "template uploaded"
		}
		info = response.data
		template_id = info.template.id

		println "instantiating the template"
		url = base_url + "/process-groups/" + group_id + "/template-instance"
		def json = '{"templateId":"' + template_id + '","originX":0.0,"originY":0.0}'
		response = instantiateTemplate(url, json)
		if (response.error) {
			println "Error while instantiating the NiFi template"
			println response.data
			println "stopping the script"
			return script_error_return_value
		} else {
			println "template instantiated"
		}
		info = response.data
		def process_group_id = info.process_group_id

		//get the process_group information
		println "fetching the process group information"
		url = base_url + "/process-groups/" + process_group_id
		response = getService(url)
		if (response.error) {
			println "Error while fetching the process group information"
			println response.data
			println "stopping the script"
			return script_error_return_value
		} else {
			println "process group information fetched"
		}
		info = response.data
		def process_group_client_id = info.revision.clientId
		def process_group_version = info.revision.version


		//set the variables for the process group
		println "setting the variable registry of the process group"
		jsonGString = """
			{
				"processGroupRevision": {
					"clientId": "",
					"version": ${process_group_version}
				},
				"variableRegistry": {
					"processGroupId": "${process_group_id}",
					"variables": [
						{
							 "variable": {
								"name": "Target_Directory",
								"value": "${StringEscapeUtils.escapeJava(target_directory)}"
							}
						},
						{
							 "variable": {
								"name": "Db_Url",
								"value": "${config.db_url}"
							}
						},
						{
							 "variable": {
								"name": "Db_Driver_Loc",
								"value": "${StringEscapeUtils.escapeJava(config.db_driver_loc)}"
							}
						},
						{
							 "variable": {
								"name": "Db_User",
								"value": "${config.db_user}"
							}                                                            
						}
					]
				}
			}
		"""
		json = jsonGString.toString()
		url = base_url + "/process-groups/" + process_group_id + "/variable-registry"
		response = putService(url, json)
		if (response.error) {
			println "Error while setting the variable registry of the process group"
			println response.data
			println "stopping the script"
			return script_error_return_value
		} else {
			println "variable registry has been updated"
		}
		info = response.data

		//fetch the controller service info and set the db  password
		println "fetching the controller service (dbcp) information"
		url = base_url + "/flow/process-groups/" + process_group_id + "/controller-services"
		response = getControllerServiceInfo(url)
		if (response.error) {
			println "Error while fetching the controller service (dbcp) information"
			println response.data
			println "stopping the script"
			return script_error_return_value
		} else {
			println "controller service (dbcp) information fetched"
		}
		info = response.data
		def id = info.id
		def controller_service_version = info.version

		//println "getting  controller service information"
		//url = base_url + "/controller-services/" + id

		println "setting the db password for dbcp controller service"
		jsonGString = """
			{
				"revision": {
					"version": ${controller_service_version}
				},
				"component": {
					"id": "${id}",
					"properties": {
						"Password": "${StringEscapeUtils.escapeJava(config.db_password)}"
					}
				}
			}
		"""
		json = jsonGString.toString()

		url = base_url + "/controller-services/" + id
		response = putService(url, json)
		if (response.error) {
			println "Error while setting the db password for dbcp controller service"
			println response.data
			println "stopping the script"
			return script_error_return_value
		} else {
			println "the db password for dbcp controller service has been set"
		}
		info = response.data


		//fetch the controller service info and enable the controller service
		println "fetching the controller service information"
		url = base_url + "/flow/process-groups/" + process_group_id + "/controller-services"
		response = getControllerServiceInfo(url)
		if (response.error) {
			println "Error while fetching the controller service information"
			println response.data
			println "stopping the script"
			return script_error_return_value
		} else {
			println "the controller service information has been fetched"
		}
		info = response.data
		controller_service_version = info.version
		id = info.id
		println "enabling the controller service - dbcp"
		url = base_url + "/controller-services/" + id
		json = '{"revision":{"version":'+controller_service_version+'},"component":{"id":"'+id+'","state":"ENABLED"}}'
		response = putService(url, json)
		if (response.error) {
			println "Error while enabling the controller service - dbcp"
			println response.data
			println "stopping the script"
			return script_error_return_value
		} else {
			println "the dbcp controller service has been enabled"
		}
		info = response.data

		println "sleeping for 30 seconds"
		sleep(30)

		println "starting the process group"
		url = base_url + "/flow/process-groups/" + process_group_id
		json = '{"id":"' + process_group_id + '","state":"RUNNING"}'
		response = putService(url, json)
		if (response.error) {
			println "Error while starting the process group"
			println response.data
			println "stopping the script"
			return script_error_return_value
		} else {
			println "the process group has started"
		}
		info = response.data

		if (max_run_time) {
			println "the process group will run for ${max_run_time / 60000} minute/minutes"
			sleep(max_run_time)
		} else {
			println "max_run_time is not set. The process group will run till the queue size becomes zero"
			sleep(30)
			while (!is_queue_empty(base_url, process_group_id)) {
				sleep(30)
			}
		}
    
		//stop the process group
		println "stopping the process group"
		url = base_url + "/flow/process-groups/" + process_group_id
		json = '{"id":"'+ process_group_id +'","state":"STOPPED"}'
		response = putService(url, json)
		if (response.error) {
			println "Error while stopping the process group"
			println response.data
			println "stopping the script"
			return script_error_return_value
		} else {
			println "the process group has been stopped"
		}
		info = response.data


		//flush the queues only if the max_run_time was set.
		if (max_run_time) {
			println "flushing the process group queues"
			//stopping the process group takes some time.
			sleep(60)
			flush_queues_for_process_group(base_url, process_group_id)
			//provide some time to complete the flush
			sleep(60)
		}


		//disable controller service
		println "disabling the controller service"
		url = base_url + "/flow/process-groups/" + process_group_id + "/controller-services"
		response = getControllerServiceInfo(url)
		if (response.error) {
			println "Error while fetching the controller service information"
			println response.data
			println "stopping the script"
			return script_error_return_value
		} else {
			println "the controller service information has been fetched"
		}
		info = response.data
		controller_service_version = info.version
		id = info.id

		url = base_url + "/controller-services/" + id
		json = '{"revision":{"version":'+controller_service_version+'},"component":{"id":"'+id+'","state":"DISABLED"}}'
		response = putService(url, json)
		if (response.error) {
			println "Error while disabling the controller service"
			println response.data
			println "stopping the script"
			return script_error_return_value
		} else {
			println "the controller service has been disabled"
		}
		info = response.data

		//delete process group
		//get the process group information
		println "deleting the process group"
		url = base_url + "/process-groups/" + process_group_id
		response = getService(url)
		if (response.error) {
			println "Error while fetching the process group information"
			println response.data
			println "stopping the script"
			return script_error_return_value
		} else {
			println "process group information has been fetched"
		}
		info = response.data
		version = info.revision.version
		client_id = info.revision.clientId
		url = base_url + "/process-groups/" + process_group_id + "?version=" + version
		response = deleteService(url)
		if (response.error) {
			println "Error while deleting the process group"
			println response.data
			println "stopping the script"
			return script_error_return_value
		} else {
			println "process group has been deleted"
		}
		info = response.data

		// delete template
		println "deleting the template"
		url = base_url + "/templates/" + template_id
		response = deleteService(url)
		if (response.error) {
			println "Error while deleting the template"
			println response.data
			println "stopping the script"
			return script_error_return_value
		} else {
			println "the template has been deleted"
		}
		info = response.data
 
		//return script_success_return_value
		
           println "check"  
}		   
              }
		catch(Exception ex) {
         println("Catching the exception"+ex);
      }
	 
	  
	}
	}
	File file1 = new File("D:\\Archive\\DataBleedReport\\DataBleedDev01\\")
	FileUtils.cleanDirectory(file1); 
	File file2 = new File("D:\\Archive\\DataBleedReport\\DataBleedDev02\\")
	FileUtils.cleanDirectory(file2); 
	File file3 = new File("D:\\Archive\\DataBleedReport\\DataBleedDev03\\")
	FileUtils.cleanDirectory(file3); 
	String remoteGetCommand = "echo y | D:\\PuTTY\\pscp.exe -pw P@ssw0rd1 -r cdnonprod@10.168.54.67:/usr/hdf/3.1.2.0-7/nifi/docs/${config.env} D:\\Archive\\DataBleedReport\\DataBleedDev01"
    bat "${remoteGetCommand}"
	String remoteGetCommand1 = "echo y | D:\\PuTTY\\pscp.exe -pw P@ssw0rd1 -r cdnonprod@10.168.54.68:/usr/hdf/3.1.2.0-7/nifi/docs/${config.env} D:\\Archive\\DataBleedReport\\DataBleedDev02"
    bat "${remoteGetCommand1}"
	String remoteGetCommand2 = "echo y | D:\\PuTTY\\pscp.exe -pw P@ssw0rd1 -r cdnonprod@10.168.54.69:/usr/hdf/3.1.2.0-7/nifi/docs/${config.env} D:\\Archive\\DataBleedReport\\DataBleedDev03"
    bat "${remoteGetCommand2}"
	}
	

   }


@NonCPS
def readFromJson(data){
    def jsonSlurper = new JsonSlurperClassic()
    return jsonSlurper.parseText(data)
}

@NonCPS
def fileUpload(url, filepath){
    def result = [error: true, data: null]
    def http = new HTTPBuilder(url)
    http.request(POST) {req ->
        //headers.'Accept' = 'text/plain'
        MultipartEntityBuilder multipartRequestEntity = new MultipartEntityBuilder()
        String key = multipartRequestEntity.addPart('template', new FileBody(new File(filepath)))
        req.entity =  multipartRequestEntity.build()
        response.success = { resp, data ->
            result.error = false
            result.data = [template:  [id: ""]]
            result.data.template.id = data.template.id.toString()
        }
        response.'409' = { resp, data ->
            println "file already exists"
            //result.data = resp.statusLine
        }
        response.failure = { resp, data ->
            println 'file upload failed'
            println "Got response: ${resp.statusLine}"
            println resp
            println data
            println "manual intervention required to fix this issue"
            //result.data = resp.statusLine
        }
    }

    return result
}

@NonCPS
def getService(url){
    def result = [error: true, data: null]
    def http = new HTTPBuilder()
    http.request( url, GET, JSON) { req ->
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
    def hashMapData = convertLazyMap2HashMap(result.data)
    result.data = hashMapData
    return result
}

@NonCPS
def getControllerServiceInfo(url) {
    def result = [error: true, data: null]
    def http = new HTTPBuilder()
    http.request( url, GET, JSON) { req ->
        response.success = { resp, data ->
            result.error = false
            result.data = [id: data.controllerServices[0].id.toString(), version: data.controllerServices[0].revision.version]
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
    def hashMapData = convertLazyMap2HashMap(result.data)
    result.data = hashMapData
    return result
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
def deleteService(url){
    def result = [error: true, data: null]
    def http = new HTTPBuilder(url)
    http.request(DELETE) { req ->
        headers.'Content-Type' = 'application/json'
        response.success = { resp, data ->
            def reponse=[resp:resp,data:data]
            result.error = false
            result.data = data
        }
        response.'404' = {
            println 'Not found'
            result.error = false
        }
        response.failure = { resp ->
            println 'HTTP DELETE failed'
            println "Response status ${resp.statusLine}"
            result.data = resp.statusLine
        }
    }
    def hashMapData = convertLazyMap2HashMap(result.data)
    result.data = hashMapData
    return result
}

@NonCPS
def postService(url, json){
    def result = [error: true, data: null]
    def http = new HTTPBuilder()
    def jsonObj = new JsonSlurperClassic().parseText(json)

    http.request(url, POST, JSON ) { req ->
        body = jsonObj
        response.success = { resp, data ->
            result.error = false
            result.data = data
        }
        response.failure = { resp, data ->
            println 'HTTP POST failed'
            println resp
            println data
            println "manual intervention may be required"
            result.data = resp.statusLine
        }
    }
    def hashMapData = convertLazyMap2HashMap(result.data)
    result.data = hashMapData
    return result
}

@NonCPS
def instantiateTemplate(url, json) {
    def result = [error: true, data: null]
    def http = new HTTPBuilder()
    def jsonObj = new JsonSlurperClassic().parseText(json)
    http.request(url, POST, JSON ) { req ->
        body = jsonObj
        response.success = { resp, data ->
            result.error = false
            result.data = [process_group_id: data.flow.processGroups[0].id.toString()]
        }
        response.failure = { resp, data ->
            println 'HTTP POST failed'
            println resp
            println data
            println "manual intervention may be required"
            result.data = resp.statusLine
        }
    }
    def hashMapData = convertLazyMap2HashMap(result.data)
    result.data = hashMapData
    return result
}

@NonCPS
def putService(url, json_string){
    def result = [error: true, data: null]
    def http = new HTTPBuilder()
    def jsonObj = new JsonSlurperClassic().parseText(json_string)
    println jsonObj
    http.request(url, PUT, JSON ) { req ->
        body = jsonObj
        response.success = { resp, data ->
            result.error = false
            result.data = [status: "ok"]
        }
        response.failure = { resp, data ->
            println "HTTP PUT failed"
            println resp
            println data
            println "manual intervention may be required"
            result.data = resp.statusLine
        }
    }
    def hashMapData = convertLazyMap2HashMap(result.data)
    result.data = hashMapData
    return result
}

@NonCPS
def putServiceJson(url, json){
    def result = [error: true, data: null]
    def http = new HTTPBuilder()
    http.request(url, PUT, JSON ) { req ->
        body = json
        response.success = { resp, data ->
            result.error = false
            result.data = [status: "ok"]
        }
        response.failure = { resp, data ->
            println "HTTP PUT failed"
            println resp
            println data
            println "manual intervention may be required"
            result.data = resp.statusLine
        }
    }
    def hashMapData = convertLazyMap2HashMap(result.data)
    result.data = hashMapData
    return result
}

@NonCPS
def flush_queues_for_process_group(base_url, process_group_id) {
    def process_group_ids = get_all_process_group_ids(base_url, process_group_id)
    process_group_ids.each {
        def url = base_url + "/process-groups/" + it + "/connections"
        def response = getService(url)
        def info = response.data
        def pg_connections = []
        if (!response.error) {
            info.connections.each {
                pg_connections.add(it.id)
            }
        }

        pg_connections.each {
            url = base_url + "/flowfile-queues/" + it + "/drop-requests"
            response = postService(url, "{}")
        }
    }
}

@NonCPS
def get_all_process_group_ids(base_url, process_group_id) {
    def pg_id_list = []
    pg_id_list.add(process_group_id)
    //get all child process groups
    def url = base_url + "/flow/process-groups/" + process_group_id
    def response = getService(url)
    def info = response.data
    def pg_count = response.error? 0 : info.processGroupFlow.flow.processGroups.size()

    if (pg_count == 0) return pg_id_list
    info.processGroupFlow.flow.processGroups.each {
        pg_id_list = pg_id_list + get_all_process_group_ids(base_url, it.id)
    }
    return pg_id_list
}

@NonCPS
def is_queue_empty (base_url, process_group_id) {
    def url = base_url + "/flow/process-groups/" + process_group_id + "/status"
    def response = getService(url)
    if (response.error) {
        println "there seems some issues with NiFi APIs. Manual interventions is required."
        println response.data
        return false
    }
    def info = response.data
    println "current queue size: ${info.processGroupStatus.aggregateSnapshot.queuedCount}"
    if (info.processGroupStatus.aggregateSnapshot.queuedCount == "0" ) {
        return true
    }
    else {
        return false
    }
}
 @NonCPS
def getFileList(xmlPath){
    def myList = []
    new File(xmlPath).eachFileMatch(~/.*.xml/) { file ->
    println file.getName()
    myList.add(xmlPath+file.getName())
	}
    return myList
}

