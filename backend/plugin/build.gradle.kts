/*
 * Copyright 2015-2025 Ritense BV, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

val jakartaInjectVersion: String by project
val jakartaPersistenceVersion: String by project
val kotlinLoggingVersion: String by project
val mockitoKotlinVersion: String by project
val okhttpVersion: String by project
val openApiGeneratorPluginVersion: String by project
val squareupMoshiVersion: String by project

plugins {
    id("org.openapi.generator") version "7.13.0"
}

dockerCompose {
    setProjectName("xential")
    isRequiredBy(project.tasks.integrationTesting)

    tasks.integrationTesting {
        useComposeFiles.addAll("$rootDir/docker-resources/docker-compose-base-test.yml")
    }
}

dependencies {
    compileOnly("org.springframework.security:spring-security-config")
    compileOnly("org.springframework.security:spring-security-web")

    compileOnly("com.ritense.valtimo:core")
    compileOnly("com.ritense.valtimo:plugin-valtimo")
    compileOnly("com.ritense.valtimo:temporary-resource-storage")
    compileOnly("com.ritense.valtimo:value-resolver")
    compileOnly("com.ritense.valtimo:zaken-api")
    compileOnly("com.ritense.valtimo:documenten-api")
    compileOnly("com.ritense.valtimo:smartdocuments")

    compileOnly("com.ritense.valtimoplugins:mTLS-SSLContext:1.0.1")

    compileOnly("org.springframework.boot:spring-boot-starter-webflux")
    compileOnly("org.springframework.boot:spring-boot-starter-data-jpa")

    compileOnly("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")

    compileOnly("jakarta.inject:jakarta.inject-api:$jakartaInjectVersion")
    compileOnly("jakarta.persistence:jakarta.persistence-api:$jakartaPersistenceVersion")

    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.moshi:moshi:$squareupMoshiVersion")
    implementation("com.squareup.moshi:moshi-kotlin:$squareupMoshiVersion")

    implementation("org.apache.httpcomponents.core5:httpcore5")
    implementation("org.apache.httpcomponents.client5:httpclient5")

    implementation("org.openapitools:openapi-generator-gradle-plugin:$openApiGeneratorPluginVersion")

    // Testing
    testImplementation("com.ritense.valtimo:building-block")
    testImplementation("com.ritense.valtimo:contract")
    testImplementation("com.ritense.valtimo:core")
    testImplementation("com.ritense.valtimo:plugin")
    testImplementation("com.ritense.valtimo:temporary-resource-storage")
    testImplementation("com.ritense.valtimo:test-utils-common")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation("org.postgresql:postgresql")

    testImplementation("com.ritense.valtimo:plugin-valtimo")
    testImplementation("com.ritense.valtimo:value-resolver")
    testImplementation("com.ritense.valtimo:zaken-api")
    testImplementation("com.ritense.valtimo:documenten-api")
    testImplementation("com.ritense.valtimo:smartdocuments")
    testImplementation("com.ritense.valtimoplugins:mTLS-SSLContext:1.0.1")

    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:$okhttpVersion")
}

configurations.named("testImplementation") {
    exclude(group = "org.slf4j", module = "slf4j-simple")
}

apply(from = "gradle/publishing.gradle")

openApiGenerate {
    generatorName = "kotlin"
    inputSpec = "$rootDir/backend/plugin/src/main/resources/dcsg_xential.yaml"
    outputDir = "${getLayout().buildDirectory.get()}/generated"
    generateApiDocumentation = false
    generateApiTests = false
    generateModelDocumentation = false
    generateModelTests = false
    apiPackage = "com.rotterdam.esb.xential.api"
    invokerPackage = "com.rotterdam.esb.xential.invoker"
    modelPackage = "com.rotterdam.esb.xential.model"
    configOptions =
        mapOf(
            "useSpringBoot3" to "true",
            "library" to "jvm-spring-restclient",
            "serializationLibrary" to "jackson",
        )
}

sourceSets {
    main {
        java {
            srcDir("${getLayout().buildDirectory.get()}/generated/src/main")
        }
    }
}
tasks.named("compileKotlin") {
    dependsOn(
        "openApiGenerate",
    )
}

tasks.named("sourcesJar") {
    dependsOn(
        "openApiGenerate",
    )
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude { it.file.path.contains("/generated/") }
    }
}
