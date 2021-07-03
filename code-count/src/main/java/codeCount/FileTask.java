package codeCount;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

public class FileTask extends RecursiveTask<Map<String, FileTask.Result>> {

    private File file;
    private Map<String, List<FileTask>> child;
    private Map<String, Result> result;
    private String type;

    private List<String> countType;
    private List<String> exclude;

    public FileTask(File file, List<String> countType, List<String> exclude) {
        this.file = file;
        if (file.isFile()) {
            String fn = file.getName();
            int index = fn.lastIndexOf(".");
            type = fn.substring(index == -1 ? fn.length() - 1 : index).toLowerCase();
        } else {
            type = "";
        }
        this.countType = countType;
        this.exclude = exclude;
    }

    @Override
    protected Map<String, Result> compute() {
        if (file.isFile()) {
            child = Map.of();
            if (countType.stream().anyMatch(name -> Objects.equals(type, name))) {
                try (FileInputStream inputStream = new FileInputStream(file);
                     InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                     BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                    Map<Boolean, List<String>> collect = bufferedReader
                            .lines()
                            .map(String::strip)
                            .collect(Collectors.partitioningBy(s -> Objects.equals("", s)));
                    result = Map.of(type, new Result(collect.get(true).size(), collect.get(false).size()));
                    return result;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            result = Map.of();
            return result;
        }
        child = Arrays.stream(Optional.ofNullable(file.listFiles()).orElse(new File[]{}))
                .filter(f -> !exclude.contains(f.getName().toLowerCase()))
                .map(file1 -> new FileTask(file1, countType, exclude))
                .collect(Collectors.groupingBy(FileTask::getType));
        result = child.values()
                .stream()
                .flatMap(List::stream)
                .map(ForkJoinTask::fork)
                .collect(Collectors.toList())
                .stream()
                .map(resultForkJoinTask -> {
                    try {
                        return resultForkJoinTask.get();
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .reduce(new HashMap<>(countType.size()), (result, r) -> {
                    countType.forEach(t -> {
                        Result r1 = result.computeIfAbsent(t, s -> new Result(0, 0));
                        Result r2 = r.get(t);
                        if (r2 != null) {
                            r1.source += r2.source;
                            r1.blank += r2.blank;
                            r1.total += r2.total;
                        }
                    });
                    return result;
                });
        return result;
    }

    public File getFile() {
        return file;
    }

    public FileTask setFile(File file) {
        this.file = file;
        return this;
    }

    public Map<String, List<FileTask>> getChild() {
        return child;
    }

    public FileTask setChild(Map<String, List<FileTask>> child) {
        this.child = child;
        return this;
    }

    public String getType() {
        return type;
    }

    public FileTask setType(String type) {
        this.type = type;
        return this;
    }

    public Map<String, Result> getResult() {
        return result;
    }

    public FileTask setResult(Map<String, Result> result) {
        this.result = result;
        return this;
    }

    public List<String> getCountType() {
        return countType;
    }

    public FileTask setCountType(List<String> countType) {
        this.countType = countType;
        return this;
    }

    static class Result {
        private long total;
        private long blank;
        private long source;

        public Result(long blank, long source) {
            this.blank = blank;
            this.source = source;
            this.total = blank + source;
        }

        public long getTotal() {
            return total;
        }

        public long getBlank() {
            return blank;
        }

        public long getSource() {
            return source;
        }
    }
}
