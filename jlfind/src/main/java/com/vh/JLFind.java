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
    private final boolean followSymbolLinks;

    public JLFind(Path dir, Pattern fileNamePattern, boolean contains, boolean followSymbolLinks) {
        this.dir = dir;
        this.fileNamePattern = fileNamePattern;
        this.contains = contains;
        this.followSymbolLinks = followSymbolLinks;
    }

    @Override
    protected ConcurrentLinkedQueue<Path> compute() {
        List<JLFind> subtasks = new ArrayList<>();
        ConcurrentLinkedQueue<Path> paths = new ConcurrentLinkedQueue<>();

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
            for (Path path : directoryStream) {
                if (isDirectory(path)) {
                    JLFind subtask = new JLFindBuilder()
                            .dir(path)
                            .fileNamePattern(fileNamePattern)
                            .contains(contains)
                            .followSymbolLinks(followSymbolLinks)
                            .build();

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

    private boolean isDirectory(Path path) {
        return followSymbolLinks
                ? Files.isDirectory(path)
                : Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
    }

    private boolean matches(String fileName) {
        return contains
            ? fileNamePattern.matcher(fileName).find()
            : fileNamePattern.matcher(fileName).matches();
    }
}
