package com.gsihealth.jenkins.utils

import com.gsihealth.jenkins.Common;

class EmailListBuilder implements Serializable{

    def list = []
    def steps

    EmailListBuilder(steps){this.steps=steps}

    EmailListBuilder CDTeam(){
        list+=Common.getCD()
        return this
    }

    EmailListBuilder DBTeam(){
        list+=Common.getDBTeam()
        return this
    }

    EmailListBuilder DevLead(){
        list+=Common.getDevLead()
        return this
    }

    EmailListBuilder add(String... addList){
        list+=addList
        return this
    }

    EmailListBuilder requestedUser(){
        list+=CommonUtils.getRequestedUser(steps)
        return this
    }


    EmailListBuilder developers(){
        list+=CommonUtils.getDevelopers(steps)
        return this
    }


    EmailListBuilder culprits(){
        list+=CommonUtils.getCulprits(steps)
        return this
    }


    String[] build(){
        return list.unique()
    }


}
