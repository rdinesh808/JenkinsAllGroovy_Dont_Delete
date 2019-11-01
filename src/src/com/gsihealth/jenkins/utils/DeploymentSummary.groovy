package com.gsihealth.jenkins.utils

import com.gsihealth.jenkins.runner.DeployTask

class DeploymentSummary implements Serializable{

    Map<String,Object> data
    def steps

    DeploymentSummary(steps){
        this.steps = steps
    }

    def populateData(DeployTask deployTask, final env, final currentBuild){

        def ipUrl = CommonUtils.replaceAll(env.BUILD_URL, "Jenkins01", "10.128.65.100")
        def homeIpUrl = CommonUtils.replaceAll(env.JENKINS_URL,"Jenkins01", "10.128.65.100")

        def appList = deployTask.deployAppNameList

        def build = [
                server : deployTask.serverName,
                requestedApps : deployTask.requestedApps,
                environment : deployTask.environment,
                branch : deployTask.branchName,
                release : deployTask.releaseNumber,
                force : deployTask.force,
                start : deployTask.startDate,
                deployedApps:appList,
                stop : deployTask.endDate,
                requestedUser : deployTask.requestedUser,
                duration : CommonUtils.getTimeDelta(deployTask.endDate, deployTask.startDate),
                homeUrl  : "${env.JENKINS_URL}",
                homeIpUrl : "${homeIpUrl}",
                url      : "${env.BUILD_URL}",
                reportUrl: "${env.BUILD_URL}Deployment_Report/",
                ipUrl    : "${ipUrl}",
                ipReportUrl    : "${ipUrl}Deployment_Report/",
                jobName  : "${env.JOB_NAME}",
                buildName: "${currentBuild.displayName}",
                status : currentBuild.result?:"SUCCESS"

        ]

        def modules = []

        for(def i = 0; i < appList.length; i++){
            def appName = appList[i]

            def data = [
                    appName : appName,
                    srcJob : deployTask.deployTargetMap.get(appName)
            ]

            def dataPath = "./report/data/${appName}.json"

            if (!deployTask.deployTargetMap.containsKey(appName) ||
                    !deployTask.deployTargetMap.get(appName).runDeploy) {
                data.status = "no-change"

            } else if (this.steps.fileExists(dataPath)) {
                def jsonData = steps.readFile dataPath
                data += CommonUtils.parseJsonString(jsonData)

            }else data.status = "not-run"

            modules.add(data)
        }

        this.data = [
                build: build,
                modules: modules
        ]

        if(deployTask.debug) print this.data

    }

    def publishReport(){

        String template = steps.libraryResource Constants.DEPLOYMENT_REPORT_TEMPLATE
        steps.writeFile file:"./${Constants.REPORT_DIR}/summary.json", text: CommonUtils.objectToJsonString(this.data)
        String summaryHtml = CommonUtils.generateFromTemplate(this.data, template)
        steps.writeFile file: "./${Constants.REPORT_DIR}/${Constants.REPORT_FILE_NAME}", text: summaryHtml
        steps.publishHTML(target: [allowMissing: true, keepAll: true, reportDir: Constants.REPORT_DIR, reportFiles: Constants.REPORT_FILE_NAME, reportName: 'Deployment Report'])

    }

    def sendEmail(String[] recipients, boolean attachLog ) {
        if (recipients.size() == 0) return
        def email = this.generateEmail()
        steps.emailext body: email.content, subject: email.subject, to: recipients.join(','), mimeType: "text/html", attachLog: attachLog
    }

    def generateEmail(){
        String template = steps.libraryResource Constants.DEPLOYMENT_EMAIL_TEMPLATE
        def email = [
                subject : "[${this.data.build.status}] ${this.data.build.jobName} - ${this.data.build.buildName}",
                content : CommonUtils.generateFromTemplate(this.data, template)
        ]
        return email
    }



}
