package org.monarchinitiative.lr2pg;


import org.monarchinitiative.lr2pg.command.*;
import org.monarchinitiative.lr2pg.io.CommandParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Component
public class Lr2pgApplicationRunner implements ApplicationRunner  {
    public static final Logger logger = LoggerFactory.getLogger(Lr2pgApplicationRunner.class);

    /**
     * Path to directory where we will download the needed files.
     */
    private String dataDownloadDirectory = null;
    /** This is where we download the files to by default (otherwise, specify {@code -f <arg>}).*/
    private static final String DEFAULT_DATA_DOWNLOAD_DIRECTORY = "data";
    /** The default number of "random" HPO cases to simulate.*/
    private static final int DEFAULT_N_CASES_TO_SIMULATE = 1000;
    /** The default number of terms to simulate per case.*/
    private static final int DEFAULT_N_TERMS_PER_CASE = 5;
    /** The default number of ranomd (noise) terms to add per simulated case*/
    private static final int DEFAULT_N_NOISE_TERMS_PER_CASE = 1;
    /** The number of HPO Cases to simulate.*/
    private int n_cases_to_simulate;
    /** The number of random HPO terms to simulate in each simulated case.*/
    private int n_terms_per_case;
    /** The number of random noise terms to add to each simulated HPO case.*/
    private int n_noise_terms;
    /** CURIE of disease (e.g., OMIM:600100) for the analysis. */
    private String diseaseId =null;
    /** If true, we do a grid search over the parameters for LR2PG clinical. */
    private boolean gridSearch=false;
    /** Default name of the SVG file with the results of analysis. */
    private static final String DEFAULT_SVG_OUTFILE_NAME="test.svg";
    /** Name of the SVG file with the results of analysis. */
    private String svgOutFileName=null;
    /** If true, overwrite previously downloaded files. */
    private boolean overwrite=false;
    /** Gene id (e.g., 2200 for FBN1) for disease to be simulated. */
    private String entrezGeneId =null;
    /** Mean pathogenicity of variants in pathogenic bin. */
    private double varpath=1.0;
    /** Count of variants in the pathogenic bin */
    private int varcount=1;
    /** Comma separated list of HPO ids */
    private String termList=null;
    /** Path to the file produced by G2GIT - with frequencies for background pathogenic mutations per gene */
    private String backgroundFreq=null;

    private static final String DEFAULT_BACKGROUND_FREQ=String.format("%s%s%s",
            DEFAULT_DATA_DOWNLOAD_DIRECTORY, File.separator,"background-freq.txt");

    /**The command object.*/
    private Command command = null;

