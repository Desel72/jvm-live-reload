plugins {
    id("core-java-library")
}

dependencies {
    api(project(":core:build-link"))
    implementation("io.grpc:grpc-netty-shaded:1.60.1")
    implementation("io.grpc:grpc-stub:1.60.1")
    implementation("io.grpc:grpc-services:1.60.1")
}

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            artifactId = "jvm-live-reload-webserver-grpc"
            pom {
                name = "jvm-live-reload-webserver-grpc"
                description = "Development-mode GRPC proxy server for Live Reload experience on JVM"
            }
        }
    }
}
