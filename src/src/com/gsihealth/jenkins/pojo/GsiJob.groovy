package com.gsihealth.jenkins.pojo

import hudson.model.Job

/**
 * Created by sonokohirano on 8/4/17.
 */
class GsiJob implements Serializable{

    String name

    GsiJob(){}

    GsiJob(Job<?,?> job){
        this.populate(job)
    }
    @NonCPS
    void populate(Job<?,?> job){
        if(job==null) return
        this.name = job.name
    }

}
