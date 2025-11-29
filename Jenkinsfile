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
                    env.COMPOSE_FILE_DATABASE = "Devops/docker-compose-databases.yml"
                    env.COMPOSE_FILE_API = "Devops/docker-compose-apis.yml"
                    if (env.ENVIRONMENT == 'develop') {
                        env.DB_SERVICE = "mysql-develop"
                    } else {
                        env.DB_SERVICE = "postgres-${env.ENVIRONMENT}"
                    }
                    switch (env.ENVIRONMENT) {
                        case 'develop':
                            env.ENV_FILE = "${env.ENV_DIR}/.env.dev"
                            break
                        case 'qa':
                            env.ENV_FILE = "${env.ENV_DIR}/.env.qa"
                            break
                        case 'staging':
                            env.ENV_FILE = "${env.ENV_DIR}/.env.staging"
                            break
                        case 'prod':
                            env.ENV_FILE = "${env.ENV_DIR}/.env.prod"
                            break
                        default:
                            env.ENV_FILE = "${env.ENV_DIR}/.env.dev"
                            break
                    }

                    echo """
                    ✅ Rama detectada: ${env.BRANCH_NAME}
                    🌎 Entorno asignado: ${env.ENVIRONMENT}
                    📄 Database Compose file: ${env.COMPOSE_FILE_DATABASE}
                    📄 API Compose file: ${env.COMPOSE_FILE_API}
                    📁 Env file: ${env.ENV_FILE}
                    """

                    if (!fileExists(env.COMPOSE_FILE_DATABASE)) {
                        error "❌ No se encontró ${env.COMPOSE_FILE_DATABASE}"
                    }
                    
                    if (!fileExists(env.COMPOSE_FILE_API)) {
                        error "❌ No se encontró ${env.COMPOSE_FILE_API}"
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
                dir("${PROJECT_PATH}") {
                    sh """
                        echo "🐳 Construyendo imagen Docker para SGH (${env.ENVIRONMENT})"
                        docker build -t sgh-api-${env.ENVIRONMENT}:latest -f Dockerfile .
                    """
                }
            }
        }

        stage('Desplegar Base de Datos') {
            steps {
                sh """
                    echo "🗄️ Desplegando base de datos MySQL para: ${env.ENVIRONMENT}"
                    echo "📄 Usando compose file: ${env.COMPOSE_FILE_DATABASE}"
                    echo "📁 Ubicación actual: \$(pwd)"
                    ls -la Devops/ || { echo "❌ No se encontró el directorio Devops"; exit 1; }
                    docker-compose -f ${env.COMPOSE_FILE_DATABASE} -p sgh-${env.ENVIRONMENT} up -d mysql-${env.ENVIRONMENT}
                    echo "✅ Base de datos desplegada correctamente"
                """
            }
        }

        stage('Desplegar SGH Backend') {
            steps {
                sh """
                    echo "🚀 Desplegando backend SGH API para: ${env.ENVIRONMENT}"
                    echo "📦 Desplegando solo el contenedor de la API..."
                    echo "📄 Usando compose file: ${env.COMPOSE_FILE_API}"
                    
                    # Asegurar que la base de datos esté funcionando antes de desplegar la API
                    echo "🔍 Verificando estado de la base de datos..."
                    sleep 60
                    
                    docker-compose -f ${env.COMPOSE_FILE_API} -p sgh-${env.ENVIRONMENT} up -d sgh-api-${env.ENVIRONMENT}
                    echo "✅ API desplegada correctamente"
                    echo "🌐 Swagger UI disponible en:"
                    case ${env.ENVIRONMENT} in
                        "develop")
                            echo "   http://localhost:8082/swagger-ui/index.html"
                            ;;
                        "qa")
                            echo "   http://localhost:8083/swagger-ui/index.html"
                            ;;
                        "staging")
                            echo "   http://localhost:8084/swagger-ui/index.html"
                            ;;
                        "prod")
                            echo "   http://localhost:8085/swagger-ui/index.html"
                            ;;
                    esac
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