plugins {
    id("java")
}

group = "stoufexis"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
    implementation("io.aeron:aeron-client:1.52.2")
    implementation("io.aeron:aeron-driver:1.52.2")
    implementation("org.agrona:agrona:2.6.0")
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
