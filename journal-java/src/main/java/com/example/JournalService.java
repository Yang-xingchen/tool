package com.example;

import com.example.model.Item;
import com.example.model.Result;
import com.example.model.Statistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JournalService {

    @Autowired
    private JournalDao journalDao;

    public Result submit(Item item) {
        try {
            journalDao.save(item);
            return get();
        } catch (Exception e) {
            log.error("submit", e);
            Result result = new Result();
            result.setSuccess(false);
            result.setMsg(e.toString());
            return result;
        }
    }

    public Result get() {
        Result result = new Result();
        try {
            List<Item> items = journalDao.getAll()
                    .stream()
                    .sorted(Comparator.comparing(Item::getTime, Comparator.reverseOrder()))
                    .collect(Collectors.toList());
            result.setTimeline(items.stream().limit(50).toList());
            result.setStatistics(toStatistics(items));
            result.setSuccess(true);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMsg(e.toString());
            log.error("get", e);
        }
        return result;
    }

    private List<Statistics> toStatistics(List<Item> items) {
        return items.stream()
                .collect(Collectors.groupingBy(Item::getKey))
                .values()
                .stream()
                .map(list -> statistic(list, list.getFirst().getKey(), Item::getSubKey))
                .sorted(Comparator.comparing(Statistics::getNextTime, Comparator.reverseOrder()))
                .toList();
    }

    private Statistics statistic(List<Item> list, String name, Function<Item, String> classifier) {
        Statistics statistics = new Statistics();
        statistics.setName(name);
        statistics.setCount(list.size());
        List<Item> sorted = list.stream().sorted(Comparator.comparing(Item::getTime, Comparator.reverseOrder())).toList();
        LocalDateTime lastTime = sorted.getFirst().getTime();
        statistics.setLastTime(lastTime);
        long rate = TimeUnit.SECONDS.toHours(Duration.between(sorted.getLast().getTime(), lastTime).getSeconds()) / sorted.size();
        statistics.setRate(rate);
        if (sorted.size() >= 15) {
            statistics.setRate15(TimeUnit.SECONDS.toHours(Duration.between(sorted.get(15).getTime(), lastTime).getSeconds()) / 15);
        } else {
            statistics.setRate15(rate);
        }
        if (sorted.size() >= 5) {
            statistics.setRate5(TimeUnit.SECONDS.toHours(Duration.between(sorted.get(5).getTime(), lastTime).getSeconds()) / 5);
        } else {
            statistics.setRate5(rate);
        }
        statistics.setNextTime(lastTime.plusHours(statistics.getRate5() / 6 * 6 + 3));
        if (classifier != null) {
            List<Statistics> children = list.stream()
                    .collect(Collectors.groupingBy(classifier))
                    .values()
                    .stream()
                    .map(subList -> statistic(subList, classifier.apply(subList.getFirst()), null))
                    .sorted(Comparator.comparing(Statistics::getNextTime, Comparator.reverseOrder()))
                    .toList();
            statistics.setChildren(children);
        } else {
            statistics.setChildren(List.of());
        }
        return statistics;
    }

    public List<Item> get(String key) {
        try {
            return journalDao.getAll()
                    .stream()
                    .filter(item -> key.equals(item.getKey()))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
