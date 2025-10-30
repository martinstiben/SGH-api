pipeline {
    agent any

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk-amd64'
        MAVEN_HOME = '/opt/maven'
        PATH = "${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${PATH}"
        PROJECT_PATH = 'Backend/SGH'
    }

    stages {

        // =======================================================
        // 1️⃣ VERIFICAR CÓDIGO FUENTE
        // =======================================================
        stage('Verificar código fuente') {
            steps {
                echo "📁 Repositorio ya clonado por Jenkins (Multibranch)"
                sh 'ls -R Devops || true'
            }
        }

        // =======================================================
        // 2️⃣ DETECTAR ENTORNO SEGÚN LA RAMA
        // =======================================================
        stage('Detectar entorno') {
            steps {
                script {
                    switch (env.BRANCH_NAME) {
                        case 'main':
                            env.ENVIRONMENT = 'prod'
                            break
                        case 'Staging':
                            env.ENVIRONMENT = 'staging'
                            break
                        case 'QA':
                            env.ENVIRONMENT = 'qa'
                            break
                        default:
                            env.ENVIRONMENT = 'develop'
                            break
                    }

                    env.ENV_DIR = "Devops/${env.ENVIRONMENT}"
                    env.COMPOSE_FILE = "${env.ENV_DIR}/Docker-Compose.yml"
                    env.ENV_FILE = "${env.ENV_DIR}/.env.${env.ENVIRONMENT}"

                    echo """
                    ✅ Rama detectada: ${env.BRANCH_NAME}
                    🌎 Entorno asignado: ${env.ENVIRONMENT}
                    📄 Compose file: ${env.COMPOSE_FILE}
                    📁 Env file: ${env.ENV_FILE}
                    """

                    if (!fileExists(env.COMPOSE_FILE)) {
                        error "❌ No se encontró ${env.COMPOSE_FILE}"
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

        // =======================================================
        // 3️⃣ COMPILAR JAVA CON MAVEN
        // =======================================================
        stage('Compilar Java con Maven') {
            steps {
                script {
                    docker.image('maven:3.9.4-openjdk-17-slim')
                        .inside('-v /var/run/docker.sock:/var/run/docker.sock -u root:root') {
                        sh """
                            echo "🔧 Compilando proyecto Java con Maven..."
                            cd ${PROJECT_PATH}
                            mvn clean compile -DskipTests
                            mvn package -DskipTests
                        """
                    }
                }
            }
        }

        // =======================================================
        // 4️⃣ CONSTRUIR IMAGEN DOCKER
        // =======================================================
        stage('Construir imagen Docker') {
            steps {
                dir("${PROJECT_PATH}") {
                    sh """
                        echo "🐳 Construyendo imagen Docker para SGH (${env.ENVIRONMENT})"
                        docker build -t sgh-api-${env.ENVIRONMENT}:latest -f Dockerfile .
                    """
                }
            }
        }

        // =======================================================
        // 5️⃣ DESPLEGAR CON DOCKER COMPOSE
        // =======================================================
        stage('Desplegar SGH') {
            steps {
                sh """
                    echo "🚀 Desplegando entorno: ${env.ENVIRONMENT}"
                    docker compose -f ${env.COMPOSE_FILE} --env-file ${env.ENV_FILE} up -d --build
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
