package org.tavall.dependency.access;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class DependencyAccessTestCompiler {
    private DependencyAccessTestCompiler() {
    }

    static Path compileSources(Map<String, String> sourcesByQualifiedName) {
        if (sourcesByQualifiedName == null || sourcesByQualifiedName.isEmpty()) {
            throw new IllegalArgumentException("sourcesByQualifiedName is required");
        }

        try {
            Path workingDirectory = Files.createTempDirectory("dependency-access-compile");
            Path sourceDirectory = workingDirectory.resolve("src");
            Path outputDirectory = workingDirectory.resolve("classes");
            Files.createDirectories(sourceDirectory);
            Files.createDirectories(outputDirectory);

            writeSources(sourceDirectory, sourcesByQualifiedName);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException("A system Java compiler is required to run the dependency-access tests");
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                    diagnostics,
                    null,
                    StandardCharsets.UTF_8)) {
                List<Path> sourceFiles = collectSourceFiles(sourceDirectory);
                Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(sourceFiles);

                String classPath = System.getProperty("java.class.path");
                List<String> compilerOptions = List.of(
                        "-classpath", classPath,
                        "-d", outputDirectory.toString(),
                        "-processor", "org.tavall.dependency.processor.DependencyAccessGrantProcessor");

                Boolean success = compiler.getTask(
                        null,
                        fileManager,
                        diagnostics,
                        compilerOptions,
                        null,
                        compilationUnits).call();

                if (success == null || !success) {
                    throw new IllegalStateException(formatDiagnostics(diagnostics));
                }
            }

            return outputDirectory;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to compile dependency-access sources", exception);
        }
    }

    static Class<?> loadClass(Path outputDirectory, String qualifiedName) {
        try {
            URL outputUrl = outputDirectory.toUri().toURL();
            URLClassLoader loader = new URLClassLoader(new URL[]{outputUrl}, DependencyAccessTestCompiler.class.getClassLoader());
            return Class.forName(qualifiedName, true, loader);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load compiled test class " + qualifiedName, exception);
        }
    }

    private static void writeSources(Path sourceDirectory, Map<String, String> sourcesByQualifiedName) throws IOException {
        for (Map.Entry<String, String> entry : sourcesByQualifiedName.entrySet()) {
            String qualifiedName = entry.getKey();
            String source = entry.getValue();
            Path sourceFile = sourceDirectory.resolve(qualifiedName.replace('.', '/') + ".java");
            Files.createDirectories(sourceFile.getParent());
            Files.writeString(sourceFile, source, StandardCharsets.UTF_8);
        }
    }

    private static List<Path> collectSourceFiles(Path sourceDirectory) throws IOException {
        List<Path> sourceFiles = new ArrayList<>();
        try (var stream = Files.walk(sourceDirectory)) {
            stream.filter(path -> path.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(sourceFiles::add);
        }
        return sourceFiles;
    }

    private static String formatDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
        StringBuilder builder = new StringBuilder("Compilation failed:\n");
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            builder.append(diagnostic.getKind())
                    .append(": ")
                    .append(diagnostic.getMessage(null))
                    .append('\n');
        }
        return builder.toString();
    }
}
