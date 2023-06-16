
# 使用 lib-httserver 需要在 App Module  build.gradle 文件中  android标签下加入
packagingOptions {
    pickFirst "META-INF/INDEX.LIST"
    pickFirst "META-INF/io.netty.versions.properties"
}

