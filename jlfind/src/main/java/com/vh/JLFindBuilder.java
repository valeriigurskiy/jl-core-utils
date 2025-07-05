package com.vh;

import java.nio.file.Path;
import java.util.regex.Pattern;

public class JLFindBuilder {
    private Path dir;
    private Pattern fileNamePattern;
    private boolean contains = false;
    private boolean followSymbolLinks = true;

    public JLFindBuilder dir(Path dir) {
        this.dir = dir;
        return this;
    }

    public JLFindBuilder fileNamePattern(Pattern fileNamePattern) {
        this.fileNamePattern = fileNamePattern;
        return this;
    }

    public JLFindBuilder contains(boolean contains) {
        this.contains = contains;
        return this;
    }

    public JLFindBuilder followSymbolLinks(boolean followSymbolLinks) {
        this.followSymbolLinks = followSymbolLinks;
        return this;
    }

    public JLFind build() {
        return new JLFind(dir, fileNamePattern, contains, followSymbolLinks);
    }
}