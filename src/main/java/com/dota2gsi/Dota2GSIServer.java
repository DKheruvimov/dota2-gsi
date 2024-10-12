package com.dota2gsi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Dota2GSIServer {
    private static final int PORT = 3000;
    private static FileWriter logWriter;
    private static final String COOLDOWN_DIR = "cooldowns";
    private static final String LOG_DIR = "logs";

    public static void main(String[] args) {
        try {
            // Создаем директории, если они не существуют
            Files.createDirectories(Paths.get(COOLDOWN_DIR));
            Files.createDirectories(Paths.get(LOG_DIR));

            // Настраиваем логирование
            String logFile = LOG_DIR + "/gsi_log_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".txt";
            logWriter = new FileWriter(logFile, true);
            logMessage("Сервер запускается...");

            // Создаем HTTP сервер
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", exchange -> {
                if ("POST".equals(exchange.getRequestMethod())) {
                    // Читаем данные из POST запроса
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody());
                    BufferedReader br = new BufferedReader(isr);
                    StringBuilder requestData = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        requestData.append(line);
                    }

                    // Обрабатываем полученные данные
                    processGSIData(requestData.toString());

                    // Отправляем ответ
                    String response = "GSI data received";
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } else {
                    // Если это не POST запрос, отправляем ошибку
                    String response = "Only POST requests are accepted";
                    exchange.sendResponseHeaders(405, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            });

            // Запускаем сервер
            server.start();
            logMessage("Сервер запущен на порту " + PORT);

            // Добавляем хук для корректного завершения работы
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    logMessage("Сервер останавливается...");
                    server.stop(0);
                    logWriter.close();
                } catch (IOException e) {
                    System.err.println("Ошибка при остановке сервера: " + e.getMessage());
                }
            }));

        } catch (IOException e) {
            System.err.println("Ошибка при запуске сервера: " + e.getMessage());
        }
    }

    private static void processGSIData(String gsiJson) {
        try {
            // Парсим JSON данные
            JsonObject jsonObject = JsonParser.parseString(gsiJson).getAsJsonObject();

            // Путь к файлу с кулдаунами
            String cooldownFile = COOLDOWN_DIR + "/ability_cooldowns.txt";
            FileWriter cooldownWriter = new FileWriter(cooldownFile);

            // Обрабатываем способности
            if (jsonObject.has("abilities")) {
                JsonObject abilities = jsonObject.getAsJsonObject("abilities");
                for (int i = 0; i < 6; i++) {
                    if (abilities.has("ability" + i)) {
                        JsonObject ability = abilities.getAsJsonObject("ability" + i);
                        if (ability.has("cooldown")) {
                            String cooldown = ability.get("cooldown").getAsString();
                            cooldownWriter.write("ability" + i + "=" + cooldown + "\n");
                        }
                    }
                }
            }

            // Обрабатываем предметы
            if (jsonObject.has("items")) {
                JsonObject items = jsonObject.getAsJsonObject("items");
                for (int i = 0; i < 6; i++) {
                    if (items.has("slot" + i)) {
                        JsonObject item = items.getAsJsonObject("slot" + i);
                        if (item.has("cooldown")) {
                            String cooldown = item.get("cooldown").getAsString();
                            cooldownWriter.write("item" + i + "=" + cooldown + "\n");
                        }
                    }
                }
            }

            cooldownWriter.close();
            logMessage("Данные о кулдаунах обновлены");

        } catch (Exception e) {
            logMessage("Ошибка при обработке GSI данных: " + e.getMessage());
        }
    }

    private static void logMessage(String message) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            logWriter.write(timestamp + " - " + message + "\n");
            logWriter.flush();
            System.out.println(timestamp + " - " + message);
        } catch (IOException e) {
            System.err.println("Ошибка при записи в лог: " + e.getMessage());
        }
    }
}