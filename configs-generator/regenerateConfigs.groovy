def string = """
Вас приветствует генератор конфигов
"""

println string


// usage:
// ./mvnw -pl configs-generator generate-resources
// after usage you must do Build -> Rebuild project


def FRONTEND_MAIN_YML_FILE = "${project.basedir}/../frontend/src/main/resources/config/application.yml";
def FRONTEND_TEST_YML_FILE = "${project.basedir}/../frontend/src/test/resources/config/application.yml";
def INTEGRATION_TEST_YML_FILE = "${project.basedir}/../integration-test/src/test/resources/config/application.yml";

def TEST_TIME_PORT = 9080;

def AUTOGENERATE_SNIPPET =
"""# This file was autogenerated via configs-generator
# Please do not edit it manually.
""";

def writeAndLog(filePath, content) {
    def file = new File(filePath);
    file.withWriter('UTF-8') { writer ->
        writer.write(content)
    }
    println("""File ${file} was successfully saved!""");
};

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////// common snippets //////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

def DATA_STORE_SNIPPET = {String contexts, boolean dropFirst ->
return """
spring.jpa:
  properties:
    hibernate.use_sql_comments: true
    hibernate.format_sql: true
  hibernate.ddl-auto: validate

spring.datasource:
    url: jdbc:postgresql://172.22.0.2:5432/blog?connectTimeout=10
    username: blog
    password: "blogPazZw0rd"
    driverClassName: org.postgresql.Driver

liquibase:
  change-log: classpath:liquibase/migration.yml
  contexts: ${contexts}
  drop-first: ${dropFirst}

spring.redis.url: redis://172.22.0.3:6379/0
spring.data.redis.repositories.enabled: false
"""};

def WEBSERVER_SNIPPET =
"""
server.tomcat.basedir: \${java.io.tmpdir}/com.github.nikit.cpp.tomcat
server.session.store-dir: \${server.tomcat.basedir}/sessions
""";

def USERS_SNIPPET=
"""custom.it.user: admin
custom.it.password: admin
""";


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////// config files ///////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

def FRONTEND_MAIN_YML_CONTENT =
"""${AUTOGENERATE_SNIPPET}
logging.level.: INFO
logging.level.org.springframework.web.socket: TRACE
#logging.level.org.springframework.security: DEBUG
#logging.level.org.springframework.session: DEBUG
#logging.level.org.springframework.security.web: DEBUG
#logging.level.org.apache.catalina: TRACE
#logging.level.org.springframework.web: DEBUG
#logging.level.org.hibernate.SQL: DEBUG
#logging.level.org.hibernate.type: TRACE


server.tomcat.accesslog.enabled: true
server.tomcat.accesslog.pattern: '%t %a "%r" %s (%D ms)'
${WEBSERVER_SNIPPET}

# this is URL
spring.mvc.static-path-pattern: /**
# You need to remove "file:..." element for production or you can to remove spring.resources.static-locations
# first element - for eliminate manual restart app in IntelliJ for copy compiled js to target/classes, last slash is important,, second element - for documentation
spring.resources.static-locations: file:frontend/src/main/resources/static/, classpath:/static/

${DATA_STORE_SNIPPET('main', false)}
""";
writeAndLog(FRONTEND_MAIN_YML_FILE, FRONTEND_MAIN_YML_CONTENT);


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
def FRONTEND_TEST_YML_CONTENT =
"""${AUTOGENERATE_SNIPPET}
logging.level.: INFO

server.port: ${TEST_TIME_PORT}
${WEBSERVER_SNIPPET}
${USERS_SNIPPET}
${DATA_STORE_SNIPPET('main, test', true)}
""";
writeAndLog(FRONTEND_TEST_YML_FILE, FRONTEND_TEST_YML_CONTENT);

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
def INTEGRATION_TEST_YML_CONTENT =
"""${AUTOGENERATE_SNIPPET}
logging.level.: INFO

server.port: ${TEST_TIME_PORT}
${WEBSERVER_SNIPPET}
custom.selenium.implicitly-wait-timeout: 10
custom.selenium.browser: PHANTOM
custom.selenium.window-height: 900
custom.selenium.window-width: 1600

custom.it.url.prefix: http://127.0.0.1:\${server.port}
custom.it.user.id: 1
${USERS_SNIPPET}

# this is URL
# spring.mvc.static-path-pattern: /**
# You need to remove "file:..." element for production or you can to remove spring.resources.static-locations
# first element - for eliminate manual restart app in IntelliJ for copy compiled js to target/classes, last slash is important,, second element - for documentation
# spring.resources.static-locations: file:../frontend/src/main/resources/static/, classpath:/static/

${DATA_STORE_SNIPPET('main, test', false)}
""";
writeAndLog(INTEGRATION_TEST_YML_FILE, INTEGRATION_TEST_YML_CONTENT);