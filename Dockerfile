# ビルド用ステージ: Maven で jar を作ります
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw package -DskipTests

# 実行用ステージ: サイトの「コード実行」機能で javac が必要なので JDK を使います
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/memoapp-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
