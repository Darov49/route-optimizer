package ru.vyatsu.route_optimizer.bean.graph;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Graph {
    private Map<String, Vertex> vertices = new HashMap<>();
    private Map<String, List<Edge>> edges = new HashMap<>();

    public void addVertex(Vertex vertex) {
        vertices.put(vertex.getId(), vertex);
    }

    public void addEdge(Edge edge) {
        edges.computeIfAbsent(edge.getStartVertex(), k -> new ArrayList<>()).add(edge);
    }

    public Vertex getVertex(String id) {
        return vertices.get(id);
    }

    public List<Edge> getEdges(String vertexId) {
        return edges.get(vertexId);
    }
}

