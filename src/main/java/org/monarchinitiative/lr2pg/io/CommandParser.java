package org.monarchinitiative.lr2pg.io;

import java.io.PrintWriter;
import org.apache.commons.cli.*;
import org.monarchinitiative.lr2pg.command.Command;
import org.monarchinitiative.lr2pg.command.DownloadCommand;

public class CommandParser {

    private String hpoPath=null;
    private String annotationPath=null;
    /** Path to a file with a list of HPO terms that a "patient" has. */
    private String patientAnnotations=null;
    /** Path to directory where we will download the needed files. */
    private String dataDownloadDirectory=null;

    private String mycommand=null;

    private Command command=null;

    public String getHpoPath() {
        return hpoPath;
    }

    public String getAnnotationPath() {
        return annotationPath;
    }

    public String getPatientAnnotations() { return patientAnnotations; }

    public CommandParser(String args[]) {
        final CommandLineParser cmdLineGnuParser = new DefaultParser();

        final Options gnuOptions = constructOptions();
        org.apache.commons.cli.CommandLine commandLine;

        try
        {
            commandLine = cmdLineGnuParser.parse(gnuOptions, args);
            String category[] = commandLine.getArgs();
            if (category.length != 1) {
                printUsage("command missing");
            } else {
                mycommand = category[0];

            }
            if (commandLine.getArgs().length < 1) {
                printUsage("no arguments passed");
                return;
            }


            if (commandLine.hasOption("o")) {
                hpoPath=commandLine.getOptionValue("o");
            } else {
                printUsage("[ERROR] hp.obo file (-o) required.");
                System.exit(1);
            }
            if (commandLine.hasOption("a")) {
                annotationPath=commandLine.getOptionValue("a");
            } else {
                printUsage("[ERROR] phenotype_annotation.tab file (-h) required.");
                System.exit(1);
            }
            if (commandLine.hasOption("i")) {
                patientAnnotations=commandLine.getOptionValue("i");
            }
            if (commandLine.hasOption("d")) {
                this.dataDownloadDirectory=commandLine.getOptionValue("d");
            }

            if (mycommand.equals("download")) {
                if (this.dataDownloadDirectory == null) {
                    System.out.println("Will download to the default location: \"data\"");
                    this.dataDownloadDirectory="data";
                }
                this.command=new DownloadCommand(dataDownloadDirectory);
            } else {
                printUsage(String.format("Did not recognize command: %s", mycommand));
            }
        }
        catch (ParseException parseException)  // checked exception
        {
            System.err.println(
                    "Encountered exception while parsing using GnuParser:\n"
                            + parseException.getMessage() );
        }

    }


    public Command getCommand() {
        return command;
    }

    /**
     * Construct and provide GNU-compatible Options.
     *
     * @return Options expected from command-line of GNU form.
     */
    public static Options constructOptions()
    {
        final Options gnuOptions = new Options();
        gnuOptions.addOption("o", "hpo", true, "HPO OBO file path")
                .addOption("d", "download", true, "path of direcotry to download files")
                .addOption("i","patient-hpo-terms",true, "list of HPO terms for the patient")
                .addOption("a", "annotations", true, "Annotation file path");
        return gnuOptions;
    }



    /**
     * Print usage information to provided OutputStream.
     */
    public static void printUsage(String message) {
        final PrintWriter writer = new PrintWriter(System.out);
        final HelpFormatter usageFormatter = new HelpFormatter();
        final String applicationName = "java -jar dimorph.jar command";
        final Options options = constructOptions();
        usageFormatter.printUsage(writer, 120, applicationName, options);
        writer.println("\twhere command is one of download,....");
        writer.println("\t- download [-d direcotry]: Download neeeded files to directory at (-d).");
        writer.close();
        System.exit(0);
    }


}

