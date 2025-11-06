pipeline {
    agent any

    options {
        // Deshabilitar el checkout automático de Jenkins
        skipDefaultCheckout()
        // Timeout general del pipeline
        timeout(time: 20, unit: 'MINUTES')
    }

    environment {
        PROJECT_PATH = 'Backend/SGH'
        ENVIRONMENT = 'qa'  // Forzar ambiente QA
    }

    stages {

        stage('Limpiar y Checkout del código') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        echo "🧹 Limpiando workspace completamente..."
                        deleteDir()
                        
                        echo "📥 Obteniendo código del repositorio..."
                        sh '''
                            echo "🔄 Clonando repositorio desde GitHub..."
                            
                            # Intentar con la rama qa primero
                            if git clone -b qa https://github.com/martinstiben/SGH-api.git .; then
                                echo "✅ Clonado rama qa exitosamente"
                            else
                                echo "⚠️ Fallo al clonar rama qa, intentando main..."
                                if git clone https://github.com/martinstiben/SGH-api.git .; then
                                    if git branch -r | grep -q "origin/main"; then
                                        echo "🔀 Cambiando a rama main..."
                                        git checkout main
                                    elif git branch -r | grep -q "origin/master"; then
                                        echo "🔀 Cambiando a rama master..."
                                        git checkout master
                                    else
                                        echo "📍 Usando rama por defecto"
                                    fi
                                    echo "✅ Clonado repositorio exitosamente"
                                else
                                    echo "❌ No se pudo clonar el repositorio"
                                    exit 1
                                fi
                            fi
                            
                            echo "📁 Verificando estructura del repositorio:"
                            ls -la
                        '''
                    }
                }
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

                    echo "🔍 Verificando estructura del workspace..."
                    sh '''
                        echo "📁 Contenido actual del directorio:"
                        ls -la
                        echo "📂 Verificando directorio Backend/SGH:"
                        if [ -d "Backend/SGH" ]; then
                            echo "✅ Backend/SGH encontrado"
                        else
                            echo "❌ Backend/SGH no encontrado"
                            echo "🔍 Listando contenido de .:"
                            ls -la
                            echo "💡 ERROR: La estructura del repositorio no es correcta"
                        fi
                        echo "📂 Verificando directorio Devops:"
                        if [ -d "Devops" ]; then
                            echo "✅ Devops encontrado"
                        else
                            echo "❌ Devops no encontrado"
                            echo "🔍 Contenido actual:"
                            ls -la
                        fi
                    '''

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
            echo "🔍 Revisa los logs arriba para más detalles"
        }
        always {
            echo "🧹 Limpieza final del pipeline completada."
        }
    }
}
