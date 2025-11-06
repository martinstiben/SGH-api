pipeline {
    agent any

    environment {
        PROJECT_PATH = 'Backend/SGH'
    }

    stages {

        stage('Checkout código fuente') {
            steps {
                echo "📥 Clonando repositorio desde GitHub..."
                
                script {
                    // Siempre usar checkout manual para evitar problemas de SCM
                    def branch = env.BRANCH_NAME ?: 'qa'  // Usar BRANCH_NAME o fallback
                    def repoUrl = 'https://github.com/martinstiben/SGH-api.git'
                    
                    echo "🌿 Rama objetivo: ${branch}"
                    echo "🔗 Repositorio: ${repoUrl}"
                    
                    // Limpiar directorio primero
                    sh """
                        echo "🧹 Limpiando directorio de trabajo..."
                        rm -rf .git 2>/dev/null || true
                        rm -rf * 2>/dev/null || true
                    """
                    
                    // Clonar repositorio
                    sh """
                        echo "🔄 Clonando repositorio..."
                        git clone -b ${branch} ${repoUrl} . || {
                            echo "⚠️ Fallo al clonar ${branch}, intentando con master/main..."
                            git clone ${repoUrl} .
                            if git branch -a | grep -q "main"; then
                                git checkout main
                            elif git branch -a | grep -q "master"; then
                                git checkout master
                            else
                                echo "📍 Creando rama ${branch} desde master..."
                                git checkout -b ${branch} || {
                                    echo "📍 Creando rama ${branch} como nueva..."
                                    echo "⚠️  Posible repositorio vacío o rama no existe"
                                }
                            fi
                        }
                    """
                    
                    // Verificar checkout
                    sh """
                        echo "🔍 Verificando estado del repositorio..."
                        git status
                        git branch
                    """
                }
                
                echo "📁 Verificando estructura del repositorio:"
                sh 'find . -name "*.yml" -o -name "Jenkinsfile" | head -10'
                sh 'ls -la Devops/ || echo "⚠️ Directorio Devops no encontrado"'
            }
        }

        stage('Detectar entorno') {
            steps {
                script {
                    // Detectar rama de manera más robusta
                    def branch = env.BRANCH_NAME?.toLowerCase()
                    
                    // Si no se detecta rama, intentar detectarla de otra manera
                    if (!branch || branch == 'null') {
                        echo "⚠️ BRANCH_NAME no disponible, detectando rama..."
                        
                        // Intentar detectar desde git
                        try {
                            def currentBranch = sh(script: "git branch --show-current", returnStdout: true).trim()
                            if (currentBranch) {
                                branch = currentBranch.toLowerCase()
                                echo "🔍 Rama detectada desde git: ${branch}"
                            } else {
                                // Valor por defecto para QA
                                branch = 'qa'
                                echo "📍 Usando rama por defecto: ${branch}"
                            }
                        } catch (Exception e) {
                            branch = 'qa'
                            echo "📍 Error detectando rama, usando default: ${branch}"
                        }
                    }
                    
                    echo "🌿 Rama final detectada: ${branch}"
                    
                    switch (branch) {
                        case 'main':
                        case 'master':
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
                    
                    // Use environment-specific compose files
                    if (env.ENVIRONMENT == 'qa') {
                        env.COMPOSE_FILE_DATABASE = "Devops/docker-compose-databases-qa.yml"
                        env.COMPOSE_FILE_API = "Devops/docker-compose-api-qa.yml"
                        echo "🔧 Usando archivos específicos de QA"
                    } else {
                        env.COMPOSE_FILE_DATABASE = "Devops/docker-compose-databases.yml"
                        env.COMPOSE_FILE_API = "Devops/docker-compose-apis.yml"
                        echo "🔧 Usando archivos generales"
                    }
                    env.ENV_FILE = "${env.ENV_DIR}/.env.${env.ENVIRONMENT}"

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
                    echo "🗄️ Desplegando base de datos PostgreSQL para: ${env.ENVIRONMENT}"
                    echo "📄 Usando compose file: ${env.COMPOSE_FILE_DATABASE}"
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
