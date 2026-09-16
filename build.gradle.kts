plugins {
    `java-library`
    jacoco
    id("com.modrinth.minotaur") version "2.8.10"
}

group = "com.sekailabs.kyouyuu"
version = "1.3.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")
    implementation("org.postgresql:postgresql:42.7.5")
    implementation("com.mysql:mysql-connector-j:9.2.0")
    implementation("com.zaxxer:HikariCP:6.2.1")
    testCompileOnly("org.projectlombok:lombok:1.18.36")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.36")
    testImplementation("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    testImplementation("net.luckperms:api:5.4")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testImplementation("org.xerial:sqlite-jdbc:3.49.1.0")
    testImplementation("org.postgresql:postgresql:42.7.5")
    testImplementation("com.mysql:mysql-connector-j:9.2.0")
    testImplementation("com.zaxxer:HikariCP:6.2.1")
    testImplementation("com.h2database:h2:2.3.232")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(project.findProperty("modrinth.projectId") as String? ?: System.getenv("MODRINTH_PROJECT_ID") ?: "kyouyuu")
    versionNumber.set(project.version.toString())
    versionType.set("release")
    uploadFile.set(tasks.jar)
    gameVersions.addAll(listOf("1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "26.1", "26.1.1", "26.1.2"))
    loaders.addAll(listOf("paper", "purpur"))
    syncBodyFrom.set(rootProject.file("README.md").readText())
}
