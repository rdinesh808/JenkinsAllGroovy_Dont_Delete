package com.gsihealth.jenkins.runner

import com.gsihealth.jenkins.pojo.GsiJob
import com.gsihealth.jenkins.pojo.GsiRun
import com.gsihealth.jenkins.utils.CommonUtils

class GitBuildTask implements Serializable{

    def name, startDate, endDate, type, release, url, requestedUser, result, culprits, developers, checkoutResult, duration
    GsiRun run

    GitBuildTask(env){
        url="${env.BUILD_URL}"
    }

    def setConfig(config){
        name = config.name
        release = config.release
        type = config.type
    }

}
