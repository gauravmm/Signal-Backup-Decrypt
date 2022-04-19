package main.java;

import java.lang.String;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.ArgumentType;
import net.sourceforge.argparse4j.inf.Namespace;


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
    Namespace ns = null;

    try {
      ns = parser.parseArgs(args);

      System.out.println(ns.toString());

    } catch (ArgumentParserException e) {
      parser.handleError(e);
    }

  }
}
