package ru.vyatsu.route_optimizer.bean.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Edge {
    private String startVertex;
    private String endVertex;
    private String routeNumber;
    private int travelTime = 0;
    private int waitTime = 0;
    private String startTime;
    private String endTime;
}

