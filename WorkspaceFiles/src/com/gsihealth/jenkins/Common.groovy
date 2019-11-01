
package com.gsihealth.jenkins


//def CD_TEAM = def DEV_TEAM = ['beth.boose@gsihealth.com', 'vigneshwar.thirumal@gsihealth.com','mani.rajappa@gsihealth.com']

static getCD (){
    return ["ContinuousDelivery@gsihealth.com"]
}

static getDevLead(){
    return [
            "development@gsihealth.com"
            ]
}

static getIt(){
    return [
            "IT@gsihealth.com"
            ]
}

static getTransformers(){
    return [
            "transformers@gsihealth.com"
            ]
}

static getDBTeam(){
    return [
            //"Franklin.Azarak@gsihealth.com",
            //"Immanuel.Washington@gsihealth.com",
            "Sadha.Sivim@gsihealth.com"

    ]

}