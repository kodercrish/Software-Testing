package st.pipeline.exec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs an external command (javac / java) with a timeout, capturing combined output. */
public final class Proc {
    public record Result(int exitCode, boolean timedOut, String output) {
        public boolean ok() { return !timedOut && exitCode == 0; }
    }

    private Proc() {}

    public static Result run(List<String> cmd, Path workDir, int timeoutSeconds) throws IOException, InterruptedException {
        Files.createDirectories(workDir);
        Path log = Files.createTempFile(workDir, "proc", ".log");
        Process p = new ProcessBuilder(cmd)
                .directory(workDir.toFile())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        boolean finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            p.descendants().forEach(ProcessHandle::destroyForcibly);
            p.destroyForcibly().waitFor();
        }
        String out = Files.readString(log);
        Files.delete(log);
        return new Result(finished ? p.exitValue() : -1, !finished, out);
    }

    public static String classpath(Path... entries) {
        return String.join(File.pathSeparator, java.util.Arrays.stream(entries).map(Path::toString).toList());
    }
}
