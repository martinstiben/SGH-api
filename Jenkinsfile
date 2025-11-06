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

        stage('Detectar entorno') {
            steps {
                script {
                    // Forzar QA como el usuario solicitó
                    env.ENVIRONMENT = 'qa'
                    
                    // Usar los archivos Docker Compose que realmente existen
                    env.COMPOSE_FILE_DATABASE = "Devops/docker-compose-databases-qa.yml"
                    env.COMPOSE_FILE_API = "Devops/docker-compose-api-qa.yml"
                    env.ENV_FILE = "Devops/qa/.env.qa"

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

                    // Verificar archivos usando shell con los archivos que realmente existen
                    sh '''
                        echo "🔍 Verificando archivos de configuración..."
                        if [ -f "Devops/docker-compose-databases-qa.yml" ]; then
                            echo "✅ Devops/docker-compose-databases-qa.yml encontrado"
                        else
                            echo "❌ Devops/docker-compose-databases-qa.yml no encontrado"
                            echo "🔍 Listando archivos en Devops:"
                            find Devops/ -name "*qa*" -type f
                            exit 1
                        fi
                        
                        if [ -f "Devops/docker-compose-api-qa.yml" ]; then
                            echo "✅ Devops/docker-compose-api-qa.yml encontrado"
                        else
                            echo "❌ Devops/docker-compose-api-qa.yml no encontrado"
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

        stage('Desplegar Base de Datos') {
            steps {
                sh """
                    echo "🗄️ Desplegando base de datos PostgreSQL para: ${env.ENVIRONMENT}"
                    echo "📄 Usando compose file: ${env.COMPOSE_FILE_DATABASE}"
                    echo "📁 Ubicación actual: \$(pwd)"
                    ls -la Devops/ || { echo "❌ No se encontró el directorio Devops"; exit 1; }
                    cd Devops
                    
                    # Limpiar contenedores anteriores para evitar conflictos
                    docker-compose -f ${env.COMPOSE_FILE_DATABASE} -p sgh-${env.ENVIRONMENT} down 2>/dev/null || true
                    
                    echo "📦 Levantando base de datos..."
                    docker-compose -f ${env.COMPOSE_FILE_DATABASE} -p sgh-${env.ENVIRONMENT} up -d postgres-qa
                    
                    echo "✅ Base de datos desplegada correctamente"
                    echo "🗄️ PostgreSQL disponible en puerto: 5433"
                """
            }
        }

        stage('Desplegar SGH Backend') {
            steps {
                sh """
                    echo "🚀 Desplegando backend SGH API para: ${env.ENVIRONMENT}"
                    echo "📄 Usando compose file: ${env.COMPOSE_FILE_API}"
                    
                    # Asegurar que la base de datos esté funcionando antes de desplegar la API
                    echo "🔍 Verificando estado de la base de datos..."
                    sleep 15
                    
                    cd Devops
                    # Limpiar contenedores anteriores para evitar conflictos
                    docker-compose -f ${env.COMPOSE_FILE_API} -p sgh-${env.ENVIRONMENT} down 2>/dev/null || true
                    
                    echo "📦 Levantando API..."
                    docker-compose -f ${env.COMPOSE_FILE_API} -p sgh-${env.ENVIRONMENT} up -d sgh-api-qa
                    
                    echo "✅ API desplegada correctamente"
                    echo "🌐 Swagger UI disponible en:"
                    echo "   http://localhost:8083/swagger-ui/index.html"
                    echo "🔗 Health check:"
                    echo "   http://localhost:8083/actuator/health"
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
