package com.example;

import com.example.model.Item;
import com.example.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RestController
public class JournalController {

    @Autowired
    private JournalService journalService;

    @GetMapping("/get")
    public Result get() {
        return journalService.get();
    }

    @GetMapping("/getLine")
    public List<Item> getItem(@RequestParam("key") String key) {
        return journalService.get(key);
    }

    @PostMapping("/submit")
    public Result submit(@RequestBody Item item) {
        Objects.requireNonNull(item);
        Objects.requireNonNull(item.getKey());
        if (item.getTime() == null) {
            item.setTime(LocalDateTime.now().withMinute(0).withSecond(0).withNano(0));
        }
        return journalService.submit(item);
    }

}
