package com.gsihealth.jenkins.runner

import com.gsihealth.jenkins.pojo.DeploySource
import com.gsihealth.jenkins.pojo.DeployTarget
import com.gsihealth.jenkins.pojo.DeploymentType
import com.gsihealth.jenkins.utils.CommonUtils
import com.gsihealth.jenkins.utils.Constants
import com.gsihealth.jenkins.utils.Logger
import hudson.model.Job
import hudson.model.Run
import jenkins.model.Jenkins
import jenkins.scm.RunWithSCM;

class DeploymentRunner implements Serializable {

    def steps

    Logger logger = new Logger()

    DeploymentRunner(steps){
        this.steps = steps
    }

    DeployTask initializeTask(jobName, config, buildParams){

        DeployTask deployTask = new DeployTask()
        deployTask.jobName = jobName
        deployTask.startDate = new Date()
        deployTask.setConfig(config)
        deployTask.setParameters(buildParams)
        deployTask.requestedUser = CommonUtils.getRequestedUser(this.steps)
        deployTask.archive = loadFromArchive(deployTask.jobName)
        deployTask.deployTargetMap = getDeployTargetMap(deployTask)

        logger.info(deployTask.hasDeployAppList() ?
                "Apps to be deployed: ${this.deployTask.deployAppNameList}" :
                "All applications are latest builds. If you want to force deployment, use 'force' option."
        )

        return deployTask
    }


    private Map<String,DeploySource> loadFromArchive(String thisJobName, prevDeployNumber=0){

        Map<String, DeploySource> _map = new HashMap<>()

        def selector =
                prevDeployNumber == 0 ?
                        [$class: 'StatusBuildSelector', stable: false] :
                        [$class: 'SpecificBuildSelector', buildNumber: "${prevDeployNumber}"]

        this.steps.step([
                projectName         : thisJobName,
                $class              : 'CopyArtifact',
                fingerprintArtifacts: true,
                optional            : (prevDeployNumber == 0),
                target              : "./${Constants.LAST_SUC_DIR}/",
                selector            : selector
        ])

        def srcInfoFilePath = "${Constants.LAST_SUC_DIR}/${Constants.OUT_DIR}/source_info.json" as String

        if (!this.steps.fileExists(srcInfoFilePath)) return _map

        logger.info "Reading previous metadata..."
        String prev = this.steps.readFile srcInfoFilePath
        Map<String,Object> map = CommonUtils.parseJsonString(prev)

        map.entrySet().each {e->
            _map.put(e.key, new DeploySource(e.value as Map))
        }
        return _map

    }


    private Map<String, DeployTarget> getDeployTargetMap(DeployTask deployTask) {

        Map<String, Run> prevRunMap = convertToJenkinsRun(deployTask.archive)

        def targetMap = [:] as Map<String, DeployTarget>

        if (!deployTask.rollBackMode) {

            for (int i = 0; i < deployTask.requestedApps.size(); i++) {

                DeployTarget target = new DeployTarget(appName: deployTask.requestedApps[i])

                String _branch = getBranch(deployTask.branchName, deployTask.custom, target.appName)
                String _srcModuleName = getSourceModuleName(deployTask.custom, target.appName)
                target.releaseNickname = getReleaseNickname(deployTask.releaseNumber, target.appName, deployTask.custom)

                target.prevSrcBuild = prevRunMap == null ? null: prevRunMap.get(target.appName)

                if (target.prevSrcBuild != null){
                    logger.info "[${target.appName}] " +
                            "Last deployed build: ${target.prevSrcBuild.parent.name} - " +
                            "#${target.prevSrcBuild.number}(${target.prevSrcBuild.displayName})"
                }

                def srcProjectName = "Build-${_branch}-${_srcModuleName}"

                logger.info "[${target.appName}] Retrieving data from '${srcProjectName}'..."

                retrieveLastSuccessfulSourceBuild(srcProjectName, target)

                if (deployTask.force) {
                    logger.info "[${target.appName}] 'force' option is enabled."
                    target.runDeploy = true
                } else if (target.prevSrcBuild == null ||
                        target.prevSrcBuild.parent.name != target.currentSrcBuild.parent.name ||
                        target.prevSrcBuild.number < target.currentSrcBuild.number) {
                    logger.info("[${target.appName}] Detected new build ${target.currentSrcBuild.parent.name} #${target.currentSrcBuild.number}")
                    target.runDeploy = true
                } else {
                    logger.info("[${target.appName}] No new build found from ${target.prevSrcBuild.parent.name}. Skipping ${target.appName} deployment...")
                }

                targetMap.put(target.appName, target)

            }
        } else {
            logger.info("Rollback requested. Utilizing previous deployment #{rollbackTarget}...")

            def rollbackSrcMap = loadFromArchive(deployTask.jobName, deployTask.rollBackNumber) as Map<String, Run>

            for (int i = 0; i < deployTask.requestedApps.size(); i++) {

                DeployTarget target = new DeployTarget(appName: deployTask.requestedApps[i])

//               def prevSrc = getPreviousSrc(appName, previousSrcMap)
                target.prevSrcBuild = prevRunMap.get(target.appName)
                target.currentSrcBuild = rollbackSrcMap.get(target.appName)

                target.runDeploy = true
                targetMap.put(target.appName, target)
            }

        }
        return targetMap
    }


