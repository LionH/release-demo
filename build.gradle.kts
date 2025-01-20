import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import java.security.MessageDigest

plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.4.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("maven-publish")
	id("signing")
}

group = "io.github.lionh"
version = System.getenv("GITHUB_REF_NAME")?.removePrefix("v") ?: "1.0.0"

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(21))
	}
	withJavadocJar()
	withSourcesJar()
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			from(components["java"])

			pom {
				name.set("Release Demo")
				description.set("A demo project for publishing to Maven Central")
				url.set("https://github.com/LionH/release-demo")
				licenses {
					license {
						name.set("MIT License")
						url.set("https://opensource.org/licenses/MIT")
					}
				}
				developers {
					developer {
						id.set("lionh")
						name.set("Lionel Tesolin")
						email.set("liontes@gmail.com")
					}
				}
				scm {
					connection.set("scm:git:git@github.com:LionH/release-demo.git")
					developerConnection.set("scm:git:ssh://git@github.com/LionH/release-demo.git")
					url.set("https://github.com/LionH/release-demo")
				}
			}
		}
	}
}

signing {
	useInMemoryPgpKeys(
		System.getenv("SIGNING_KEY"),
		System.getenv("SIGNING_PASSWORD")
	)
	sign(publishing.publications["mavenJava"])
}

tasks.register<Copy>("preparePomForBundle") {
	group = "publishing"
	description = "Prepares the POM file with the correct naming for the deployment bundle."

	val publicationDir = layout.buildDirectory.dir("publications/mavenJava")
	val targetDir = layout.buildDirectory.dir("deployment-artifacts/io/github/lionh/releasedemo/${project.version}")

	dependsOn("generatePomFileForMavenJavaPublication", "signMavenJavaPublication")

	from(publicationDir) {
		include("pom-default.xml", "pom-default.xml.asc")
		rename("pom-default.xml", "${project.name}-${project.version}.pom")
		rename("pom-default.xml.asc", "${project.name}-${project.version}.pom.asc")
	}
	into(targetDir)
}

tasks.register<Copy>("prepareArtifactsForBundle") {
	group = "publishing"
	description = "Prepares the JAR files for the deployment bundle."

	val libsDir = layout.buildDirectory.dir("libs")
	val targetDir = layout.buildDirectory.dir("deployment-artifacts/io/github/lionh/releasedemo/${project.version}")

	dependsOn("signMavenJavaPublication")

	from(layout.buildDirectory.dir("libs")) {
		include("**/*-javadoc.jar", "**/*-javadoc.jar.asc")
		include("**/*-plain.jar", "**/*-plain.jar.asc")
		include("**/*-sources.jar", "**/*-sources.jar.asc")
	}
	into(targetDir)
}

tasks.register("generateChecksums") {
	group = "publishing"
	description = "Generates MD5 and SHA1 checksums for deployment files."

	val targetDir = layout.buildDirectory.dir("deployment-artifacts/io/github/lionh/releasedemo/${project.version}")

	dependsOn("preparePomForBundle", "prepareArtifactsForBundle")
	doLast {
		targetDir.get().asFile.listFiles()?.forEach { file ->
			if (file.isFile && !file.name.endsWith(".md5") && !file.name.endsWith(".sha1")) {
				val md5 = file.readBytes().toMD5()
				file.resolveSibling("${file.name}.md5").writeText(md5)

				val sha1 = file.readBytes().toSHA1()
				file.resolveSibling("${file.name}.sha1").writeText(sha1)
			}
		}
	}
}

tasks.register<Zip>("createDeploymentBundle") {
	group = "publishing"
	description = "Creates the deployment bundle ZIP file for Sonatype."

	archiveFileName.set("${project.group}-${project.name}-${project.version}-bundle.zip")
	destinationDirectory.set(layout.buildDirectory.dir("deployment-bundle"))

	dependsOn("generateChecksums")

	from(layout.buildDirectory.dir("deployment-artifacts")) {
		include("**/*")
	}

	doLast {
		println("Deployment bundle created at: ${archiveFile.get().asFile.absolutePath}")
	}
}

tasks.register("uploadDeploymentBundle") {
	group = "publishing"
	description = "Uploads the deployment bundle ZIP file to Sonatype Publisher API."

	dependsOn("createDeploymentBundle")

	doLast {
		val httpClient: CloseableHttpClient = HttpClients.createDefault()
		val post = HttpPost("https://central.sonatype.com/api/v1/publisher/upload")
		val bundleFile = layout.buildDirectory.file("deployment-bundle/${project.group}-${project.name}-${project.version}-bundle.zip").get().asFile

		if (!bundleFile.exists()) {
			throw GradleException("Deployment bundle not found: ${bundleFile.absolutePath}")
		}

		// Add Authorization header
		val bearerToken = System.getenv("BEARER_TOKEN")
		if (bearerToken.isNullOrEmpty()) {
			throw GradleException("BEARER_TOKEN environment variable is not set.")
		}
		post.addHeader("Authorization", "Bearer $bearerToken")

		// Create the multipart entity
		val entity = MultipartEntityBuilder.create()
			.setContentType(org.apache.hc.core5.http.ContentType.MULTIPART_FORM_DATA)
			.addBinaryBody(
				"bundle",
				bundleFile,
				org.apache.hc.core5.http.ContentType.APPLICATION_OCTET_STREAM,
				bundleFile.name
			)
			.build()

		post.entity = entity

		// Log the request details
		println("===== HTTP Request =====")
		println("URL: ${post.uri}")
		println("Method: ${post.method}")
		post.headers.forEach { header ->
			println("  ${header.name}: ${header.value}")
		}
		println("Uploading file: ${bundleFile.absolutePath}")
		println("File size: ${bundleFile.length()} bytes")

		// Execute the request
		println("\n===== Sending Request =====")
		httpClient.execute(post).use { response ->
			val statusCode = response.code
			val responseBody = response.entity.content.reader().readText()

			// Log the response details
			println("\n===== HTTP Response =====")
			println("Status Code: $statusCode")
			println("Reason Phrase: ${response.reasonPhrase}")
			println("Response Body: $responseBody")

			// Throw an exception if the status code is not successful
			if (statusCode !in 200..299) {
				throw GradleException("Failed to upload bundle: $statusCode ${response.reasonPhrase}\n$responseBody")
			}
		}
	}
}

// Helper functions for checksum generation
fun ByteArray.toMD5(): String = MessageDigest.getInstance("MD5")
	.digest(this)
	.joinToString("") { "%02x".format(it) }

fun ByteArray.toSHA1(): String = MessageDigest.getInstance("SHA-1")
	.digest(this)
	.joinToString("") { "%02x".format(it) }