    @Override
    public void run(ApplicationArguments args) {
        logger.info("Application started with command-line arguments: {}", Arrays.toString(args.getSourceArgs()));


        logger.info("NonOptionArgs: {}", args.getNonOptionArgs());
        logger.info("OptionNames: {}", args.getOptionNames());
        for (String name : args.getOptionNames()){
            logger.info("arg-" + name + "=" + args.getOptionValues(name));
        }

        if (args.containsOption("help") || args.containsOption("h")) {
            printUsage("See the README file for usage!");
        }

        List<String> nonoptionargs = args.getNonOptionArgs();
        if (nonoptionargs.size() != 1) {
            printUsage("[ERROR] No program command given");
        }
        // if we get here, we have one command
        String mycommand = nonoptionargs.get(0);

        // collect the options
        if (args.containsOption("download")) {
            this.dataDownloadDirectory = args.getOptionValues("download").get(0);
        }

        if (args.containsOption("grid")) {
            this.gridSearch = true;
        }
        if (args.containsOption("term-list")) {
            this.termList = args.getOptionValues("term-list").get(0);
        }
        if (args.containsOption("varcount")) {
            try {
                varcount=Integer.parseInt(args.getOptionValues("varcount").get(0));
            } catch (NumberFormatException e) {
                System.err.println("Count not parse varcount");
                e.printStackTrace();
                System.exit(1);
            }
        }

        if (args.containsOption("varpath")) {
            try {
                varpath=Double.parseDouble(args.getOptionValues("varpath").get(0));
            } catch (NumberFormatException e) {
                System.err.println("Count not parse variant pathogenicity");
                e.printStackTrace();
                System.exit(1);
            }
        }

        if (args.containsOption("geneid")) {
            this.entrezGeneId =args.getOptionValues("geneid").get(0);
        }

        if (args.containsOption("overwrite")) {
            this.overwrite=true;
        }

        if (args.containsOption("disease")) {
            diseaseId =args.getOptionValues("disease").get(0);
        }
        if (args.containsOption("svg")) {
            svgOutFileName=args.getOptionValues("svg").get(0);
        } else {
            svgOutFileName=DEFAULT_SVG_OUTFILE_NAME;
        }
        if (args.containsOption("t")) {
            String term = args.getOptionValues("t").get(0);
            try {
                n_terms_per_case = Integer.parseInt(term);
            } catch (NumberFormatException nfe) {
                printUsage("[ERROR] Malformed argument for -t option (must be integer)");
            }
        } else {
            n_terms_per_case = DEFAULT_N_TERMS_PER_CASE;
        }
        if (args.containsOption("n")) {
            String noise = args.getOptionValues("n").get(0);
            try {
                n_noise_terms = Integer.parseInt(noise);
            } catch (NumberFormatException nfe) {
                printUsage("[ERROR] Malformed argument for -n option (must be integer)");
            }
        } else {
            n_noise_terms = DEFAULT_N_NOISE_TERMS_PER_CASE;
        }
        if (args.containsOption("s")) {
            String simul = args.getOptionValues("s").get(0);
            try {
                n_cases_to_simulate = Integer.parseInt(simul);
            } catch (NumberFormatException nfe) {
                printUsage("[ERROR] Malformed argument for -s option (must be integer)");
            }
        } else {
            n_cases_to_simulate = DEFAULT_N_CASES_TO_SIMULATE;
        }





        switch (mycommand) {
            case "download":
                if (this.dataDownloadDirectory == null) {
                    this.dataDownloadDirectory = DEFAULT_DATA_DOWNLOAD_DIRECTORY;
                }
                logger.warn(String.format("Download command to %s", dataDownloadDirectory));
                this.command = new DownloadCommand(dataDownloadDirectory, overwrite);
                break;
            case "simulate":
                if (this.dataDownloadDirectory == null) {
                    this.dataDownloadDirectory = DEFAULT_DATA_DOWNLOAD_DIRECTORY;
                }
                this.command = new SimulateCasesCommand(this.dataDownloadDirectory,
                        n_cases_to_simulate, n_terms_per_case, n_noise_terms, gridSearch);
                break;
            case "svg":
                if (this.dataDownloadDirectory == null) {
                    this.dataDownloadDirectory = DEFAULT_DATA_DOWNLOAD_DIRECTORY;
                }
                if (diseaseId ==null) {
                    printUsage("svg command requires --disease option");
                }
                //n_terms_per_case, n_noise_terms);
                this.command = new HpoCase2SvgCommand(this.dataDownloadDirectory, diseaseId,svgOutFileName,n_terms_per_case,n_noise_terms);
                break;
            case "phenogeno":
                if (this.dataDownloadDirectory == null) {
                    this.dataDownloadDirectory = DEFAULT_DATA_DOWNLOAD_DIRECTORY;
                }
                if (termList==null) {
                    System.err.println("[ERROR] --term-list with list of HPO ids required");
                    phenoGenoUsage();
                    System.exit(1);
                }
                if (diseaseId==null){
                    System.err.println("[ERROR] --disease option (e.g., OMIM:600100) required");
                    phenoGenoUsage();
                    System.exit(1);
                }
                if (entrezGeneId==null){
                    System.err.println("[ERROR] --geneid option (e.g., 2200) required");
                    phenoGenoUsage();
                    System.exit(1);
                }
                if (backgroundFreq==null) {
                    backgroundFreq=DEFAULT_BACKGROUND_FREQ;
                }
                this.command = new SimulatePhenoGenoCaseCommand(this.dataDownloadDirectory,
                        this.entrezGeneId,
                        this.varcount,
                        this.varpath,
                        this.diseaseId,
                        this.termList,
                        this.backgroundFreq);
                break;
            default:
                printUsage(String.format("Did not recognize command: \"%s\"", mycommand));
        }

        command.execute();
        logger.trace("done execution");


    }



