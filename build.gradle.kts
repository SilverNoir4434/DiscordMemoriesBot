plugins {
    application
    id("com.gradleup.shadow") version "8.3.1"
}

application.mainClass = "org.github.SilverNoir4434"
group = "org.github.SilverNoir4434"
version = "1.0.0"

val jdaVersion = "6.3.1" //

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("net.dv8tion:JDA:${jdaVersion}")
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
    implementation("org.quartz-scheduler:quartz:2.5.2")
    implementation("ch.qos.logback:logback-classic:1.5.25")
}


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isIncremental = true

    // Set this to the version of java you want to use,
    // the minimum required for JDA is 1.8
    sourceCompatibility = "1.8"
}

tasks.test {
    useJUnitPlatform()
}