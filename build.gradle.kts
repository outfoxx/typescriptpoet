import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `java-library`
  jacoco
  `maven-publish`
  signing

  kotlin("jvm") version "1.7.10"
  id("org.jetbrains.dokka") version "1.7.10"

  id("net.minecrell.licenser") version "0.4.1"
  id("org.jmailen.kotlinter") version "3.3.0"
  id("com.github.breadmoirai.github-release") version "2.2.12"
}


val releaseVersion: String by project
val isSnapshot = releaseVersion.endsWith("SNAPSHOT")


group = "io.outfoxx"
version = releaseVersion
description = "A Kotlin/Java API for generating .ts source files."


//
// DEPENDENCIES
//

// Versions

val guavaVersion = "31.1-jre"
val junitJupiterVersion = "5.6.2"
val hamcrestVersion = "1.3"

repositories {
  mavenCentral()
  jcenter()
}

dependencies {

  //
  // LANGUAGES
  //

  // kotlin
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlin:kotlin-reflect")

  //
  // MISCELLANEOUS
  //

  implementation("com.google.guava:guava:$guavaVersion")

  //
  // TESTING
  //

  // junit
  testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
  testImplementation("org.hamcrest:hamcrest-all:$hamcrestVersion")

}


//
// COMPILE
//

val javaVersion = JavaVersion.VERSION_1_8

java {
  sourceCompatibility = javaVersion
  targetCompatibility = javaVersion

  withSourcesJar()
  withJavadocJar()
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = "$javaVersion"
    }
  }
}


//
// TEST
//

jacoco {
  toolVersion = "0.8.7"
}

tasks {
  test {
    useJUnitPlatform()

    finalizedBy(jacocoTestReport)
    jacoco {}
  }

  jacocoTestReport {
    dependsOn(test)
  }
}


//
// DOCS
//

tasks {
  dokkaHtml {
    outputDirectory.set(file("$buildDir/javadoc/$releaseVersion"))
  }

  javadoc {
    dependsOn(dokkaHtml)
  }
}


//
// CHECKS
//

kotlinter {
  indentSize = 2
}

license {
  header = file("HEADER.txt")
  include("**/*.kt")
}


//
// PUBLISHING
//

publishing {

  publications {

    create<MavenPublication>("library") {
      from(components["java"])

      pom {

        name.set("TypeScript Poet")
        description.set("TypeScriptPoet is a Kotlin and Java API for generating .ts source files.")
        url.set("https://github.com/outfoxx/typescriptpoet")

        organization {
          name.set("Outfox, Inc.")
          url.set("https://outfoxx.io")
        }

        issueManagement {
          system.set("GitHub")
          url.set("https://github.com/outfoxx/typescriptpoet/issues")
        }

        licenses {
          license {
            name.set("Apache License 2.0")
            url.set("https://raw.githubusercontent.com/outfoxx/typescriptpoet/master/LICENSE.txt")
            distribution.set("repo")
          }
        }

        scm {
          url.set("https://github.com/outfoxx/typescriptpoet")
          connection.set("scm:https://github.com/outfoxx/typescriptpoet.git")
          developerConnection.set("scm:git@github.com:outfoxx/typescriptpoet.git")
        }

        developers {
          developer {
            id.set("kdubb")
            name.set("Kevin Wooten")
            email.set("kevin@outfoxx.io")
          }
        }

      }
    }

  }

  repositories {

    maven {
      name = "MavenCentral"
      val snapshotUrl = "https://oss.sonatype.org/content/repositories/snapshots/"
      val releaseUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
      url = uri(if (isSnapshot) snapshotUrl else releaseUrl)

      credentials {
        username = project.findProperty("ossrhUsername")?.toString()
        password = project.findProperty("ossrhPassword")?.toString()
      }
    }

  }

}

signing {
  if (!hasProperty("signing.keyId")) {
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
  }
  sign(publishing.publications["library"])
}

tasks.withType<Sign>().configureEach {
  onlyIf { !isSnapshot }
}


//
// RELEASING
//

githubRelease {
  owner("outfoxx")
  repo(name)
  tagName("v$releaseVersion")
  targetCommitish("main")
  releaseName("🎉 $releaseVersion Release")
  draft(true)
  prerelease(!releaseVersion.matches("""^\d+\.\d+\.\d+$""".toRegex()))
  releaseAssets(
    files("$buildDir/libs/${name}-${releaseVersion}*.jar")
  )
  overwrite(true)
  token(project.findProperty("github.token") as String? ?: System.getenv("GITHUB_TOKEN"))
}

tasks {

  register("publishMavenRelease") {
    dependsOn(
      "publishAllPublicationsToMavenCentralRepository"
    )
  }

  register("publishRelease") {
    dependsOn(
      "publishMavenRelease",
      "githubRelease"
    )
  }

}
