package com.vh;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

public class JLFind extends RecursiveTask<List<Path>> {

    private final Path dir;
    private final Pattern fileNamePattern;
    private final boolean contains;

    public JLFind(Path dir, Pattern fileNamePattern, boolean contains) {
        this.dir = dir;
        this.fileNamePattern = fileNamePattern;
        this.contains = contains;
    }

    @Override
    protected List<Path> compute() {
        List<Path> foundPaths = new ArrayList<>();
        List<JLFind> subtasks = new ArrayList<>();

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
            directoryStream.forEach(path -> {
                if (Files.isDirectory(path) && !Files.isSymbolicLink(path)) {
                    JLFind subtask = new JLFind(path, fileNamePattern, contains);
                    subtask.fork();
                    subtasks.add(subtask);
                } else if (Files.isRegularFile(path)) {
                    String fileName = path.getFileName().toString();
                    boolean matches = contains
                            ? fileNamePattern.matcher(fileName).find()
                            : fileNamePattern.matcher(fileName).matches();
                    if (matches) {
                        foundPaths.add(path);
                    }
                }
            });
        } catch (IOException | SecurityException ignored) { }

        for (JLFind subtask : subtasks) {
            foundPaths.addAll(subtask.join());
        }

        return foundPaths;
    }

    public List<Path> search() {
        try (ForkJoinPool pool = new ForkJoinPool()) {
            JLFind task = new JLFind(dir, fileNamePattern, contains);
            return pool.invoke(task);
        }
    }
}
