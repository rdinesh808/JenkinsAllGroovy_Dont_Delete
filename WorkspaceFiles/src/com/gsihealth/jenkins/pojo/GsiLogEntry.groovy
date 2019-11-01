package com.gsihealth.jenkins.pojo

/**
 * Created by sonokohirano on 8/4/17.
 */
class GsiLogEntry implements Serializable{

    String author
    int revision
    String message
    String date
    String[] paths

    GsiLogEntry(){}

    GsiLogEntry(hudson.scm.SubversionChangeLogSet.LogEntry logEntry){
        this.populate(logEntry)
    }

    @NonCPS
    void populate(hudson.scm.SubversionChangeLogSet.LogEntry logEntry){
        if(logEntry==null) return
        this.author = logEntry.author
        this.revision = logEntry.revision
        this.message = logEntry.msg
        this.date = logEntry.date
        this.paths = logEntry.paths.collect{it.value}
    }

}
