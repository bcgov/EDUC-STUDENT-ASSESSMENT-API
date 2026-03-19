package ca.bc.gov.educ.assessment.api.util;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class FileTrimAndZip {

    // ─── CONFIG ───────────────────────────────────────────────────────────────
    static final String INPUT_FOLDER_NAME = "input";   // Folder name inside src/main/resources
    static final String OUTPUT_FOLDER     = "./output";
    static final String ZIP_NAME          = "processed_files.zip";
    static final int    EXPECTED_LINE_LENGTH = 122;    // Length AFTER stripping
    // ──────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws IOException {

        // 1. Resolve input folder from classpath (works in Maven/IDE/jar)
        File inputDir = resolveFromClasspath(INPUT_FOLDER_NAME);
        if (inputDir == null || !inputDir.exists() || !inputDir.isDirectory()) {
            System.err.println("❌ Input folder not found: \"" + INPUT_FOLDER_NAME + "\"");
            System.err.println("   Make sure the folder exists at: src/main/resources/" + INPUT_FOLDER_NAME);
            System.exit(1);
        }
        System.out.println("📁 Input folder resolved to: " + inputDir.getAbsolutePath());

        // 2. Create output folder (clean slate)
        File outputDir = new File(OUTPUT_FOLDER);
        if (outputDir.exists()) deleteDirectory(outputDir);
        outputDir.mkdirs();

        File[] files = inputDir.listFiles(File::isFile);
        if (files == null || files.length == 0) {
            System.err.println("❌ No files found in input folder.");
            System.exit(1);
        }

        Arrays.sort(files);
        System.out.println("📂 Found " + files.length + " files to process...");

        int totalLines = 0;
        int warnings   = 0;

        // 3. Process each file
        for (int i = 0; i < files.length; i++) {
            File inputFile  = files[i];
            File outputFile = new File(OUTPUT_FOLDER, inputFile.getName());

            List<String> inputLines  = Files.readAllLines(inputFile.toPath());
            List<String> outputLines = new ArrayList<>();

            for (int lineNum = 0; lineNum < inputLines.size(); lineNum++) {
                String line = inputLines.get(lineNum);

                if (line.isEmpty()) {
                    outputLines.add(line);
                    continue;
                }

                // Strip last 8 characters
                String stripped = line.length() > 8
                        ? line.substring(0, line.length() - 8)
                        : "";

                // Warn if result doesn't match expected length
                if (stripped.length() != EXPECTED_LINE_LENGTH) {
                    System.out.printf("  ⚠️  %s line %d: expected %d chars after strip, got %d (original: %d)%n",
                            inputFile.getName(), lineNum + 1,
                            EXPECTED_LINE_LENGTH, stripped.length(), line.length());
                    warnings++;
                }

                outputLines.add(stripped);
                totalLines++;
            }

            Files.write(outputFile.toPath(), outputLines);

            if ((i + 1) % 50 == 0 || i + 1 == files.length) {
                System.out.printf("  ✅ Processed %d/%d files...%n", i + 1, files.length);
            }
        }

        // 4. Zip all processed files
        System.out.println("\n📦 Zipping processed files into \"" + ZIP_NAME + "\"...");
        zipDirectory(outputDir, ZIP_NAME);
        System.out.println("✅ Done! Zip created: " + ZIP_NAME);

        // 5. Summary
        System.out.println("\n📊 Summary:");
        System.out.println("   Files processed : " + files.length);
        System.out.println("   Total lines     : " + totalLines);
        System.out.println("   Warnings        : " + warnings);
    }

    /**
     * Resolves a folder from the classpath (i.e. src/main/resources/<name>).
     * Works when run from an IDE, via "mvn exec:java", or plain "java" after compile.
     */
    static File resolveFromClasspath(String folderName) {
        // Try classpath lookup first
        URL url = FileTrimAndZip.class.getClassLoader().getResource(folderName);
        if (url != null) {
            try {
                return new File(url.toURI());
            } catch (URISyntaxException e) {
                return new File(url.getPath());
            }
        }

        // Fallback: walk up from working directory to find src/main/resources/<name>
        File dir = new File(System.getProperty("user.dir"));
        while (dir != null) {
            File candidate = new File(dir, "src/main/resources/" + folderName);
            if (candidate.exists() && candidate.isDirectory()) return candidate;
            dir = dir.getParentFile();
        }

        return null;
    }

    // Zip all files inside a directory into a single zip file
    static void zipDirectory(File sourceDir, String zipName) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipName))) {
            for (File file : Objects.requireNonNull(sourceDir.listFiles(File::isFile))) {
                try (InputStream fis = new FileInputStream(file)) {
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = fis.read(buffer)) > 0) zos.write(buffer, 0, len);
                    zos.closeEntry();
                }
            }
        }
    }

    // Recursively delete a directory
    static void deleteDirectory(File dir) {
        File[] contents = dir.listFiles();
        if (contents != null) for (File f : contents) deleteDirectory(f);
        dir.delete();
    }
}