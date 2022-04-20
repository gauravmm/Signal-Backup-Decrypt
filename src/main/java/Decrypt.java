import java.io.IOException;
import java.lang.String;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import securesms.backup.FullBackupImporter;

public class Decrypt {
  public static void main(String[] args) {

    ArgumentParser parser = ArgumentParsers.newFor("Decrypt Signal Backups").build()
        .defaultHelp(true)
        .description("Decrypt Signal messenger backups.");
    parser.addArgument("-v", "--verbose")
        .action(Arguments.storeTrue())
        .help("Be verbose");
    parser.addArgument("file").type(String.class);
    parser.addArgument("key").type(String.class);
    parser.addArgument("--output-dir").type(String.class).setDefault("out");
    Namespace ns = null;

    try {
      ns = parser.parseArgs(args);

      Path outdir = Paths.get(ns.getString("output_dir"));

      try {
        FullBackupImporter.importFile(ns.getString("file"), ns.getString("key"), outdir);

      } catch (IOException e) {
        System.err.println(e);
        return;
      }

    } catch (ArgumentParserException e) {
      parser.handleError(e);
    }

  }
}
