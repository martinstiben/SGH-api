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
                            
                            # Usar la rama actual del pipeline
                            if git clone -b ${env.BRANCH_NAME} https://github.com/martinstiben/SGH-api.git .; then
                                echo "✅ Clonado rama ${env.BRANCH_NAME} exitosamente"
                                echo "🎯 Pipeline ejecutándose en ambiente basado en rama ${env.BRANCH_NAME}"
                            else
                                echo "❌ No se pudo clonar la rama ${env.BRANCH_NAME}"
                                echo "💡 Verifica que la rama ${env.BRANCH_NAME} exista en el repositorio"
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

                    // Configurar archivos según el ambiente detectado
                    switch (env.ENVIRONMENT) {
                        case 'develop':
                            env.COMPOSE_FILE_DATABASE = "Devops/docker-compose-databases.yml"
                            env.COMPOSE_FILE_API = "Devops/docker-compose-apis.yml"
                            env.ENV_FILE = "Devops/develop/.env.dev"
                            break
                        case 'qa':
                            env.COMPOSE_FILE_DATABASE = "Devops/docker-compose-databases.yml"
                            env.COMPOSE_FILE_API = "Devops/docker-compose-apis.yml"
                            env.ENV_FILE = "Devops/qa/.env.qa"
                            break
                        case 'staging':
                            env.COMPOSE_FILE_DATABASE = "Devops/docker-compose-databases-staging.yml"
                            env.COMPOSE_FILE_API = "Devops/docker-compose-api-staging.yml"
                            env.ENV_FILE = "Devops/staging/.env.staging"
                            break
                        case 'prod':
                            env.COMPOSE_FILE_DATABASE = "Devops/docker-compose-databases.yml"
                            env.COMPOSE_FILE_API = "Devops/docker-compose-apis.yml"
                            env.ENV_FILE = "Devops/prod/.env.prod"
                            break
                    }

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
                        docker build -t sgh-api-${env.ENVIRONMENT}:latest -f Dockerfile .
                    """
                }
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

                    echo "📦 Levantando base de datos de ${env.ENVIRONMENT}..."
                    docker-compose -f ${env.COMPOSE_FILE_DATABASE} -p sgh-${env.ENVIRONMENT} up -d mysql-${env.ENVIRONMENT}

                    echo "⏳ Esperando que la base de datos esté lista..."
                    sleep 30

                    echo "🔍 Verificando que la base de datos esté corriendo:"
                    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep DB_${env.ENVIRONMENT}

                    echo "✅ Base de datos DB_${env.ENVIRONMENT} desplegada correctamente"
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

                    echo "📦 Levantando API de ${env.ENVIRONMENT}..."
                    docker-compose -f ${env.COMPOSE_FILE_API} -p sgh-${env.ENVIRONMENT} up -d sgh-api-${env.ENVIRONMENT}

                    echo "⏳ Esperando que la API esté lista..."
                    sleep 30

                    echo "🔍 Verificando contenedores que están corriendo:"
                    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

                    echo "✅ Despliegue completado - Contenedores de ${env.ENVIRONMENT}:"
                    echo "   🗄️ DB_${env.ENVIRONMENT} (Base de datos MySQL)"
                    echo "   🚀 API_${env.ENVIRONMENT} (Spring Boot API)"
                    echo ""
                """
            }
        }
    }

    post {
        success {
            script {
                def port = ""
                switch(env.ENVIRONMENT) {
                    case 'develop':
                        port = "8082"
                        break
                    case 'qa':
                        port = "8083"
                        break
                    case 'staging':
                        port = "8084"
                        break
                    case 'prod':
                        port = "8085"
                        break
                }
                echo "🎉 Despliegue de SGH completado correctamente para ${env.ENVIRONMENT}"
                echo "🌐 Tu API está disponible en: http://localhost:${port}"
                echo "📚 Swagger UI: http://localhost:${port}/swagger-ui/index.html"
                echo "🔍 Health check: http://localhost:${port}/actuator/health"
            }
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