package ru.vyatsu.route_optimizer.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Stop {
    private String code;
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Stop vertex = (Stop) o;

        return code.equals(vertex.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
