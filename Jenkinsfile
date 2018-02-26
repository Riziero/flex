node('me') {

    stage ('Clone') {
        checkout scm
        bat 'gradlew testFX test'
        // junit '**/build/test-results/**/TEST-*.xml'
    }

    stage('echo'){
        echo 'King is cool!'
    }

    stage('zip_it'){
        bat 'gradlew buildreportZip'
        // archiveArtifacts 'build/libs//*.war'
    }

}
