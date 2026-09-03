plugins {
    id("java")
    application
}

group = "stoufexis"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    implementation("org.json:json:20260814")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs.add("--enable-preview")
}

tasks.compileJava {
    options.compilerArgs.add("--enable-preview")
}

application {
    mainClass.set("stoufexis.jarpc.gen.Cli")
}