plugins {
    val kotlinVersion = "1.7.22"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("net.mamoe.mirai-console") version "2.14.0"
}

group = "io.github.absdf15"
version = "1.1.2"

repositories {
    mavenLocal()
    mavenCentral()
}
val ktorVersion = "2.2.4"
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    // mirai
    implementation(platform("net.mamoe:mirai-bom:2.14.0"))
    implementation("net.mamoe:mirai-console-compiler-common")
    testImplementation("net.mamoe:mirai-core-mock")
    testImplementation("net.mamoe:mirai-logging-slf4j")
    testImplementation(kotlin("test"))
    // ktor
    implementation(platform("io.ktor:ktor-bom:$ktorVersion"))
    implementation("io.ktor:ktor-client-okhttp")
    implementation("io.ktor:ktor-client-auth")
    implementation("io.ktor:ktor-client-encoding")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-serialization-gson")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.0")
    // hanLP分词
    implementation("com.hankcs:hanlp:portable-1.8.4")
    // 处理数学公式和markdown语法
    implementation("com.vladsch.flexmark:flexmark-all:0.64.2")
    implementation("org.scilab.forge:jlatexmath:1.0.7")
    // 浏览器
    compileOnly("xyz.cssxsh.mirai:mirai-selenium-plugin:2.3.0")
    testImplementation("xyz.cssxsh.mirai:mirai-selenium-plugin:2.3.0")
    // 审核类库
    implementation("io.github.toolgood:toolgood-words:3.1.0.0")
    // 核心前置
    compileOnly("io.github.absdf15.qbot.core:QBotCore:0.1.0")
}

repositories {
    mavenLocal()
    mavenCentral()
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
}
