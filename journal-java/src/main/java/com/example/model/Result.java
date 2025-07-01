package com.example.model;

import lombok.Data;

import java.util.List;

@Data
public class Result {

    private boolean success;
    private String msg;
    private List<Item> timeline;
    private List<Statistics> statistics;

}
