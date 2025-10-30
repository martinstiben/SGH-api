pipeline {
    agent any

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk-amd64'
        MAVEN_HOME = '/opt/maven'
        PATH = "${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${PATH}"
        PROJECT_PATH = 'Backend/SGH'
    }

    stages {

        stage('Verificar código fuente') {
            steps {
                echo "📁 Jenkins ya hizo el checkout automáticamente"
                sh 'ls -la'
            }
        }

        stage('Detectar entorno') {
            steps {
                script {
                    switch (env.BRANCH_NAME) {
                        case 'main': env.ENVIRONMENT = 'prod'; break
                        case 'Staging': env.ENVIRONMENT = 'staging'; break
                        case 'QA': env.ENVIRONMENT = 'qa'; break
                        default: env.ENVIRONMENT = 'develop'; break
                    }

                    env.ENV_DIR = "Devops/${env.ENVIRONMENT}"
                    env.COMPOSE_FILE = "${env.ENV_DIR}/Docker-Compose.yml"
                    env.ENV_FILE = "${env.ENV_DIR}/.env.${env.ENVIRONMENT}"

                    echo """
                    ✅ Rama: ${env.BRANCH_NAME}
                    🌎 Entorno: ${env.ENVIRONMENT}
                    📄 Compose: ${env.COMPOSE_FILE}
                    📁 Env: ${env.ENV_FILE}
                    """

                    if (!fileExists(env.COMPOSE_FILE)) {
                        error "❌ Falta ${env.COMPOSE_FILE}"
                    }

                    if (!fileExists(env.ENV_FILE)) {
                        echo "⚠️ Generando archivo de entorno temporal..."
                        writeFile file: env.ENV_FILE, text: '''
                            PORT=8080
                            DB_HOST=localhost
                            DB_USER=admin
                            DB_PASS=secret
                        '''
                    }
                }
            }
        }

        stage('Compilar Java con Maven') {
            steps {
                script {
                    docker.image('maven:3.9.4-openjdk-17-slim')
                        .inside('-v /var/run/docker.sock:/var/run/docker.sock -u root:root') {
                        sh """
                            echo "🔧 Compilando con Maven..."
                            cd ${PROJECT_PATH}
                            mvn clean compile -DskipTests
                            mvn package -DskipTests
                        """
                    }
                }
            }
        }

        stage('Construir imagen Docker') {
            steps {
                dir("${PROJECT_PATH}") {
                    sh """
                        echo "🐳 Construyendo imagen SGH (${env.ENVIRONMENT})"
                        docker build -t sgh-api-${env.ENVIRONMENT}:latest -f Dockerfile .
                    """
                }
            }
        }

        stage('Desplegar SGH') {
            steps {
                sh """
                    echo "🚀 Desplegando SGH en ${env.ENVIRONMENT}"
                    docker compose -f ${env.COMPOSE_FILE} --env-file ${env.ENV_FILE} up -d --build
                """
            }
        }
    }

    post {
        success {
            echo "🎉 Despliegue exitoso en ${env.ENVIRONMENT}"
        }
        failure {
            echo "💥 Fallo en el despliegue de SGH en ${env.ENVIRONMENT}"
        }
        always {
            echo "🧹 Limpieza final completada."
        }
    }
}
