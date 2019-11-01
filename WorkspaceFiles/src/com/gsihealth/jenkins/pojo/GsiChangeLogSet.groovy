package com.gsihealth.jenkins.pojo

import hudson.scm.ChangeLogSet
import hudson.scm.SubversionChangeLogSet

class GsiChangeLogSet implements Serializable{

    GsiRun run
    GsiLogEntry[] gsiLogEntries

    GsiChangeLogSet(){}

    GsiChangeLogSet(ChangeLogSet changeLogSet){
        this.populate(changeLogSet)
    }

    @NonCPS
    void populate(ChangeLogSet changeLogSet){
        if(changeLogSet==null) return
        this.run = new GsiRun(changeLogSet.run)
        if(changeLogSet instanceof SubversionChangeLogSet){
            SubversionChangeLogSet svnChangeLogSet = (SubversionChangeLogSet) changeLogSet
            this.gsiLogEntries = svnChangeLogSet.getLogs().collect() {
                new GsiLogEntry(it)
            }
        }
    }

}