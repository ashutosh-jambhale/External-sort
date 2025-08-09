/*
   Balanced 2-way Sort Merge
   Name: Ashutosh Jambhale
   References:
   Heap Sort: https://www.geeksforgeeks.org/heap-sort/, https://www.programiz.com/dsa/heap-sort
   Newline Handling: https://stackoverflow.com/questions/42769110/keep-new-lines-when-reading-in-a-file
   Temp Files: https://www.geeksforgeeks.org/create-a-temporary-file-in-java/
   Tape Sorting: https://www.geeksforgeeks.org/sorting-with-tapes-balanced-merge/
*/
import java.io.*;
import java.util.*;

public class XSort {
    private static final int MIN_RUN = 64;
    private static final int MAX_RUN = 1024;
    private static final File TEMP_DIR = new File(".");
    private static final int BUFFER_SIZE = 8192;
    private static final Set<File> ALL_TEMP_FILES = new HashSet<>(); // to track all temp files

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage ex: cat MobyDick.txt | java XSort 64 2 > Moby.sorted");
            System.exit(1);
        }

        int runSize = parseRunSize(args[0]);
        boolean mergeRequested = args.length == 2 && args[1].equals("2");

        try {
            if (!TEMP_DIR.canWrite()) {
                System.err.println("Cannot write in temp directory: " + TEMP_DIR.getAbsolutePath());
                System.exit(1);
            }

            System.err.println("Creating initial runs");
            List<File> initialRuns = createInitialRuns(runSize);
            System.err.println("Initial runs created " + initialRuns.size());

            if (mergeRequested) {                              // Checks if merging is requested
                System.err.println("Starting balanced 2-way merge");
                File sortedFile = balancedMergeSort(initialRuns);
                System.err.println("Merging complete.");
                outputToStdout(sortedFile);
            } else if (!initialRuns.isEmpty()) {
                System.err.println("No merging requested.");
                outputToStdout(initialRuns.get(0));
            }
            cleanAllTempFiles();
        } catch (IOException e) {
            System.err.println("Error during sorting: " + e.getMessage());
            System.exit(1);
        }
    }

    private static int parseRunSize(String arg) {      // Parses run size from string argument
        try {
            int runSize = Integer.parseInt(arg);
            if (runSize < MIN_RUN || runSize > MAX_RUN) {
                System.err.println("Run size must be from " + MIN_RUN + " to " + MAX_RUN + ".");
                System.exit(1);
            }
            return runSize;
        } catch (NumberFormatException e) {
            System.err.println("Run size should be an integer");
            System.exit(1);
            return 0;
        }
    }

    private static List<File> createInitialRuns(int runSize) throws IOException {         // Creates sorted runs from input
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in), BUFFER_SIZE)) {
            List<File> runs = new ArrayList<>();
            List<String> buffer = new ArrayList<>(runSize);
            String line;
            int runCount = 0;

            while ((line = reader.readLine()) != null) {  // Reads lines until end of input
                buffer.add(line);
                if (buffer.size() == runSize) {
                    heapSort(buffer);
                    File runFile = createTempFile("run" + runCount++ + "-");
                    writeToFile(buffer, runFile);
                    runs.add(runFile);
                    ALL_TEMP_FILES.add(runFile); // track for cleanup
                    buffer.clear();
                }
            }
            if (!buffer.isEmpty()) {  // checks for leftover lines in buffer
                heapSort(buffer);
                File runFile = createTempFile("run" + runCount + "-");
                writeToFile(buffer, runFile);
                runs.add(runFile);
                ALL_TEMP_FILES.add(runFile);
            }

            return runs;
        }
    }

    private static void writeToFile(List<String> list, File file) throws IOException {        // writes list to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file), BUFFER_SIZE)) {
            for (String line : list) {
                writer.write(line);
                writer.newLine();
            }
        }
        System.err.println("Wrote data: " + file.getName());
    }

    private static void heapSort(List<String> list) {
        int n = list.size();
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(list, n, i);        // Heapifies each subtree
        }
        for (int i = n - 1; i >= 0; i--) {
            Collections.swap(list, 0, i); // moves max element to end
            heapify(list, i, 0);
        }
    }

    private static void heapify(List<String> list, int n, int i) {
        int Largest = i;
        int left = 2 * i + 1;
        int right = 2 * i + 2;

        if (left < n && list.get(left).compareTo(list.get(Largest)) > 0) Largest = left; // Checks if left child is larger
        if (right < n && list.get(right).compareTo(list.get(Largest)) > 0) Largest = right; // Checks if right child is larger
        if (Largest != i) { // If a child is larger than current node
            Collections.swap(list, i, Largest);
            heapify(list, n, Largest);
        }
    }

    private static File merge(File tape1, File tape2) throws IOException { // Merges 2 sorted files
        File outputTape = createTempFile("merge-");
        ALL_TEMP_FILES.add(outputTape); // Track for cleanup
        try (BufferedReader reader1 = new BufferedReader(new FileReader(tape1), BUFFER_SIZE);
             BufferedReader reader2 = tape2.exists() ? new BufferedReader(new FileReader(tape2), BUFFER_SIZE) : null;
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputTape), BUFFER_SIZE)) {

            String line1 = reader1.readLine();
            String line2 = reader2 != null ? reader2.readLine() : null;

            while (line1 != null || line2 != null) {
                if (line1 == null) {
                    writer.write(line2);
                    writer.newLine();
                    line2 = reader2.readLine();
                } else if (line2 == null) {
                    writer.write(line1);
                    writer.newLine();
                    line1 = reader1.readLine();
                } else if (line1.compareTo(line2) <= 0) {
                    writer.write(line1);
                    writer.newLine();
                    line1 = reader1.readLine();
                } else {
                    writer.write(line2);
                    writer.newLine();
                    line2 = reader2.readLine();
                }
            }
        }
        System.err.println("Merged " + tape1.getName() + " and " + (tape2.exists() ? tape2.getName() : "empty") + " into " + outputTape.getName());
        return outputTape;
    }

    private static File balancedMergeSort(List<File> initialRuns) throws IOException { // Merges all runs into 1 file
        if (initialRuns.isEmpty()) {
            System.err.println("no runs to merge");
            return null;
        }
        if (initialRuns.size() == 1) {
            return initialRuns.get(0);
        }

        List<File> tapes = new ArrayList<>(initialRuns); // Creates working copy of initial runs

        while (tapes.size() > 1) {
            List<File> newTapes = new ArrayList<>();
            for (int i = 0; i < tapes.size(); i += 2) {
                File tape1 = tapes.get(i);
                File tape2 = (i + 1 < tapes.size()) ? tapes.get(i + 1) : createNullTape();
                File mergedTape = merge(tape1, tape2);
                newTapes.add(mergedTape);
            }
            tapes = newTapes;
            System.err.println("Runs after this merge pass " + tapes.size());
        }

        return tapes.get(0); // Final file tracked in ALL_TEMP_FILES
    }

    private static File createNullTape() throws IOException { // Creates an empty temp file
        File emptyTape = createTempFile("empty-");
        ALL_TEMP_FILES.add(emptyTape);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(emptyTape), BUFFER_SIZE)) {      // Empty file
        }
        return emptyTape;
    }

    private static void outputToStdout(File file) throws IOException { // Outputs file to stdout
        try (BufferedReader reader = new BufferedReader(new FileReader(file), BUFFER_SIZE)) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
    }

    private static File createTempFile(String prefix) throws IOException { // Creates temp file with prefix
        File file = File.createTempFile(prefix, ".tmp", TEMP_DIR);
        if (!file.canWrite()) {
            throw new IOException("Cannot write to temp file" + file.getAbsolutePath());
        }
        System.err.println("Created temp file " + file.getName());
        return file;
    }
    
    private static void cleanAllTempFiles() {  // Cleans up all tracked temp files
        System.err.println("Cleaning all temporary files");
        for (File file : ALL_TEMP_FILES) {
            if (file.exists()) {
                if (file.delete()) {
                    System.err.println("Deleted file: " + file.getName());
                } else {
                    System.err.println("Failed to delete file: " + file.getName());
                }
            }
        }
        ALL_TEMP_FILES.clear(); // Clears set of tracked files
    }
}