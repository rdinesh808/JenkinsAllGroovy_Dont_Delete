package com.gsihealth.jenkins.utils

trait EmailTrait {
    def sendEmail(steps, email, String[] recipients, boolean attachLog ){
        if (recipients.size() == 0) return
        steps.emailext body: email.content, subject: email.subject, to: recipients.join(','), mimeType: "text/html", attachLog: attachLog
    }
}
