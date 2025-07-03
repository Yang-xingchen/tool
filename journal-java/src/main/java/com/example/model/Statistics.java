package com.example.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Statistics {

    /**
     * 名称
     */
    private String name;
    /**
     * 次数
     */
    private Integer count;
    /**
     * 上次时间
     */
    private LocalDateTime lastTime;
    /**
     * 频率
     */
    private Long rate;
    /**
     * 频率
     */
    private Long rate5;
    /**
     * 频率
     */
    private Long rate15;
    /**
     * 预计下次时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime nextTime;

    private List<Statistics> children;

}
