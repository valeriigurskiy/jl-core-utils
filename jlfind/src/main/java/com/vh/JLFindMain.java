package com.vh;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

import static com.vh.CliUtils.errorExit;

public class JLFindMain {

    private static final boolean DEFAULT_CONTAINS_VALUE = false;
    private static final Boolean DEFAULT_FOLLOW_SYMBOL_LINKS_VALUE = true;

    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")) {
            printUsageAndExit();
        }

        String startDir = null;

        int i = 0;
        if (!args[0].startsWith("-")) {
            startDir = args[0];
            i = 1;
        }

        if (startDir == null) {
            startDir = System.getenv("PWD");
        }

        if (startDir == null) {
            errorExit("Missing path argument.");
        }

        StringBuilder regexBuilder = new StringBuilder();
        JLFindBuilder jlFindBuilder = new JLFindBuilder()
                .contains(DEFAULT_CONTAINS_VALUE)
                .followSymbolLinks(DEFAULT_FOLLOW_SYMBOL_LINKS_VALUE);

        while (i < args.length) {
            String arg = args[i];

            switch (arg) {
                case "-n", "--name" -> {
                    i++;
                    boolean first = true;
                    while (i < args.length) {
                        String current = args[i];
                        if (current.startsWith("-")) {
                            break;
                        }
                        if (!first) {
                            regexBuilder.append("|");
                        }
                        regexBuilder.append(Pattern.quote(current));
                        first = false;
                        i++;
                    }
                }
                case "-c", "--contains" -> {
                    jlFindBuilder.contains(true);
                    i++;
                }
                case "-P" -> {
                    jlFindBuilder.followSymbolLinks(false);
                    i++;
                }
                case "-L" -> {
                    jlFindBuilder.followSymbolLinks(true);
                    i++;
                }
                default -> errorExit("Unknown option: " + arg);
            }
        }

        if (regexBuilder.isEmpty()) {
            errorExit("Missing file names after -n or --name");
        }

        jlFindBuilder.dir(Paths.get(startDir));
        jlFindBuilder.fileNamePattern(Pattern.compile(regexBuilder.toString()));

        try (ForkJoinPool pool = new ForkJoinPool()) {
            JLFind jlFind = jlFindBuilder.build();

            ConcurrentLinkedQueue<Path> paths = pool.invoke(jlFind);
            paths.forEach(System.out::println);
        }
    }

    private static void printUsageAndExit() {
        System.out.println("""
        Usage: jlfind <path> [OPTIONS]

        Arguments:
            <path>           Root directory to start the search from

        Options:
            -n, --name       One or more file names to search for
            -c, --contains   Filename should contain argument value
            -P               Never follow symbol links
            -L               Follow symbol links
        """
        );
        System.exit(1);
    }
}
