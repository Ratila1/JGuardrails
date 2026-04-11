plugins {
    application
}

dependencies {
    implementation(project(":jguardrails-core"))
    implementation(project(":jguardrails-detectors"))
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

// Позволяет передавать -PmainClass=... при запуске
application {
    mainClass.set(providers.gradleProperty("mainClass").orElse("io.jguardrails.examples.BasicExample"))
}
