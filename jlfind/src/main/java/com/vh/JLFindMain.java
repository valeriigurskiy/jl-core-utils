package com.vh;

import static com.vh.CliUtils.errorExit;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

public class JLFindMain {
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

        startDir = Objects.equals(startDir, ".") ? System.getenv("PWD") : startDir;

        if (startDir == null) {
            errorExit("Missing path argument.");
        }

        StringBuilder regexBuilder = new StringBuilder();

        boolean contains = false;

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
                    contains = true;
                    i++;
                }
                default -> errorExit("Unknown option: " + arg);
            }
        }

        if (regexBuilder.isEmpty()) {
            errorExit("Missing file names after -n or --name");
        }

        Path root = Paths.get(startDir);

        Pattern pattern = Pattern.compile(regexBuilder.toString());

        try (ForkJoinPool pool = new ForkJoinPool()) {
            JLFind jlFind = new JLFind(root, pattern, contains);
            
            ConcurrentLinkedQueue<Path> paths = jlFind.compute(); 
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
        """
        );
        System.exit(1);
    }
}
