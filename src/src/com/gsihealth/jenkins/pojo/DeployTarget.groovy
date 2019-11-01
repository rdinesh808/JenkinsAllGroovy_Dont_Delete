package com.gsihealth.jenkins.pojo

import hudson.model.Run
import hudson.scm.ChangeLogSet
class DeployTarget implements Serializable{
    String appName
    String releaseNickname
    GsiRun currentSrcBuild
    GsiRun prevSrcBuild
    boolean runDeploy
    List<List<GsiChangeLogSet>> changeLogSetList = []

    void setCurrentSrcBuild(Run run){
        if(run==null) return
        this.currentSrcBuild = new GsiRun(run)
    }

    void setPrevSrcBuild(Run run){
        if(run==null) return
        this.prevSrcBuild = new GsiRun(run)
    }

    void addChangeLogSets(List<ChangeLogSet> changeLogSets){
        if(changeLogSets==null) return
        changeLogSetList.add(changeLogSets.collect{new GsiChangeLogSet(it)})
    }

}