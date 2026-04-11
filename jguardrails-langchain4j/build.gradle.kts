dependencies {
    api(project(":jguardrails-core"))
    api(project(":jguardrails-detectors"))
    compileOnly("dev.langchain4j:langchain4j-core:0.32.0")
    testImplementation("ch.qos.logback:logback-classic:1.5.6")
}
