package com.gsihealth.jenkins.utils

def info(message){
    if(message instanceof String) {
        echo message
    }else if (message instanceof Map){
        echo CommonUtils.objectToJsonString(message)
    }else print message
}


