package com.gsihealth.jenkins.runner

import com.gsihealth.jenkins.pojo.DeploySource
import com.gsihealth.jenkins.pojo.DeployTarget
import com.gsihealth.jenkins.pojo.DeploymentType
import hudson.model.ParameterValue

class DeployTask implements Serializable{

    Date startDate
    Date endDate

    String jobName
    String environment
    String serverName
    String branchName
    String releaseNumber

    DeploymentType deploymentType

    boolean rollBackMode
    String rollBackNumber

    boolean force

    boolean debug

    int sleepSecBeforeDeploy
    boolean disableEmail

    List<String> requestedApps = []

    String requestedUser

    Map<String,DeployTarget> deployTargetMap

    Map<String, DeploySource> archive

    Map<String,Object> custom

    String result

    @NonCPS
    String[] getDeployAppNameList(){
        if(!this.deployTargetMap) return []
        this.deployTargetMap.entrySet().findAll() {it.value.runDeploy}.collect({it.key}) as String[]
    }

    @NonCPS
    String getQuotedDeployList(){
        deployAppNameList.each {"'${it}'"}.join(",")
    }

    boolean hasDeployAppList(){
        this.deployAppNameList.length > 0
    }



    DeployTask setConfig(config){
        this.environment = config.environment
        this.branchName = config.branch
        this.releaseNumber = config.release
        this.deploymentType =
                config.spring ? DeploymentType.SPRING_APP :
                        config.nodeApp ? DeploymentType.NODE_APP : DeploymentType.GLASSFISH_APP
        this.debug = config.debug?:false
        this.custom = config.custom?:[:]
        return this
    }

    @NonCPS
    DeployTask setParameters(buildParams){
        this.requestedApps = []

        buildParams.each { k,v ->
            switch(k){

                case "Run Rollback":
                    this.rollBackMode = v.toBoolean()?:false
                    break
                case "Rollback Target":
                    this.rollBackNumber = v.split('/')[-1] ?: null
                    break
                case "Force deploy":
                    this.force = v.toBoolean() ?: false
                    break
                case "Sleep before deploy":
                    this.sleepSecBeforeDeploy = v.toInteger()?:0
                    break
                case "Disable email":
                    this.disableEmail = v.toBoolean()?:false
                    break
                default:
                    if (k.startsWith("Deploy")) {
                        if (v) {
                            this.requestedApps.add(k.split(" ")[1])
                        }
                    }else{
                        print "Not supported"
                    }
            }
        }

        if(this.rollBackMode && this.rollBackNumber == null){
            throw new IllegalArgumentException("rollback number can not be null if rollback mode is enabled");
        }

    }

    boolean shouldSendEmail(){
        !this.disableEmail && !this.debug
    }



}