/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.access;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command-line entry point used by builds to lower dependency-access source before Javac.
 */
public final class DependencyAccessSourceLowererMain {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "(?m)^\\s*package\\s+([\\w.]+)\\s*;");

    private DependencyAccessSourceLowererMain() {
    }

    /**
     * Lowers Java source roots into one generated source directory.
     *
     * @param arguments {@code --output <directory> --source <directory> [--source <directory>...]}
     */
    public static void main(String[] arguments) {
        Arguments parsedArguments = Arguments.parse(arguments);
        try {
            recreateDirectory(parsedArguments.outputDirectory());
            Map<String, String> sources = readSources(parsedArguments.sourceDirectories());
            Map<String, String> loweredSources = new DependencyAccessSourceLowerer()
                    .lowerSources(sources);
            writeSources(parsedArguments.outputDirectory(), loweredSources);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to lower dependency-access source", exception);
        }
    }

    private static Map<String, String> readSources(List<Path> sourceDirectories) throws IOException {
        Map<String, String> sources = new LinkedHashMap<>();
        for (Path sourceDirectory : sourceDirectories) {
            if (!Files.isDirectory(sourceDirectory)) {
                continue;
            }
            try (var paths = Files.walk(sourceDirectory)) {
                for (Path sourceFile : paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .sorted(Comparator.comparing(Path::toString))
                        .toList()) {
                    String source = Files.readString(sourceFile, StandardCharsets.UTF_8);
                    String qualifiedName = qualifiedName(sourceDirectory, sourceFile, source);
                    String previous = sources.putIfAbsent(qualifiedName, source);
                    if (previous != null) {
                        throw new IllegalArgumentException(
                                "Duplicate Java source type supplied to dependency access lowering: "
                                        + qualifiedName);
                    }
                }
            }
        }
        return sources;
    }

    private static String qualifiedName(
            Path sourceDirectory,
            Path sourceFile,
            String source) {
        String fileName = sourceFile.getFileName().toString();
        String simpleName = fileName.substring(0, fileName.length() - ".java".length());
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(source);
        if (packageMatcher.find()) {
            return packageMatcher.group(1) + "." + simpleName;
        }

        Path relativePath = sourceDirectory.relativize(sourceFile);
        String relativeName = relativePath.toString()
                .replace(relativePath.getFileSystem().getSeparator(), ".");
        return relativeName.substring(0, relativeName.length() - ".java".length());
    }

    private static void writeSources(
            Path outputDirectory,
            Map<String, String> sourcesByQualifiedName) throws IOException {
        for (Map.Entry<String, String> entry : sourcesByQualifiedName.entrySet()) {
            Path outputFile = outputDirectory.resolve(
                    entry.getKey().replace('.', '/') + ".java");
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, entry.getValue(), StandardCharsets.UTF_8);
        }
    }

    private static void recreateDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        Files.createDirectories(directory);
    }

    private record Arguments(Path outputDirectory, List<Path> sourceDirectories) {
        private static Arguments parse(String[] arguments) {
            Path outputDirectory = null;
            List<Path> sourceDirectories = new ArrayList<>();
            for (int index = 0; index < arguments.length; index++) {
                String argument = arguments[index];
                switch (argument) {
                    case "--output" -> {
                        outputDirectory = Path.of(requireValue(arguments, ++index, argument))
                                .toAbsolutePath()
                                .normalize();
                    }
                    case "--source" -> sourceDirectories.add(
                            Path.of(requireValue(arguments, ++index, argument))
                                    .toAbsolutePath()
                                    .normalize());
                    default -> throw new IllegalArgumentException(
                            "Unknown dependency access lowerer argument: " + argument);
                }
            }
            if (outputDirectory == null) {
                throw new IllegalArgumentException("--output is required");
            }
            if (sourceDirectories.isEmpty()) {
                throw new IllegalArgumentException("At least one --source directory is required");
            }
            return new Arguments(outputDirectory, List.copyOf(sourceDirectories));
        }

        private static String requireValue(
                String[] arguments,
                int valueIndex,
                String argumentName) {
            if (valueIndex >= arguments.length || arguments[valueIndex].isBlank()) {
                throw new IllegalArgumentException(argumentName + " requires a value");
            }
            return arguments[valueIndex];
        }
    }
}
