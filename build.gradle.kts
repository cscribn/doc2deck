plugins {
    application
    java
}

group = "com.appfire"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.poi:poi-ooxml:5.4.1")
    implementation("org.docx4j:docx4j-JAXB-ReferenceImpl:11.5.7")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.24.3")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.17.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.17.0")
}

application {
    mainClass.set("com.appfire.presentation.Application")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
