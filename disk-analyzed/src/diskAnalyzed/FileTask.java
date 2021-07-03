package diskAnalyzed;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

public class FileTask extends RecursiveTask<Long> {

    private File file;
    private List<FileTask> child;
    private long size;

    public FileTask(File name) {
        this.file = name;
    }

    public File getFile() {
        return file;
    }

    public FileTask setFile(File file) {
        this.file = file;
        return this;
    }

    public List<FileTask> getChild() {
        return child;
    }

    public FileTask setChild(List<FileTask> child) {
        this.child = child;
        return this;
    }

    public long getSize() {
        return size;
    }

    public FileTask setSize(long size) {
        this.size = size;
        return this;
    }

    @Override
    protected Long compute() {
        if (file.isFile()) {
            size = file.length();
            child = List.of();
            return size;
        }
        child = Arrays.stream(Optional.ofNullable(file.listFiles()).orElse(new File[]{}))
                .map(FileTask::new)
                .collect(Collectors.toList());
        size = child.stream()
                .map(ForkJoinTask::fork)
                .collect(Collectors.toList())
                .stream()
                .mapToLong(longForkJoinTask -> {
                    try {
                        return longForkJoinTask.get();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                })
                .sum();
        child.sort(Comparator.comparingLong(f -> -f.size));
        return size;
    }
}
