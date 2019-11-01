package com.gsihealth.jenkins.utils

import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic
import groovy.text.GStringTemplateEngine
import groovy.time.TimeCategory
import hudson.model.ParameterValue
import hudson.model.ParametersAction

class CommonUtils implements Serializable{

    def steps

    CommonUtils(steps){
        this.steps = steps
    }

    @NonCPS
    static Map<String, Object> parseJsonString(String data){
        if(!data) return null
        def jsonSlurper = new JsonSlurperClassic()
        return (Map) jsonSlurper.parseText(data)
    }

    @NonCPS
    static String generateFromTemplate (Map<?,?> map, String template){
        def engine = new GStringTemplateEngine()
        return engine.createTemplate(template).make(map).toString()
    }

    @NonCPS
    static String objectToJsonString(Map data){
        return JsonOutput.toJson(data)
    }

    @NonCPS
    static String replaceAll(original, regex, newText){
        return original.replaceAll(regex, newText)
    }

    @NonCPS
    static getParameters(parameters){
        parameters.collectEntries { [(it.name) : it.value] }
    }

    @NonCPS
    static getRequestedUser(steps){
        return steps.emailextrecipients([[$class: 'RequesterRecipientProvider']])
    }

    @NonCPS
    static getDevelopers(steps){
        return steps.emailextrecipients([[$class: 'DevelopersRecipientProvider']])
    }

    @NonCPS
    static getCulprits(steps){
        return steps.emailextrecipients([[$class: 'CulpritsRecipientProvider']])
    }



    @NonCPS
    static String getPSCmd(String command){
        String prefix = "powershell.exe -command \""
        String suffix = "\";exit \$LASTEXITCODE;"
        return prefix+command+suffix
    }

    @NonCPS
    static String getTimeDelta(timeStop, timeStart){
        return TimeCategory.minus(timeStop,timeStart).toString()
    }

}