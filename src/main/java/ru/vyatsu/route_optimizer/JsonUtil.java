package ru.vyatsu.route_optimizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import ru.vyatsu.route_optimizer.bean.StopSchedule;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class JsonUtil {
    public static void writeStopsToJson(List<StopSchedule> stops, String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), stops);
    }
}
