package com.gsihealth.jenkins.utils

class ReportUtils implements Serializable{

    def steps
    def deploymentSummary

    ReportUtils(steps){
        this.steps = steps
    }



}
