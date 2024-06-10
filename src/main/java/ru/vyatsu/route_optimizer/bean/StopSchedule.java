package ru.vyatsu.route_optimizer.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StopSchedule {
    private String code;
    private String name;
    private List<String> schedule; // Полное расписание для остановки
}

