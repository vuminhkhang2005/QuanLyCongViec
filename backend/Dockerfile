# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Sao chép pom.xml để tải dependencies trước (giúp cache layer của Docker tốt hơn)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Sao chép mã nguồn và build ứng dụng
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Sao chép file jar đã build từ Stage 1
COPY --from=build /app/target/todolist-0.0.1-SNAPSHOT.jar app.jar

# Khai báo cổng ứng dụng chạy
EXPOSE 8080

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
