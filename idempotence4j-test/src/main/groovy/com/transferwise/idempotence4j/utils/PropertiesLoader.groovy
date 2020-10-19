package com.transferwise.idempotence4j.utils;

class PropertiesLoader {

    static Properties loadProperties(String resourceFileName) throws IOException {
        def configuration = new Properties()
        def inputStream = PropertiesLoader.class.getClassLoader().getResourceAsStream(resourceFileName)
        configuration.load(inputStream)
        inputStream.close()

        return configuration
    }
}
