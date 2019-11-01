package com.gsihealth.jenkins.utils

import com.gsihealth.jenkins.runner.GitBuildTask

class GitBuildSummary implements Serializable{

    def steps, subject,env
    GitBuildTask task

    GitBuildSummary(steps, env, GitBuildTask task){
        this.steps=steps
        this.task=task
        this.env = env
    }



    def sendEmail(recipients, boolean attachLog) {
        this.steps.echo "generating email..."
        def email = this.generateEmail()
        if (recipients.size() == 0) return
        steps.emailext body: email.content, subject: email.subject, to: recipients.join(','), mimeType: "text/html", attachLog: attachLog
    }

    def generateEmail() {
//        String template = steps.libraryResource Constants.GIT_BUILD_EMAIL_TEMPLATE
        String template = steps.readFile("${env.JENKINS_HOME}/email-templates/gsiBuild-html.groovy")
        def email = [
                subject : "[${task.result}] - ${task.run.parent.name} - ${task.run.displayName}",
                content : CommonUtils.generateFromTemplate([task: task], template)
        ]
        return email
    }
}