    private Map<String, Run> convertToJenkinsRun(Map<String,DeploySource> map) {

        if(map==null) return null

        Map<String, Run> _map = new HashMap<>()
        map.entrySet().each {e->
            def project = Jenkins.instance.getItem(e.value.jobName) as Job
            _map.put e.key, project.getBuildByNumber(e.value.buildNumber)
        }

        return _map

    }



    private String getReleaseNickname(String defaultRelease, String appName, LinkedHashMap custom) {
        String _release = defaultRelease
        if (custom[appName] && custom[appName].release && custom[appName].release != deployTask.releaseNumber) {
            _release = custom[appName].release
            logger.info("Using custom release '${_release}' for ${appName}...")
        }
        return _release
    }


    private Object getSourceModuleName(LinkedHashMap custom, String appName) {
        def _srcProject = custom[appName] && custom[appName].srcProject ? custom[appName].srcProject : appName
        if(_srcProject!=appName) logger.info "[${appName}] Using custom source project '${_srcProject}'..."
        _srcProject
    }

    private String getBranch(String defaultBranch, LinkedHashMap custom, String appName) {
        def _branch = custom[appName] && custom[appName].branch ? custom[appName].branch : defaultBranch

        if (_branch!=defaultBranch) {
            logger.info("[${appName}] Using custom branch '${_branch}' ...")
        }
        _branch
    }

    boolean isGlassfishDeployment(DeployTask deployTask){
        deployTask.deploymentType == DeploymentType.GLASSFISH_APP
    }

    @NonCPS
    static void retrieveLastSuccessfulSourceBuild(String srcProjectName, DeployTarget target){

        DeploySource nextSource = new DeploySource()

        def project = Jenkins.instance.getItem(srcProjectName) as Job
        if(project == null) throw new IllegalArgumentException("${srcProjectName} does not exist.")

        Run<?,?> lastSuccessfulBuild = project.getLastSuccessfulBuild()
        if(lastSuccessfulBuild == null) throw new IllegalArgumentException("${srcProjectName} does not have successful build.")

        target.currentSrcBuild = lastSuccessfulBuild

        if(target.currentSrcBuild instanceof RunWithSCM){
            RunWithSCM<?,?> runWithSCM = target.currentSrcBuild as RunWithSCM<?, ?>

            target.addChangeLogSets(runWithSCM.changeSets)

            if(!target.prevSrcBuild || target.prevSrcBuild.number == 0) return

            if( target.prevSrcBuild.number >= target.currentSrcBuild.number-1) return

            for(def i=nextSource.buildNumber-1; i > target.prevSrcBuild.number; i--){
                Run<?,?> run = project.getBuildByNumber(i)
                if((run instanceof RunWithSCM)) {
                    RunWithSCM<?,?> _runWithSCM = run as RunWithSCM<?, ?>
                    target.addChangeLogSets(_runWithSCM.changeSets)
                }
            }
        }

    }


