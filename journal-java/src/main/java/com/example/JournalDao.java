package com.example;

import com.example.model.Item;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class JournalDao {

    @Autowired
    private ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public void save(Item item) throws Exception {
        item.setId(System.currentTimeMillis());
        Path path = Paths.get("data", item.getTime().format(DATE_TIME_FORMATTER) + ".json");
        if (!Files.exists(path)) {
            Files.writeString(path, "[]", StandardOpenOption.CREATE);
        }
        Files.writeString(Paths.get("backup.json"), Files.readString(path));
        List<Item> items = new ArrayList<>();
        items.add(item);
        items.addAll(readFile(path));
        items.sort(Comparator.comparing(Item::getTime, Comparator.reverseOrder()));
        objectMapper.writeValue(path.toFile(), items);
    }

    public List<Item> getAll() throws Exception {
        Path dataPath = Paths.get("data");
        if (!Files.exists(dataPath)) {
            Files.createDirectories(dataPath);
        }
        try (Stream<Path> list = Files.list(dataPath)) {
            return list
                    .map(this::readFile)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }
    }

    private List<Item> readFile(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), new TypeReference<>() {});
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
