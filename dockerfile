# Sử dụng image OpenJDK làm base
FROM openjdk:20-jdk

# Thêm metadata
LABEL maintainer="your-email@example.com"

# Tạo thư mục làm việc
WORKDIR /app

# Copy file JAR vào container
COPY build/libs/gateway-0.0.1-SNAPSHOT.jar app.jar

# Chạy ứng dụng
ENTRYPOINT ["java","-jar","app.jar"]
