pipeline {
    agent any

    environment {
        PROJECT_PATH = 'Backend/SGH'
        ENVIRONMENT = 'qa'  // Forzar ambiente QA
    }

    stages {

        stage('Limpiar y Checkout Manual') {
            steps {
                echo "🧹 Limpiando workspace..."
                deleteDir()
                
                echo "📥 Haciendo checkout manual del repositorio..."
                sh """
                    echo "🔄 Clonando repositorio desde GitHub..."
                    git clone -b qa https://github.com/martinstiben/SGH-api.git . || {
                        echo "⚠️ Fallo al clonar rama qa, intentando main..."
                        git clone https://github.com/martinstiben/SGH-api.git .
                        if git branch -a | grep -q "main"; then
                            git checkout main
                        elif git branch -a | grep -q "master"; then
                            git checkout master
                        else
                            echo "📍 Repositorio no tiene rama qa/main/master, usando lo que hay"
                        fi
                    }
                    
                    echo "📁 Verificando estructura del repositorio:"
                    ls -la
                """
            }
        }

        stage('Configurar entorno QA') {
            steps {
                script {
                    env.ENV_DIR = "Devops/qa"
                    env.COMPOSE_FILE_DATABASE = "Devops/docker-compose-databases-qa.yml"
                    env.COMPOSE_FILE_API = "Devops/docker-compose-api-qa.yml"
                    env.ENV_FILE = "${env.ENV_DIR}/.env.qa"

                    echo """
                    ✅ Configuración para QA
                    🌎 Entorno forzado: ${env.ENVIRONMENT}
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
                        echo "⚠️ Archivo de entorno no encontrado, usando valores por defecto..."
                        // Los valores están en el .env.qa que ya debe existir
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
                    echo "🗄️ Desplegando base de datos PostgreSQL para: ${env.ENVIRONMENT}"
                    echo "📄 Usando compose file: ${env.COMPOSE_FILE_DATABASE}"
                    echo "📁 Ubicación actual: \$(pwd)"
                    ls -la Devops/ || { echo "❌ No se encontró el directorio Devops"; exit 1; }
                    cd Devops
                    docker-compose -f ${env.COMPOSE_FILE_DATABASE} -p sgh-${env.ENVIRONMENT} up -d postgres-${env.ENVIRONMENT}
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
                    sleep 10
                    
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
