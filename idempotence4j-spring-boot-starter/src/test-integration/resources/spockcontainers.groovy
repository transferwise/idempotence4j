import org.testcontainers.containers.PostgreSQLContainer

startupTimeout 3000 // this can take time

container(PostgreSQLContainer, 'postgres:10.9') {
    def dbUser = 'username'
    def dbPassword = 'password'

    withDatabaseName 'idempotence4j_db'
    withUsername dbUser
    withPassword dbPassword
    exports {
        prop 'DATASOURCE_URL', jdbcUrl
        prop 'DATASOURCE_USERNAME', dbUser
        prop 'DATASOURCE_PASSWORD', dbPassword
    }
}

cluster('database') {

}
