pipeline {
    agent any

    environment {
        PROJECT_PATH = 'Backend/SGH'
    }

    stages {

        stage('Checkout código fuente') {
            steps {
                echo "📥 Clonando repositorio desde GitHub..."
                checkout scm
                sh 'ls -R Devops || true'
            }
        }

        stage('Detectar entorno') {
            steps {
                script {
                    def branch = env.BRANCH_NAME?.toLowerCase()
                    switch (branch) {
                        case 'main':
                            env.ENVIRONMENT = 'prod'
                            break
                        case 'staging':
                            env.ENVIRONMENT = 'staging'
                            break
                        case 'qa':
                            env.ENVIRONMENT = 'qa'
                            break
                        default:
                            env.ENVIRONMENT = 'develop'
                            break
                    }

                    env.ENV_DIR = "Devops/${env.ENVIRONMENT}"
                    if (env.ENVIRONMENT == "staging") {
                        env.COMPOSE_FILE = "Devops/docker-compose-api-staging.yml:Devops/docker-compose-databases-staging.yml"
                        env.COMPOSE_FILE1 = "Devops/docker-compose-api-staging.yml"
                        env.COMPOSE_FILE2 = "Devops/docker-compose-databases-staging.yml"
                    } else {
                        env.COMPOSE_FILE = "${env.ENV_DIR}/Docker-Compose.yml"
                        env.COMPOSE_FILE1 = env.COMPOSE_FILE
                    }
                    env.ENV_FILE = "${env.ENV_DIR}/.env.${env.ENVIRONMENT}"

                    echo """
                    ✅ Rama detectada: ${env.BRANCH_NAME}
                    🌎 Entorno asignado: ${env.ENVIRONMENT}
                    📄 Compose file: ${env.COMPOSE_FILE}
                    📁 Env file: ${env.ENV_FILE}
                    """

                    if (env.ENVIRONMENT == "staging") {
                        if (!fileExists(env.COMPOSE_FILE1)) {
                            error "❌ No se encontró ${env.COMPOSE_FILE1}"
                        }
                        if (!fileExists(env.COMPOSE_FILE2)) {
                            error "❌ No se encontró ${env.COMPOSE_FILE2}"
                        }
                    } else {
                        if (!fileExists(env.COMPOSE_FILE)) {
                            error "❌ No se encontró ${env.COMPOSE_FILE}"
                        }
                    }

                    if (!fileExists(env.ENV_FILE)) {
                        echo "⚠️ Archivo de entorno no encontrado, creando uno temporal..."
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
            agent {
                docker {
                    image 'maven:3.9.6-eclipse-temurin-17'
                    args '-v /root/.m2:/root/.m2'
                    reuseNode true
                }
            }
            steps {
                dir("${PROJECT_PATH}") {
                    sh '''
                        echo "🔧 Compilando proyecto Java con Maven..."
                        mvn clean compile -DskipTests
                        mvn package -DskipTests
                    '''
                }
            }
        }

        stage('Construir imagen Docker') {
            steps {
                script {
                    // Determinar el perfil de Spring Boot según el entorno
                    def springProfile = "dev"
                    if (env.ENVIRONMENT == "prod") {
                        springProfile = "prod"
                    } else if (env.ENVIRONMENT == "staging") {
                        springProfile = "staging"
                    } else if (env.ENVIRONMENT == "qa") {
                        springProfile = "qa"
                    }

                    dir("${PROJECT_PATH}") {
                        sh """
                            echo "🐳 Construyendo imagen Docker para SGH (${env.ENVIRONMENT}) con perfil: ${springProfile}"
                            docker build -t sgh-api-${env.ENVIRONMENT}:latest -f Dockerfile .
                        """
                    }
                }
            }
        }

        stage('Desplegar SGH') {
            steps {
                sh """
                    echo "🚀 Desplegando entorno: ${env.ENVIRONMENT}"
                    docker-compose -f ${env.COMPOSE_FILE} --env-file ${env.ENV_FILE} up -d --build
                """
            }
        }
    }

    post {
        success {
            echo "🎉 Despliegue de SGH completado correctamente para ${env.ENVIRONMENT}"
        }
        failure {
            echo "💥 Error durante el despliegue de SGH en ${env.ENVIRONMENT}"
        }
        always {
            echo "🧹 Limpieza final del pipeline completada."
        }
    }
}