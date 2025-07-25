package com.example;

import com.example.model.Item;
import com.example.model.Result;
import com.example.model.Statistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JournalService {

    @Autowired
    private JournalDao journalDao;

    public static final ZoneOffset ZONE_OFFSET = ZoneOffset.ofHours(8);
    private static final Comparator<Statistics> STATISTICS_COMPARABLE = (a, b) -> {
        if (a.getNextTime() == null && b.getNextTime() == null) {
            return b.getLastTime().compareTo(a.getLastTime());
        }
        if (a.getNextTime() == null) {
            return 1;
        }
        if (b.getNextTime() == null) {
            return -1;
        }
        long now = System.currentTimeMillis() / 1000;
        int comp = (int) (Math.abs(a.getNextTime().toEpochSecond(ZONE_OFFSET) - now)
                - Math.abs(b.getNextTime().toEpochSecond(ZONE_OFFSET) - now));
        return comp != 0 ? comp : b.getLastTime().compareTo(a.getLastTime());
    };

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
                .sorted(STATISTICS_COMPARABLE)
                .toList();
    }

    private Statistics statistic(List<Item> list, String name, Function<Item, String> classifier) {
        Statistics statistics = new Statistics();
        statistics.setName(name);
        statistics.setCount(list.size());
        List<Item> sorted = list.stream().sorted(Comparator.comparing(Item::getTime, Comparator.reverseOrder())).toList();
        LocalDateTime lastTime = sorted.getFirst().getTime();
        statistics.setLastTime(sorted.getFirst().getTime());
        long rate = sorted.size() > 1
            ? TimeUnit.SECONDS.toHours(Duration.between(sorted.getLast().getTime(), lastTime).getSeconds()) / (sorted.size() - 1)
            : 0;
        statistics.setRate(rate);
        if (sorted.size() >= 15) {
            statistics.setRate15(TimeUnit.SECONDS.toHours(Duration.between(sorted.get(14).getTime(), lastTime).getSeconds()) / 14);
        } else {
            statistics.setRate15(rate);
        }
        if (sorted.size() >= 5) {
            statistics.setRate5(TimeUnit.SECONDS.toHours(Duration.between(sorted.get(4).getTime(), lastTime).getSeconds()) / 4);
        } else {
            statistics.setRate5(rate);
        }
        statistics.setNextTime(nextTime(list.stream().map(Item::getTime).toList()));
        if (statistics.getNextTime() != null) {
            Duration duration = Duration.between(LocalDateTime.now(), statistics.getNextTime());
            String string = (duration.isNegative() ? "-" : "")
                    + Math.abs(duration.toDays()) + "d"
                    + Math.abs(duration.toHoursPart()) + "H";
            statistics.setNextDuration(string);
        }
        if (classifier != null) {
            List<Statistics> children = list.stream()
                    .collect(Collectors.groupingBy(classifier))
                    .values()
                    .stream()
                    .map(subList -> statistic(subList, classifier.apply(subList.getFirst()), null))
                    .sorted(STATISTICS_COMPARABLE)
                    .peek(child -> child.setParent(name))
                    .toList();
            statistics.setChildren(children);
        } else {
            statistics.setChildren(List.of());
        }
        return statistics;
    }

    public LocalDateTime nextTime(List<LocalDateTime> times) {
        if (times.size() <= 1) {
            return null;
        }
        int count = 5;
        times = times.stream().sorted(Comparator.comparing(Function.identity())).toList();
        LocalDateTime last5 = times.get(Math.max(times.size() - count, 0));
        int avgDay = Period.between(last5.toLocalDate(), times.getLast().toLocalDate()).getDays() / Math.min(times.size() - 1, count - 1);
        int avgHour = (int) times.stream().mapToInt(LocalDateTime::getHour).average().orElse(0);
        return LocalDateTime.of(times.getLast().toLocalDate().plusDays(avgDay), LocalTime.of(avgHour, 0));
    }

    public List<Item> get(String key, String subKey) {
        try {
            return journalDao.getAll()
                    .stream()
                    .filter(item -> key.equals(item.getKey()))
                    .filter(item -> subKey == null || subKey.equals(item.getSubKey()))
                    .sorted(Comparator.comparing(Item::getTime, Comparator.reverseOrder()))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
