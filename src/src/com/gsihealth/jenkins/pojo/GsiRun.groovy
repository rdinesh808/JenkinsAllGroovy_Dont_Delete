package com.gsihealth.jenkins.pojo

import hudson.model.Run

class GsiRun implements Serializable{

    GsiRun(){}
    GsiRun(Run run){ this.populate(run) }

    int number
    String displayName
    GsiJob parent

    @NonCPS
    void populate(Run run){
        if(run==null) return
        this.number = run.number
        this.displayName = run.displayName
        this.parent = new GsiJob(run.parent)
    }

}