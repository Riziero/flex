node('me') {

    stage ('Clone') {
        checkout scm
        bat 'gradlew clean'
        bat 'gradlew testFX test'
        junit '**/build/test-results/**/TEST-*.xml'
    }

    stage('echo'){
        echo 'King is cool!'
    }

    stage('zip_it'){
        bat 'gradlew buildreportZip'
        archiveArtifacts 'buildreporter.zip'
    }

}
