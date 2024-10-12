# Используем официальный образ Java
FROM openjdk:11-jdk-slim

# Рабочая директория внутри контейнера
WORKDIR /app

# Копируем файлы проекта
COPY src/ /app/src/
COPY pom.xml /app/

# Устанавливаем Maven и собираем проект
RUN apt-get update && \
    apt-get install -y maven && \
    mvn package && \
    apt-get remove -y maven && \
    apt-get autoremove -y && \
    apt-get clean

# Указываем порт, который будет открыт в контейнере
EXPOSE 3000

# Запускаем приложение
CMD ["java", "-jar", "target/dota2-gsi-server-1.0.jar"]