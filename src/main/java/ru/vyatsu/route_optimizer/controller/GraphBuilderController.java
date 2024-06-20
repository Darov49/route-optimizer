package ru.vyatsu.route_optimizer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.vyatsu.route_optimizer.bean.StopSchedule;
import ru.vyatsu.route_optimizer.bean.graph.Edge;
import ru.vyatsu.route_optimizer.bean.graph.Graph;
import ru.vyatsu.route_optimizer.bean.graph.Schedule;
import ru.vyatsu.route_optimizer.bean.graph.Vertex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ru.vyatsu.route_optimizer.constant.StringConstants.JSON_FILE_EXTENSION;

@RestController
public class GraphBuilderController {

    @Value("${routes.directory}")
    private String dataDirectory;

    @Value("${graph.file}")
    private String outputFile;


    @GetMapping("/api/loadGraph")
    public String loadGraph() throws IOException {
        Graph graph = new Graph();
        ObjectMapper objectMapper = new ObjectMapper();

        File folder = new File(dataDirectory);
        File[] listOfFiles = folder.listFiles((dir, name) -> name.endsWith(JSON_FILE_EXTENSION));

        if (listOfFiles == null || listOfFiles.length == 0) {
            return "No schedule files were found. Please run scraper-controller to generate them.";
        }


        for (File file : listOfFiles) {
            List<StopSchedule> stopSchedules = objectMapper.readValue(file,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, StopSchedule.class));

            // Объединение всех имеющихся расписаний в одно расписание для всех остановок
            for (StopSchedule stopSchedule : stopSchedules) {
                Vertex vertex = graph.getVertex(stopSchedule.getCode());
                if (vertex == null) {
                    vertex = new Vertex(stopSchedule.getCode(), stopSchedule.getName(), new ArrayList<>());
                    graph.addVertex(vertex);
                }

                Schedule schedule = new Schedule(file.getName().replace(JSON_FILE_EXTENSION, ""),
                        stopSchedule.getSchedule());
                vertex.getSchedules().add(schedule);
            }

            // Создание ребер между остановками
            for (int i = 0; i < stopSchedules.size() - 1; i++) {
                StopSchedule currentStop = stopSchedules.get(i);
                StopSchedule nextStop = stopSchedules.get(i + 1);

                List<String> currentSchedule = currentStop.getSchedule();
                List<String> nextSchedule = nextStop.getSchedule();

                int travelTime = 0;
                boolean isTravelTimeValid = false;

                for (int j = 0; j < currentSchedule.size() && j < nextSchedule.size(); j++) {
                    try {
                        isTravelTimeValid = convertTimeToMinutes(currentSchedule.get(j))
                                < convertTimeToMinutes(nextSchedule.get(j));
                        break;
                    } catch (NumberFormatException e) {
                        // Игнорируем ошибку и пробуем следующую пару
                    }
                }

                if (isTravelTimeValid) {
                    Edge edge = new Edge(currentStop.getCode(), nextStop.getCode(),
                            file.getName().replace(JSON_FILE_EXTENSION, ""),
                            travelTime, 0, "", "");
                    graph.addEdge(edge);
                }
            }
        }

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputFile), graph);

        return "Graph has been successfully saved to " + outputFile;
    }

    private int convertTimeToMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }
}
