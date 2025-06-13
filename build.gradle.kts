plugins {
  kotlin("jvm") version "2.1.20"
  id("java")
  id("org.jetbrains.intellij") version "1.17.3" // 使用已知的稳定版本
}

intellij {
  localPath.set("/Applications/Android Studio.app/Contents") // 使用已知的稳定版本
  type.set("AI")          // 制定打开IDE类型是 android studio
  plugins.set(listOf("org.jetbrains.android")) // 使用正确的插件名称
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  testImplementation(kotlin("test"))
  implementation(kotlin("stdlib"))
  // 添加 HTTP 客户端库（如 OkHttp）
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("org.json:json:20231013")
}

tasks.test {
  useJUnitPlatform()
}

kotlin {
  jvmToolchain(17)
}