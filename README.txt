XSort is a Java program that sorts big lists of data coming from standard input and shows the sorted list on standard output. It uses a heap sort to make small sorted chunks and then mixes them together with a balanced 2-way merge trick, using temp files to handle big data that doesn’t fit in memory.

How to Use It:
Compile the code: javac XSort.java
Run it: cat MobyDick.txt | java XSort 64 2 > Moby.sorted
Run Size: A number between 64 and 1024 that says how many lines go in each chunk.
Merge Flag: Add a 2 to mix all the chunks together; if you skip it, you just get the first chunk without mixing.

How It’s Built:
Temp Files: Makes short-term files in the current folder (.) with names starting with run, merge, or empty.
Buffer Size: Uses 8192 bytes to read and write fast.
File Tracking: Keeps a list (called a HashSet) of all temp files so it can clean them up later.

How It Works:
Making Chunks: Splits the input into small groups (runs) of the size you pick, sorts each one with heap sort, and saves them as temp files.
Mixing: If you add the 2 flag, it mixes the chunks two at a time until there’s just one sorted file left.
Showing the Result: Sends the final sorted file.
Cleaning Up: Deletes all the temp files when it’s done.