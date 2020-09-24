import org.testcontainers.containers.MariaDBContainer

startupTimeout 3000 // this can take time

container(MariaDBContainer, 'mariadb:10.3.13') {
    def dbUser = 'username'
    def dbPassword = 'password'

    withDatabaseName 'idempotence4j_db'
    withUsername dbUser
    withPassword dbPassword
    exports {
        prop 'datasource.url', jdbcUrl + '?characterEncoding=UTF-8&useMysqlMetadata=true'
    }
}

cluster('database') {

}
