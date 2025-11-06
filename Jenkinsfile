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
                            echo "🔄 Clonando repositorio desde GitHub..."
                            
                            # Solo intentar con la rama qa
                            if git clone -b qa https://github.com/martinstiben/SGH-api.git .; then
                                echo "✅ Clonado rama qa exitosamente"
                            else
                                echo "❌ No se pudo clonar la rama qa. Repositorio no tiene rama qa o no tienes acceso."
                                echo "💡 Asegúrate de que el repositorio tenga una rama 'qa' y tengas permisos de lectura."
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
                    // Forzar QA como el usuario solicitó
                    env.ENVIRONMENT = 'qa'
                    
                    // Usar la estructura real del repositorio: Docker-Compose.yml por ambiente
                    env.COMPOSE_FILE_DATABASE = "Devops/qa/Docker-Compose.yml"
                    env.COMPOSE_FILE_API = "Devops/qa/Docker-Compose.yml"
                    env.ENV_FILE = "Devops/qa/.env.qa"

                    echo """
                    ✅ Entorno forzado: ${env.ENVIRONMENT}
                    📄 Compose file: ${env.COMPOSE_FILE_DATABASE}
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
                        
                        # Verificar el Docker Compose de QA
                        if [ -f "Devops/qa/Docker-Compose.yml" ]; then
                            echo "✅ Devops/qa/Docker-Compose.yml encontrado"
                            echo "📄 Servicios definidos en el Docker Compose:"
                            grep -A 2 "container_name:" Devops/qa/Docker-Compose.yml || grep "    [a-zA-Z]" Devops/qa/Docker-Compose.yml
                        else
                            echo "❌ Devops/qa/Docker-Compose.yml no encontrado"
                            echo "🔍 Listando estructura completa de qa:"
                            ls -la Devops/qa/
                            echo "🔍 Buscando todos los archivos Docker-Compose en Devops:"
                            find Devops/ -name "Docker-Compose.yml" -type f
                            exit 1
                        fi
                        
                        if [ -f "Devops/qa/.env.qa" ]; then
                            echo "✅ Devops/qa/.env.qa encontrado"
                        else
                            echo "❌ Devops/qa/.env.qa no encontrado"
                            echo "🔍 Listando contenido de Devops/qa:"
                            ls -la Devops/qa/
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

        stage('Desplegar servicios QA') {
            steps {
                sh """
                    echo "🚀 Desplegando servicios SGH para: ${env.ENVIRONMENT}"
                    echo "📄 Usando compose file: ${env.COMPOSE_FILE_DATABASE}"
                    echo "📁 Ubicación actual: \$(pwd)"
                    
                    # Navegar al directorio QA
                    cd Devops/qa
                    
                    # Mostrar los servicios que se van a levantar
                    echo "📄 Servicios definidos en Docker-Compose.yml:"
                    grep -A 1 "container_name:" Docker-Compose.yml
                    
                    # Limpiar contenedores anteriores para evitar conflictos
                    echo "🧹 Limpiando contenedores anteriores..."
                    docker-compose -f Docker-Compose.yml -p sgh-${env.ENVIRONMENT} down 2>/dev/null || true
                    
                    echo "📦 Levantando servicios de QA..."
                    docker-compose -f Docker-Compose.yml -p sgh-${env.ENVIRONMENT} up -d
                    
                    echo "⏳ Esperando que los servicios estén listos..."
                    sleep 15
                    
                    echo "🔍 Verificando contenedores que están corriendo:"
                    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
                    
                    echo "✅ Despliegue completado - Contenedores de QA:"
                    echo "   🗄️ DB_QA (Base de datos PostgreSQL)"
                    echo "   🚀 API_QA (Spring Boot API)"
                    echo ""
                    echo "🌐 Swagger UI disponible en:"
                    echo "   http://localhost:8083/swagger-ui/index.html"
                    echo "🔗 Health check:"
                    echo "   http://localhost:8083/actuator/health"
                    echo "🗄️ Base de datos PostgreSQL en puerto: 5433"
                """
            }
        }
    }

    post {
        success {
            echo "🎉 Despliegue de SGH completado correctamente para ${env.ENVIRONMENT}"
            echo "🌐 Tu API está disponible en: http://localhost:8083"
            echo "📚 Swagger UI: http://localhost:8083/swagger-ui/index.html"
            echo "🔍 Health check: http://localhost:8083/actuator/health"
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
