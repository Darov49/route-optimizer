package ru.vyatsu.route_optimizer.bean;

import java.util.List;

public class PathResult {
    private List<StopInfo> path;
    private String totalTime;

    public PathResult(List<StopInfo> path, String totalTime) {
        this.path = path;
        this.totalTime = totalTime;
    }

    public List<StopInfo> getPath() {
        return path;
    }

    public String getTotalTime() {
        return totalTime;
    }

    public static class StopInfo {
        private String stopId;
        private String stopName;
        private String arrivalTime;
        private String routeNumber;
        private String transferInfo;

        public StopInfo(String stopId, String stopName, String arrivalTime, String routeNumber, String transferInfo) {
            this.stopId = stopId;
            this.stopName = stopName;
            this.arrivalTime = arrivalTime;
            this.routeNumber = routeNumber;
            this.transferInfo = transferInfo;
        }

        public String getStopId() {
            return stopId;
        }

        public String getStopName() {
            return stopName;
        }

        public String getArrivalTime() {
            return arrivalTime;
        }

        public String getRouteNumber() {
            return routeNumber;
        }

        public String getTransferInfo() {
            return transferInfo;
        }
    }
}

