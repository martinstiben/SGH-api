pipeline {
    agent any

    options {
        // Deshabilitar el checkout automático de Jenkins
        skipDefaultCheckout()
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
                    // Forzar Staging como el usuario solicitó
                    env.ENVIRONMENT = 'staging'
                    
                    // Usar los archivos Docker Compose correctos como en develop
                    env.COMPOSE_FILE_DATABASE = "Devops/docker-compose-databases-staging.yml"
                    env.COMPOSE_FILE_API = "Devops/docker-compose-api-staging.yml"
                    env.ENV_FILE = "Devops/staging/.env.staging"

                    echo """
                    ✅ Entorno forzado: ${env.ENVIRONMENT}
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
                    sh '''
                        echo "🔍 Verificando archivos de configuración..."
                        
                        # Verificar el Docker Compose de Base de Datos
                        if [ -f "Devops/docker-compose-databases-staging.yml" ]; then
                            echo "✅ Devops/docker-compose-databases-staging.yml encontrado"
                            echo "📄 Servicio de base de datos definido:"
                            grep -A 1 "container_name:" Devops/docker-compose-databases-staging.yml | head -5
                        else
                            echo "❌ Devops/docker-compose-databases-staging.yml no encontrado"
                            exit 1
                        fi
                        
                        # Verificar el Docker Compose de API
                        if [ -f "Devops/docker-compose-api-staging.yml" ]; then
                            echo "✅ Devops/docker-compose-api-staging.yml encontrado"
                            echo "📄 Servicio de API definido:"
                            grep -A 1 "container_name:" Devops/docker-compose-api-staging.yml | head -5
                        else
                            echo "❌ Devops/docker-compose-api-staging.yml no encontrado"
                            exit 1
                        fi
                        
                        if [ -f "Devops/staging/.env.staging" ]; then
                            echo "✅ Devops/staging/.env.staging encontrado"
                        else
                            echo "❌ Devops/staging/.env.staging no encontrado"
                            exit 1
                        fi
                    '''
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
                    
                    # Limpiar contenedores anteriores para evitar conflictos
                    echo "🧹 Limpiando contenedores anteriores de base de datos..."
                    docker-compose -f ${env.COMPOSE_FILE_DATABASE} -p sgh-${env.ENVIRONMENT} down 2>/dev/null || true
                    
                    echo "📦 Levantando base de datos de Staging..."
                    docker-compose -f ${env.COMPOSE_FILE_DATABASE} -p sgh-${env.ENVIRONMENT} up -d postgres-staging
                    
                    echo "⏳ Esperando que la base de datos esté lista..."
                    sleep 10
                    
                    echo "🔍 Verificando que la base de datos esté corriendo:"
                    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep DB_Staging
                    
                    echo "✅ Base de datos DB_Staging desplegada correctamente en puerto: 5434"
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
                    sleep 15
                    
                    echo "🔍 Verificando contenedores que están corriendo:"
                    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
                    
                    echo "✅ Despliegue completado - Contenedores de Staging:"
                    echo "   🗄️ DB_Staging (Base de datos PostgreSQL)"
                    echo "   🚀 API_Staging (Spring Boot API)"
                    echo ""
                    echo "🌐 Swagger UI disponible en:"
                    echo "   http://localhost:8084/swagger-ui/index.html"
                    echo "🔗 Health check:"
                    echo "   http://localhost:8084/actuator/health"
                    echo "🗄️ Base de datos PostgreSQL en puerto: 5434"
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
        }
        failure {
            echo "💥 Error durante el despliegue de SGH en ${env.ENVIRONMENT}"
            echo "🔍 Revisa los logs arriba para más detalles"
        }
        always {
            echo "🧹 Limppieza final del pipeline completada."
        }
    }
}