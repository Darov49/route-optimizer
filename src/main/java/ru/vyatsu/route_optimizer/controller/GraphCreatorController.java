package ru.vyatsu.route_optimizer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

@RestController
public class GraphCreatorController {

    private static final String DATA_DIRECTORY = "routes";
    private static final String OUTPUT_FILE = "graph.json";

    @GetMapping("/api/loadGraph")
    public String loadGraph() throws IOException {
        Graph graph = new Graph();
        ObjectMapper objectMapper = new ObjectMapper();

        File folder = new File(DATA_DIRECTORY);
        File[] listOfFiles = folder.listFiles((dir, name) -> name.endsWith(".json"));

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                List<StopSchedule> stopSchedules = objectMapper.readValue(file, objectMapper.getTypeFactory().constructCollectionType(List.class, StopSchedule.class));
                for (StopSchedule stopSchedule : stopSchedules) {
                    Vertex vertex = graph.getVertex(stopSchedule.getCode());
                    if (vertex == null) {
                        vertex = new Vertex(stopSchedule.getCode(), stopSchedule.getName(), new ArrayList<>());
                        graph.addVertex(vertex);
                    }

                    Schedule schedule = new Schedule(file.getName().replace(".json", ""), stopSchedule.getSchedule());
                    vertex.getSchedules().add(schedule);
                }
            }
        }

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                List<StopSchedule> stopSchedules = objectMapper.readValue(file, objectMapper.getTypeFactory().constructCollectionType(List.class, StopSchedule.class));
                for (int i = 0; i < stopSchedules.size() - 1; i++) {
                    StopSchedule currentStop = stopSchedules.get(i);
                    StopSchedule nextStop = stopSchedules.get(i + 1);

                    List<String> currentSchedule = currentStop.getSchedule();
                    List<String> nextSchedule = nextStop.getSchedule();

                    int travelTime = 0;
                    boolean travelTimeCalculated = false;

                    for (int j = 0; j < currentSchedule.size() && j < nextSchedule.size(); j++) {
                        try {
                            travelTime = calculateTravelTime(currentSchedule.get(j), nextSchedule.get(j));
                            travelTimeCalculated = true;
                            break;
                        } catch (NumberFormatException e) {
                            // Игнорируем ошибку и пробуем следующую пару
                        }
                    }

                    if (travelTimeCalculated) {
                        Edge edge = new Edge(currentStop.getCode(), nextStop.getCode(), file.getName().replace(".json", ""), travelTime, 0, "", "");
                        graph.addEdge(edge);
                    }
                }
            }
        }

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(OUTPUT_FILE), graph);

        return "Graph has been successfully saved to " + OUTPUT_FILE;
    }

    private int calculateTravelTime(String startTime, String endTime) {
        String[] startParts = startTime.split(":");
        String[] endParts = endTime.split(":");


        int startMinutes = Integer.parseInt(startParts[0]) * 60 + Integer.parseInt(startParts[1]);
        int endMinutes = Integer.parseInt(endParts[0]) * 60 + Integer.parseInt(endParts[1]);

        return endMinutes - startMinutes;
    }
}
