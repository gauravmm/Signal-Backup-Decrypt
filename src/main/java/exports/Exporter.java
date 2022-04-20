package exports;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class Exporter implements Closeable, Flushable {
    private Map<String, OutputStream> openAppendFiles = new HashMap<>();
    private Path outdir;

    public Exporter(Path outdir) {
        if (Files.notExists(outdir)) {
            try {
                Files.createDirectory(outdir);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.outdir = outdir;
    }

    public void writeAppendFile(String filename, byte[] data) {
        if (!this.openAppendFiles.containsKey(filename)) {
            Path fileout = this.outdir.resolve(filename);
            try {
                OutputStream os = Files.newOutputStream(fileout, StandardOpenOption.CREATE);
                this.openAppendFiles.put(filename, os);
            } catch (IOException e) {
                System.err.println(e);
                System.exit(1);
            }
        }

        OutputStream os = this.openAppendFiles.get(filename);
        assert os != null;

        try {
            os.write(data);
        } catch (IOException e) {
            System.err.println(e);
            System.exit(1);
        }
    }

    public void flush() {
        for (OutputStream fs : this.openAppendFiles.values())
            try {
                fs.flush();
            } catch (IOException e) {
                System.err.println(e);
            }
    }

    public void close() {
        for (OutputStream fs : this.openAppendFiles.values())
            try {
                fs.close();
            } catch (IOException e) {
                System.err.println(e);
            }

        this.openAppendFiles.clear();
    }
}
