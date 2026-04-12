pipeline {
    agent any
    environment {
        DEPLOY_PATH = "${env.DEPLOY_PATH}"
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build Backend') {
            steps {
                sh 'cd server/main && ./gradlew clean bootJar'
                sh 'cd server/match && ./gradlew clean bootJar'
                sh 'cd server/batch && ./gradlew clean bootJar'
            }
        }
        stage('Build Frontend') {
            steps {
                sh '''
                    cd client/web
                    echo "VITE_API_BASE_URL=http://www.shinhan6th.com" > .env.production
                    npm install && npm run build
                '''
            }
        }
        stage('Deploy') {
            steps {
                sh '''
                    cp server/main/build/libs/*.jar ${DEPLOY_PATH}/main/app.jar
                    cp server/match/build/libs/*.jar ${DEPLOY_PATH}/match/app.jar
                    cp server/batch/build/libs/*.jar ${DEPLOY_PATH}/batch/app.jar
                    cp -r client/web/dist/* ${DEPLOY_PATH}/frontend/dist/
                    cd ${DEPLOY_PATH} && docker compose up -d --build main match batch
                    docker restart nginx
                '''
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
