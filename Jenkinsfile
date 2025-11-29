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

                            # SOLO usar la rama main - es independiente
                            if git clone -b main https://github.com/martinstiben/SGH-api.git .; then
                                echo "✅ Clonado rama main exitosamente"
                                echo "🎯 Pipeline ejecutándose en ambiente Production (independiente)"
                            else
                                echo "❌ No se pudo clonar la rama main"
                                echo "💡 La rama main debe existir para ejecutar este pipeline de Production"
                                echo "🔧 Verifica que la rama 'main' esté creada en el repositorio"
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
                    // Forzar Production como el usuario solicitó - este pipeline es específico para Production
                    env.ENVIRONMENT = 'prod'

                    env.ENV_DIR = "Devops/${env.ENVIRONMENT}"
                    env.COMPOSE_FILE_DATABASE = "Devops/docker-compose-databases.yml"
                    env.COMPOSE_FILE_API = "Devops/docker-compose-apis.yml"
                    env.DB_SERVICE = "mysql-prod"
                    env.ENV_FILE = "${env.ENV_DIR}/.env.prod"

                    echo """

                    ✅ Rama detectada: ${env.BRANCH_NAME}
                    🌎 Entorno asignado: ${env.ENVIRONMENT}
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
                            echo "💡 ERROR: La estructura del repositorio no es correcta"
                            exit 1
                        fi
                        echo "📂 Verificando directorio Devops:"
                        if [ -d "Devops" ]; then
                            echo "✅ Devops encontrado"
                            echo "📁 Contenido de Devops:"
                            ls -la Devops/
                        else
                            echo "❌ Devops no encontrado"
                            echo "💡 ERROR: La estructura del repositorio no es correcta"
                            exit 1
                        fi
                    '''

                    // Verificar archivos usando la estructura real del repositorio
                    sh """
                        echo "🔍 Verificando archivos de configuración..."

                        # Verificar el Docker Compose de Base de Datos
                        if [ -f "${env.COMPOSE_FILE_DATABASE}" ]; then
                            echo "✅ ${env.COMPOSE_FILE_DATABASE} encontrado"
                            echo "📄 Servicio de base de datos definido:"
                            grep -A 1 "container_name:" ${env.COMPOSE_FILE_DATABASE} | head -5
                        else
                            echo "❌ ${env.COMPOSE_FILE_DATABASE} no encontrado"
                            exit 1
                        fi

                        # Verificar el Docker Compose de API
                        if [ -f "${env.COMPOSE_FILE_API}" ]; then
                            echo "✅ ${env.COMPOSE_FILE_API} encontrado"
                            echo "📄 Servicio de API definido:"
                            grep -A 1 "container_name:" ${env.COMPOSE_FILE_API} | head -5
                        else
                            echo "❌ ${env.COMPOSE_FILE_API} no encontrado"
                            exit 1
                        fi

                        if [ -f "${env.ENV_FILE}" ]; then
                            echo "✅ ${env.ENV_FILE} encontrado"
                        else
                            echo "❌ ${env.ENV_FILE} no encontrado"
                            exit 1
                        fi
                    """
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
                    docker network create --driver bridge network_prod || echo "Red network_prod ya existe"
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
                    docker volume rm mysql_data_${env.ENVIRONMENT} 2>/dev/null || true

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

                    echo "📦 Levantando base de datos MySQL de Production..."
                    docker-compose -f ${env.COMPOSE_FILE_DATABASE} -p sgh-${env.ENVIRONMENT} up -d mysql-prod

                    echo "⏳ Esperando que la base de datos esté lista..."
                    sleep 60

                    echo "🔍 Verificando que la base de datos esté corriendo:"
                    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep DB_Prod

                    echo "✅ Base de datos DB_Prod desplegada correctamente en puerto: 3310"
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

                    echo "📦 Levantando API de Production..."
                    docker-compose -f ${env.COMPOSE_FILE_API} -p sgh-${env.ENVIRONMENT} up -d sgh-api-prod

                    echo "⏳ Esperando que la API esté lista..."
                    sleep 90

                    echo "🔍 Verificando contenedores que están corriendo:"
                    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

                    echo "✅ Despliegue completado - Contenedores de Production:"
                    echo "   🗄️ DB_Prod (Base de datos MySQL)"
                    echo "   🚀 API_Prod (Spring Boot API)"
                    echo ""
                    echo "🌐 Swagger UI disponible en:"
                    echo "   http://localhost:8085/swagger-ui/index.html"
                    echo "🔗 Health check:"
                    echo "   http://localhost:8085/actuator/health"
                    echo "🗄️ Base de datos MySQL en puerto: 3310"
                """
            }
        }
    }

    post {
        success {
            echo "🎉 Despliegue de SGH completado correctamente para ${env.ENVIRONMENT}"
            echo "🌐 Tu API está disponible en: http://localhost:8085"
            echo "📚 Swagger UI: http://localhost:8085/swagger-ui/index.html"
            echo "🔍 Health check: http://localhost:8085/actuator/health"
            echo "🗄️ Base de datos MySQL: localhost:3310"
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