package exports;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatch;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;


public class Exporter implements Closeable, Flushable {
    private Map<String, OutputStream> openAppendFiles = new HashMap<>();
    private Path outdir;

    public Exporter(Path outdir, boolean clobber) {
        if (clobber)
            try {
                FileUtils.deleteDirectory(outdir.toFile());
            } catch (Exception e) {
                System.err.println(e);
            }

        if (Files.notExists(outdir)) {
            try {
                Files.createDirectory(outdir);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.outdir = outdir;
    }

    public OutputStream writeOnceStream(Path fpath)
            throws IOException {
        Path parentdir = fpath.getParent();
        if (Files.notExists(parentdir))
            try {
                Files.createDirectories(parentdir);
            } catch (Exception e) {
                e.printStackTrace();
            }

        return Files.newOutputStream(fpath, StandardOpenOption.CREATE);
    }

    public AttachmentMetadata writeFromBuffer(String type, String basename, byte[] filedata)
            throws IOException {
        String ext = "";
        AttachmentMetadata am = new AttachmentMetadata();

        try {
            MagicMatch mm = Magic.getMagicMatch(filedata);
            am.mimetype = mm.getMimeType();
            if (mm.getExtension() == "") {
                if (am.mimetype == "image/gif")
                    ext = ".gif";
                else 
                    System.err.println(String.format("No extension for %s", am.mimetype));
            } else {
                ext = "." + mm.getExtension();
            }
        } catch (MagicMatchNotFoundException | MagicParseException | MagicException e) {
            System.err.println(String.format("Could not infer type for %s", basename));
            ext = ".mp4";
        }

        am.path = this.outdir.resolve(type).resolve(basename + ext);

        OutputStream os = this.writeOnceStream(am.path);
        os.write(filedata);
        os.close();

        return am;
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
