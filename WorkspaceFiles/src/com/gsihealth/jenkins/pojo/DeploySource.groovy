package com.gsihealth.jenkins.pojo


class DeploySource implements Serializable{

    /**
     * Name of the source job
     * */
    String name

    /**
     * Source build used for deployment
     * */
    SourceBuild srcBuild

    String getJobName(){
        return this.name
    }

    void setJobName(String jobName){
        this.name = name
    }

    String getBuildName(){
        if(this.srcBuild==null) return null
        return this.srcBuild.name
    }

    void setBuildName(String buildName){
        if(this.srcBuild==null) this.srcBuild = new SourceBuild()
        this.srcBuild.name = buildName
    }

    int getBuildNumber(){
        if(this.srcBuild==null) return 0
        return this.srcBuild.number
    }

    void setBuildNumber(int buildNumber){
        if(this.srcBuild==null) this.srcBuild = new SourceBuild()
        this.srcBuild.number = buildNumber
    }


}