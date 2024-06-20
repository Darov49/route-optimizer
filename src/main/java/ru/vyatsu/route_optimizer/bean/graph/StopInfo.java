package ru.vyatsu.route_optimizer.bean.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StopInfo {
    private String stopId;
    private String stopName;
    private String arrivalTime;
    private String routeNumber;
    private String transferInfo;
}
