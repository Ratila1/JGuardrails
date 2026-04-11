dependencies {
    api(project(":jguardrails-core"))
    api(project(":jguardrails-detectors"))
    compileOnly("org.springframework.ai:spring-ai-core:1.0.0-M6")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.2.5")
    testImplementation("ch.qos.logback:logback-classic:1.5.6")
}
