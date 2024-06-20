package ru.vyatsu.route_optimizer.bean.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PathResult {
    private List<StopInfo> path;
    private String totalTime;
}
