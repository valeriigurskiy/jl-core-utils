package com.vh;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

public class JLFind extends RecursiveTask<ConcurrentLinkedQueue<Path>> {

    private final Path dir;
    private final Pattern fileNamePattern;
    private final boolean contains;

    public JLFind(Path dir, Pattern fileNamePattern, boolean contains) {
        this.dir = dir;
        this.fileNamePattern = fileNamePattern;
        this.contains = contains;
    }

    @Override
    protected ConcurrentLinkedQueue<Path> compute() {
        List<JLFind> subtasks = new ArrayList<>();
        ConcurrentLinkedQueue<Path> paths = new ConcurrentLinkedQueue<>();

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    JLFind subtask = new JLFind(path, fileNamePattern, contains);
                    subtask.fork();
                    subtasks.add(subtask);
                }

                String fileName = path.getFileName().toString();
                
                if (matches(fileName)) {
                    paths.add(path);
                }
            }
        } catch (IOException | SecurityException ignored) { }

        for (JLFind subtask : subtasks) {
            paths.addAll(subtask.join());
        }

        return paths;
    }

    private boolean matches(String fileName) {
        return contains
            ? fileNamePattern.matcher(fileName).find()
            : fileNamePattern.matcher(fileName).matches();
    }
}
