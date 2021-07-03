package diskAnalyzed;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandMain {

    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws Throwable {
        String path;
        if (args.length == 2) {
            path = args[1];
        } else {
            System.out.println("请输入路径:");
            path = scanner.next();
        }
        FileTask result = new FileTask(new File(path));
        ForkJoinPool.commonPool().submit(result).get();
        print(result);
    }

    static boolean f = true;
    private static void print(FileTask fileTask) {
        while (f) {
            System.out.println("path:" + fileTask.getFile().getAbsolutePath());
            System.out.println("size:" + fileTask.getSize() + " B");
            AtomicInteger index = new AtomicInteger(1);
            System.out.println("   0. 上层目录");
            fileTask.getChild().forEach(child -> {
                System.out.format("%4d. %s %-,20d %s\n",
                        index.get(),
                        child.getFile().isFile() ? "F" : "D",
                        child.getSize(),
                        child.getFile().getAbsoluteFile());
                index.incrementAndGet();
            });
            System.out.printf("%4d. %s\n", index.get(), "退出");
            System.out.println("请输入序号：");
            int i = Integer.parseInt(scanner.next()) - 1;
            if (i == -1) {
                return;
            } else if (index.get() == i + 1) {
                f = false;
            } else {
                print(fileTask.getChild().get(i));
            }
        }
    }

}
