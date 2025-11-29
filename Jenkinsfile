pipeline {
    agent any

    options {
        // Timeout general del pipeline
        timeout(time: 20, unit: 'MINUTES')
        // Log rotation
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    environment {
        PROJECT_PATH = 'Backend/SGH'
    }

    stages {

        stage('Checkout código fuente') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        echo "🧹 Limpiando workspace completamente..."
                        deleteDir()
                        
                        echo "📥 Clonando repositorio desde GitHub..."
                        sh '''
                            echo "🔄 Verificando ramas disponibles en el repositorio..."
                            
                            # Intentar listar las ramas disponibles
                            git ls-remote --heads https://github.com/martinstiben/SGH-api.git
                            
                            echo "🔄 Intentando clonar la rama más apropiada..."
                            
                            # SOLO usar la rama Staging - es independiente
                            if git clone -b Staging https://github.com/martinstiben/SGH-api.git .; then
                                echo "✅ Clonado rama Staging exitosamente"
                                echo "🎯 Pipeline ejecutándose en ambiente Staging (independiente)"
                            else
                                echo "❌ No se pudo clonar la rama Staging"
                                echo "💡 La rama Staging debe existir para ejecutar este pipeline de Staging"
                                echo "🔧 Verifica que la rama 'Staging' esté creada en el repositorio"
                                exit 1
                            fi
                            
                            echo "📁 Verificando estructura del repositorio:"
                            ls -la
                        '''
                    }
                }
            }
        }

        stage('Detectar entorno') {
            steps {
                script {
                    // Forzar Staging como el usuario solicitó - este pipeline es específico para Staging
                    env.ENVIRONMENT = 'staging'

                    env.ENV_DIR = "Devops/${env.ENVIRONMENT}"
                    env.COMPOSE_FILE_DATABASE = "Devops/docker-compose-databases.yml"
                    env.COMPOSE_FILE_API = "Devops/docker-compose-apis.yml"
                    env.DB_SERVICE = "mysql-staging"
                    env.ENV_FILE = "${env.ENV_DIR}/.env.staging"

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
                            PORT=8084
                            DB_URL=jdbc:mysql://mysql-staging:3306/DB_SGH_Staging
                            DB_USER=sgh_user
                            DB_PASSWORD=stg_C0mpl3x_K3y_2024
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
                        # Asegurar que Docker tenga acceso a internet
                        docker build --network host -t sgh-api-${env.ENVIRONMENT}:latest -f Dockerfile .
                    """
                }
            }
        }

        stage('Crear Redes Docker') {
            steps {
                sh """
                    echo "🌐 Creando redes Docker"
                    docker network create --driver bridge network_staging || echo "Red network_staging ya existe"
                    echo "✅ Redes creadas correctamente"
                """
            }
        }

        stage('Limpiar Base de Datos') {
            steps {
                sh """
                    echo "🗄️ Limpiando base de datos MySQL para: ${env.ENVIRONMENT}"
                    echo "🧹 Eliminando volumen de datos anterior para fresh start..."

                    # Eliminar el volumen anterior para start limpio
                    docker volume rm mysql_data_staging 2>/dev/null || true

                    echo "✅ Volumen de base de datos limpio - listo para fresh start"
                """
            }
        }

        stage('Desplegar Base de Datos') {
            steps {
                sh """
                    echo "🗄️ Desplegando base de datos MySQL para: ${env.ENVIRONMENT}"
                    echo "📄 Usando compose file: ${env.COMPOSE_FILE_DATABASE}"
                    echo "📁 Ubicación actual: \$(pwd)"

                    # Limpiar contenedores anteriores para evitar conflictos
                    echo "🧹 Limpiando contenedores anteriores de base de datos..."
                    docker-compose -f ${env.COMPOSE_FILE_DATABASE} -p sgh-${env.ENVIRONMENT} down 2>/dev/null || true

                    echo "📦 Levantando base de datos MySQL de Staging..."
                    docker-compose -f ${env.COMPOSE_FILE_DATABASE} -p sgh-${env.ENVIRONMENT} up -d mysql-staging

                    echo "⏳ Esperando que la base de datos esté lista..."
                    sleep 60

                    echo "🔍 Verificando que la base de datos esté corriendo:"
                    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep DB_Staging

                    echo "✅ Base de datos DB_Staging desplegada correctamente en puerto: 3309"
                """
            }
        }

        stage('Desplegar SGH Backend') {
            steps {
                sh """
                    echo "🚀 Desplegando backend SGH API para: ${env.ENVIRONMENT}"
                    echo "📄 Usando compose file: ${env.COMPOSE_FILE_API}"

                    # Limpiar contenedores anteriores para evitar conflictos
                    echo "🧹 Limpiando contenedores anteriores de API..."
                    docker-compose -f ${env.COMPOSE_FILE_API} -p sgh-${env.ENVIRONMENT} down 2>/dev/null || true

                    echo "📦 Levantando API de Staging..."
                    docker-compose -f ${env.COMPOSE_FILE_API} -p sgh-${env.ENVIRONMENT} up -d sgh-api-staging

                    echo "⏳ Esperando que la API esté lista..."
                    sleep 90

                    echo "🔍 Verificando contenedores que están corriendo:"
                    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

                    echo "✅ Despliegue completado - Contenedores de Staging:"
                    echo "   🗄️ DB_Staging (Base de datos MySQL)"
                    echo "   🚀 API_Staging (Spring Boot API)"
                    echo ""
                    echo "🌐 Swagger UI disponible en:"
                    echo "   http://localhost:8084/swagger-ui/index.html"
                    echo "🔗 Health check:"
                    echo "   http://localhost:8084/actuator/health"
                    echo "🗄️ Base de datos MySQL en puerto: 3309"
                """
            }
        }
    }

    post {
        success {
            echo "🎉 Despliegue de SGH completado correctamente para ${env.ENVIRONMENT}"
            echo "🌐 Tu API está disponible en: http://localhost:8084"
            echo "📚 Swagger UI: http://localhost:8084/swagger-ui/index.html"
            echo "🔍 Health check: http://localhost:8084/actuator/health"
            echo "🗄️ Base de datos MySQL: localhost:3309"
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