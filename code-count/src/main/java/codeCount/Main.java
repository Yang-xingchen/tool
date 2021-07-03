package codeCount;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        File file = new File("");
        List<String> types = Stream.of(".java", ".py", ".c", ".cpp", ".cs", ".xml", ".vue", ".js", ".yaml", ".yml", ".md", ".properties", ".sql")
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        List<String> exclude = Stream.of(".idea", ".git", "target", ".metadata", ".recommenders", "node_modules", ".vscode")
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        FileTask task = new FileTask(file, types, exclude);
        Map<String, FileTask.Result> resultMap = ForkJoinPool.commonPool().submit(task).get();
        saveToFile(() -> toCsv(task, types, 3), "./result.csv");
    }

    public static void saveToFile(Supplier<String> supplier, String outName) {
        try (FileOutputStream outputStream = new FileOutputStream(outName);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream)) {
            outputStreamWriter.write(supplier.get());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toCsv(FileTask result, List<String> types, int layer) {
        StringBuilder res = new StringBuilder();
        res.append("path, ");
        types.forEach(t -> res.append(t).append(", "));
        res.append("\n").append(toCsvByLayer(result, types, layer));
        return res.toString();
    }

    public static StringBuilder toCsvByLayer(FileTask result, List<String> types, int layer) {
        StringBuilder res = new StringBuilder();
        res.append(result.getFile().getPath()).append(", ");
        types.forEach(t -> {
            FileTask.Result r = result.getResult().get(t);
            if (r == null) {
                return;
            }
            res.append(r.getSource()).append(", ");
        });
        res.append("\n");
        if (layer > 0 && result.getChild().get("") != null) {
            result.getChild().get("").stream().map(fileTask -> toCsvByLayer(fileTask, types, layer - 1)).forEach(res::append);
        }
        return res;
    }

}