    void restartGlassfish(DeployTask deployTask) {
        stage('Restart') {
            if (!deployTask.hasDeployAppList()) return

            def PS_restart = "D:/jenkins_utils/deployment_v2/ps/Restart-GFServer.ps1 " +
                    "-env '${deployTask.environment}' -server '${deployTask.serverName}' -appList @(${deployTask.quotedDeployList})"
            if (deployTask.debug) logger.info("[Debug] Executed ${CommonUtils.getPSCmd(PS_restart)}")
            else steps.bat script: "${CommonUtils.getPSCmd(PS_restart)}"
        }
    }




    void deploy(DeployTask deployTask) {
        String timeStampStr = (new Date()).format("yyyy_MM_dd__HH_mm_ss")
        String workspace = steps.pwd()
        if (deployTask.sleepSecBeforeDeploy > 0) {
            sleep deployTask.sleepSecBeforeDeploy
        }

        logger.info(deployTask.deployAppNameList)
        logger.info("Copying artifacts...")
        for (def i = 0; i < deployTask.deployAppNameList.size(); i++) {
            String appName = deployTask.deployAppNameList[i]
            DeployTarget target =  deployTask.deployTargetMap.get(appName)

            deployApp(deployTask, target, workspace, timeStampStr)

            //merge previous and current source info

            if(!deployTask.archive.containsKey(appName)) deployTask.archive.put(appName, new DeploySource())
            deployTask.archive.get(appName).name = target.currentSrcBuild.parent.name
            deployTask.archive.get(appName).srcBuild = [
                    name: target.currentSrcBuild.displayName,
                    number: target.currentSrcBuild.number
            ]

            logger.info(deployTask.archive.get(appName))

            // save
            steps.writeFile file: "${workspace}/${Constants.OUT_DIR}/source_info.json", text: CommonUtils.objectToJsonString(deployTask.archive)

        }

        deployTask.endDate = new Date()

    }

    private deployApp(DeployTask deployTask, DeployTarget deployTarget, String workspace, String deployBuildNickName) {
        def userInput = true
        def didTimeout = false
        try {
//        timeout(time: Constants.WAIT_SEC_BTWN_DEPLOYMENT, unit: 'SECONDS') {
            steps.timeout(time:1, unit: 'SECONDS') {
                userInput = input(
                        id: 'Proceed1',
                        message: "Starting ${deployTarget.appName} deployment in 10 seconds.")
            }
        } catch (err) {
            def user = err.getCauses()[0].getUser()
            if ('SYSTEM' == user.toString()) { // SYSTEM means timeout.
                didTimeout = true
            } else {
                userInput = false
            }
            if (!userInput) steps.error message: "Aborted by: [${user}]"
        }

        steps.step([
                projectName         : deployTarget.currentSrcBuild.parent.name,
                $class              : 'CopyArtifact',
                filter              : '**/*.war, **/*.zip, **/*.jar',
                fingerprintArtifacts: true,
                flatten             : true,
                selector            : [$class: 'SpecificBuildSelector', buildNumber: "${deployTarget.currentSrcBuild.number}"]
        ])


        def PS_deploy = "D:/jenkins_utils/deployment_v2/ps/"

        switch (deployTask.deploymentType) {
            case DeploymentType.SPRING_APP:
                PS_deploy += "Deploy-SpringModules.ps1 "
                break
            case DeploymentType.NODE_APP:
                PS_deploy += "Deploy-NodeModules.ps1 "
                break
            case DeploymentType.GLASSFISH_APP:
                PS_deploy += "Deploy-Modules.ps1 "
                break
            default:
                throw steps.error("Not supported");
        }

        PS_deploy += "-env '${deployTask.environment}' -server '${deployTask.serverName}' -appList @('${deployTarget.appName}') -sourceDir ${workspace} -release '${deployTask.releaseNumber}' -buildName '${deployBuildNickName}'"

        if (deployTask.debug) logger.info "[Debug] Executed ${CommonUtils.getPSCmd(PS_deploy)}"
        else steps.bat script: "${CommonUtils.getPSCmd(PS_deploy)}"

    }

}
