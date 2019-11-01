package com.gsihealth.jenkins.utils

interface IEmailHandler {
    def sendEmail(String[] recipients, boolean attachLog)
    def generateEmail()
}
