pipeline {
    agent any

    environment {
        DEPLOY_PATH = '/srv/stone/app/STO'
        GIT_BRANCH  = 'dev'
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    stages {
        stage('Deploy') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'deploy-ssh-key',
                        keyFileVariable: 'SSH_KEY',
                        usernameVariable: 'DEPLOY_USER'
                    )
                ]) {
                    sh '''
                        ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} "
                            cd ${DEPLOY_PATH} &&
                            git pull origin ${GIT_BRANCH} &&
                            docker compose up -d --build
                        "
                    '''
                }
            }
        }
    }

    post {
        success {
            echo '배포 성공'
        }
        failure {
            echo '배포 실패'
        }
    }
}