    private static String getVersion() {
        String DEFAULT="0.4.0";// default, should be overwritten by the following.
        String version=null;
        try {
            Package p = CommandParser.class.getPackage();
            version = p.getImplementationVersion();
        } catch (Exception e) {
            // do nothing
        }
        return version!=null?version:DEFAULT;
    }

    private static void printUsageIntro() {
        String version = getVersion();
        System.out.println();
        System.out.println("Program: LR2PG (v. "+version +")");
        System.out.println();
        System.out.println("Usage: java -jar Lr2pg.jar <command> [options]");
        System.out.println();
        System.out.println("Available commands:");
        System.out.println();
    }

    private static void phenoGenoUsage() {
        System.out.println("phenogeno:");
        System.out.println("\tjava -jar Lr2pg.jar phenogeno --disease <id> --geneid <string> \\\n" +
                "\t\t--term-list <string> [-d <directory>] [--varcount <int>]\\\n" +
                "\t\t-b <file> [--varpath <double>]");
        System.out.println("\t--disease <id>: id of disease to simulate (e.g., OMIM:600321)");
        System.out.println("\t-d <directory>: name of directory with HPO data (default:\"data\")");
        System.out.println("\t-b <file>: path to background-freq.txt file");
        System.out.println("\t--geneid <string>: symbol of affected gene");
        System.out.println("\t--term-list <string>: comma-separated list of HPO ids");
        System.out.println("\t--varcount <int>: number of variants in pathogenic bin (default: 1)");
        System.out.println("\t--varpath <double>: mean pathogenicity score of variants in pathogenic bin (default: 1.0)");
    }

    private static void simulateUsage() {
        System.out.println("simulate:");
        System.out.println("\tjava -jar Lr2pg.jar simulate [-d <directory>] [-s <int>] [-t <int>] [-n <int>] [--grid]");
        System.out.println("\t-d <directory>: name of directory with HPO data (default:\"data\")");
        System.out.println(String.format("\t-s <int>: number of cases to simulate (default: %d)", DEFAULT_N_CASES_TO_SIMULATE));
        System.out.println(String.format("\t-t <int>: number of HPO terms per case (default: %d)", DEFAULT_N_TERMS_PER_CASE));
        System.out.println(String.format("\t-n <int>: number of noise terms per case (default: %d)", DEFAULT_N_NOISE_TERMS_PER_CASE));
        System.out.println("\t--grid: Indicates a grid search over noise and imprecision should be performed");
        System.out.println();
    }

    private static void svgUsage() {
        System.out.println("svg:");
        System.out.println("\tjava -jar Lr2pg.jar svg --disease <name> [-- svg <file>] [-d <directory>] [-t <int>] [-n <int>]");
        System.out.println("\t--disease <string>: name of disease to simulate (e.g., OMIM:600321)");
        System.out.println(String.format("\t--svg <file>: name of output SVG file (default: %s)", DEFAULT_SVG_OUTFILE_NAME));
        System.out.println(String.format("\t-t <int>: number of HPO terms per case (default: %d)", DEFAULT_N_TERMS_PER_CASE));
        System.out.println(String.format("\t-n <int>: number of noise terms per case (default: %d)", DEFAULT_N_NOISE_TERMS_PER_CASE));
    }

    private static void downloadUsage() {
        System.out.println("download:");
        System.out.println("\tjava -jar Lr2pg.jar download [-d <directory>] [--overwrite]");
        System.out.println("\t-d <directory>: name of directory to which HPO data will be downloaded (default:\"data\")");
        System.out.println("\t--overwrite: do not skip even if file already downloaded");
        System.out.println();
    }



    /**
     * Print usage information
     */
    public static void printUsage(String message) {
        printUsageIntro();
        System.out.println();
        System.out.println(message);
        downloadUsage();
        simulateUsage();
        phenoGenoUsage();
        svgUsage();
        System.exit(0);
    }
}
