pipeline {
    agent any

    environment {
        DEPLOY_DIR = '/srv/sto'
    }

    stages {
        stage('Pull') {
            steps {
                sh 'cd $DEPLOY_DIR && git pull'
            }
        }

        stage('Build & Deploy') {
            steps {
                sh 'cd $DEPLOY_DIR && docker compose build --no-cache'
                sh 'cd $DEPLOY_DIR && docker compose up -d'
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
