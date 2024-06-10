package ru.vyatsu.route_optimizer;

import ru.vyatsu.route_optimizer.bean.graph.Edge;
import ru.vyatsu.route_optimizer.bean.graph.Graph;
import ru.vyatsu.route_optimizer.bean.graph.Schedule;
import ru.vyatsu.route_optimizer.bean.graph.Vertex;

import java.util.*;

public class DijkstraAlgorithm {

    private final Graph graph;
    private final Map<String, Integer> distances;
    private final Map<String, String> previous;
    private final PriorityQueue<Vertex> queue;
    private final Set<String> processed;
    private final Map<String, String> routeNumbers;

    public DijkstraAlgorithm(Graph graph) {
        this.graph = graph;
        this.distances = new HashMap<>();
        this.previous = new HashMap<>();
        this.queue = new PriorityQueue<>(Comparator.comparingInt(
                vertex -> distances.getOrDefault(vertex.getId(), Integer.MAX_VALUE)));
        this.processed = new HashSet<>();
        this.routeNumbers = new HashMap<>();
    }

    public void execute(String startVertexId, String startTime) {
        // Конвертация времени в минуты
        int startMinutes = convertTimeToMinutes(startTime);

        distances.put(startVertexId, startMinutes);
        queue.add(graph.getVertex(startVertexId));

        while (!queue.isEmpty()) {
            Vertex currentVertex = queue.poll();
            if (processed.contains(currentVertex.getId())) {
                continue;
            }
            processed.add(currentVertex.getId());

            int currentTime = distances.getOrDefault(currentVertex.getId(), Integer.MAX_VALUE);

            if (graph.getEdges().containsKey(currentVertex.getId())) {
                for (Edge edge : graph.getEdges().get(currentVertex.getId())) {
                    Vertex nextVertex = graph.getVertex(edge.getEndVertex());
                    if (nextVertex == null) {
                        continue;
                    }

                    // Расчет времени ожидания маршрута (в случае пересадки) и времени в пути
                    int waitTime = calculateWaitTime(currentVertex, edge, currentTime);
                    int travelTime = calculateTravelTime(currentVertex, nextVertex, edge, currentTime);
                    int newDist = currentTime + waitTime + travelTime;

                    // Новый путь оптимальнее
                    if (travelTime > 0 && newDist < distances.getOrDefault(nextVertex.getId(), Integer.MAX_VALUE)) {
                        distances.put(nextVertex.getId(), newDist);
                        previous.put(nextVertex.getId(), currentVertex.getId());
                        routeNumbers.put(nextVertex.getId(), edge.getRouteNumber());
                        queue.add(nextVertex);
                    }
                }
            }
        }
    }

    private int calculateWaitTime(Vertex startVertex, Edge edge, int currentTime) {
        List<Schedule> schedules = startVertex.getSchedules();
        int minWaitTime = Integer.MAX_VALUE;

        for (Schedule schedule : schedules) {
            if (schedule.getRouteNumber().equals(edge.getRouteNumber())) {
                for (String time : schedule.getTimes()) {
                    // Если маршрут содержит текст (нестандартный маршрут), то извлекаем время
                    String cleanTime = time.replaceAll("[^0-9:]", "");

                    int timeMinutes = convertTimeToMinutes(cleanTime);

                    // Найдено ближайшее время
                    if (timeMinutes >= currentTime) {
                        int waitTime = timeMinutes - currentTime;
                        if (waitTime < minWaitTime) {
                            minWaitTime = waitTime;
                        }
                        break;
                    }
                }
            }
        }

        return minWaitTime == Integer.MAX_VALUE ? 0 : minWaitTime;
    }

    private int calculateTravelTime(Vertex startVertex, Vertex endVertex, Edge edge, int currentTime) {
        List<Schedule> startSchedules = startVertex.getSchedules();
        List<Schedule> endSchedules = endVertex.getSchedules();

        // Расчет времени в пути на основании расписания отправной остановки, конечной остановки и текущего времени
        for (Schedule startSchedule : startSchedules) {
            if (startSchedule.getRouteNumber().equals(edge.getRouteNumber())) {
                for (String startTime : startSchedule.getTimes()) {
                    String cleanStartTime = startTime.replaceAll("[^0-9:]", "");
                    int startMinutes = convertTimeToMinutes(cleanStartTime);

                    // Поиск ближайшего к имеющемуся времени в расписании
                    if (startMinutes >= currentTime) {
                        for (Schedule endSchedule : endSchedules) {
                            if (endSchedule.getRouteNumber().equals(edge.getRouteNumber())) {
                                for (String endTime : endSchedule.getTimes()) {
                                    String cleanEndTime = endTime.replaceAll("[^0-9:]", "");
                                    int endMinutes = convertTimeToMinutes(cleanEndTime);
                                    if (endMinutes > startMinutes) {
                                        return endMinutes - startMinutes;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return Integer.MAX_VALUE; // Если нет подходящего времени, возвращаем максимум
    }

    private int convertTimeToMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    public List<String> getPath(String endVertexId) {
        List<String> path = new ArrayList<>();
        for (String vertexId = endVertexId; vertexId != null; vertexId = previous.get(vertexId)) {
            path.add(vertexId);
        }
        Collections.reverse(path);
        return path;
    }

    public int getTime(String vertexId) {
        return distances.getOrDefault(vertexId, Integer.MAX_VALUE);
    }

    public String getRouteNumber(String vertexId) {
        return routeNumbers.getOrDefault(vertexId, "Unknown");
    }
}
