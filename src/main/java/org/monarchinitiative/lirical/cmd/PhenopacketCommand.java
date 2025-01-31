package org.monarchinitiative.lirical.cmd;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Multimap;

import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.io.PhenopacketImporter;
import org.monarchinitiative.lirical.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.output.LiricalTemplate;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Download a number of files needed for LIRICAL analysis
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Run LIRICAL from a Phenopacket")
public class PhenopacketCommand extends PrioritizeCommand {
    private static final Logger logger = LoggerFactory.getLogger(PhenopacketCommand.class);
    @Parameter(names = {"-b", "--background"}, description = "path to non-default background frequency file")
    protected String backgroundFrequencyFile;
    @Parameter(names = {"-p", "--phenopacket"}, description = "path to phenopacket file")
    protected String phenopacketPath = null;
    @Parameter(names = {"-e", "--exomiser"}, description = "path to the Exomiser data directory")
    protected String exomiserDataDirectory = null;
    @Parameter(names={"--transcriptdb"}, description = "transcript database (UCSC, Ensembl, RefSeq)")
    protected String transcriptDb="refseq";
    /** Reference to HPO object. */
    private Ontology ontology;

    /**
     * If true, the phenopacket contains the path of a VCF file.
     */
    private boolean hasVcf;
    /**
     * List of HPO terms observed in the subject of the investigation.
     */
    private List<TermId> hpoIdList;
    /**
     * List of excluded HPO terms in the subject.
     */
    private List<TermId> negatedHpoIdList;
    /**
     * String representing the genome build (hg19 or hg38). We get this from the Phenopacket
     */
    private String genomeAssembly;
    /**
     * Path to the VCF file (if any).
     */
    private String vcfPath = null;

    public PhenopacketCommand() {
    }

    /**
     * Run an analysis of a phenopacket that contains a VCF file.
     */
    private void runVcfAnalysis() {
        LiricalFactory factory = new LiricalFactory.Builder(ontology)
                .datadir(this.datadir)
                .genomeAssembly(this.genomeAssembly)
                .exomiser(this.exomiserDataDirectory)
                .vcf(this.vcfPath)
                .backgroundFrequency(this.backgroundFrequencyFile)
                .global(this.globalAnalysisMode)
                .orphanet(this.useOrphanet)
                .transcriptdatabase(this.transcriptDb)
                .build();
        factory.qcHumanPhenotypeOntologyFiles();
        factory.qcExternalFilesInDataDir();
        factory.qcExomiserFiles();
        factory.qcGenomeBuild();
        factory.qcVcfFile();

        Map<TermId, Gene2Genotype> genotypemap = factory.getGene2GenotypeMap();

        GenotypeLikelihoodRatio genoLr = factory.getGenotypeLR();
        Ontology ontology = factory.hpoOntology();
        Map<TermId, HpoDisease> diseaseMap = factory.diseaseMap(ontology);
        PhenotypeLikelihoodRatio phenoLr = new PhenotypeLikelihoodRatio(ontology, diseaseMap);
        Multimap<TermId, TermId> disease2geneMultimap = factory.disease2geneMultimap();
        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(this.hpoIdList)
                .ontology(ontology)
                .negated(this.negatedHpoIdList)
                .diseaseMap(diseaseMap)
                .disease2geneMultimap(disease2geneMultimap)
                .genotypeMap(genotypemap)
                .phenotypeLr(phenoLr)
                .genotypeLr(genoLr);

        CaseEvaluator evaluator = caseBuilder.build();
        HpoCase hcase = evaluator.evaluate();


        if (!factory.transcriptdb().equals("n/a")) {
            this.metadata.put("transcriptDatabase", factory.transcriptdb());
        }
        int n_genes_with_var = factory.getGene2GenotypeMap().size();
        this.metadata.put("genesWithVar", String.valueOf(n_genes_with_var));
        this.metadata.put("exomiserPath", factory.getExomiserPath());
        this.metadata.put("hpoVersion", factory.getHpoVersion());
        this.metadata.put("sample_name", factory.getSampleName());
        this.geneId2symbol = factory.geneId2symbolMap();
        List<String> errors = evaluator.getErrors();
        LiricalTemplate.Builder builder = new LiricalTemplate.Builder(hcase,ontology,this.metadata)
                .genotypeMap(genotypemap)
                .geneid2symMap(this.geneId2symbol)
                .threshold(this.LR_THRESHOLD)
                .mindiff(minDifferentialsToShow)
                .errors(errors)
                .outdirectory(this.outdir)
                .prefix(this.outfilePrefix);
        LiricalTemplate template = outputTSV ?
                builder.buildGenoPhenoTsvTemplate() :
                builder.buildGenoPhenoHtmlTemplate();
        template.outputFile();
    }

    /**
     * Run an analysis of a phenopacket that only has Phenotype data
     */
    private void runPhenotypeOnlyAnalysis() {
        LiricalFactory factory = new LiricalFactory.Builder(ontology)
                .datadir(this.datadir)
                .orphanet(this.useOrphanet)
                .build();
        factory.qcHumanPhenotypeOntologyFiles();
        factory.qcExternalFilesInDataDir();
        Ontology ontology = factory.hpoOntology();
        Map<TermId, HpoDisease> diseaseMap = factory.diseaseMap(ontology);
        PhenotypeLikelihoodRatio phenoLr = new PhenotypeLikelihoodRatio(ontology, diseaseMap);
        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(this.hpoIdList)
                .ontology(ontology)
                .negated(this.negatedHpoIdList)
                .diseaseMap(diseaseMap)
                .phenotypeLr(phenoLr);
        CaseEvaluator evaluator = caseBuilder.buildPhenotypeOnlyEvaluator();
        HpoCase hcase = evaluator.evaluate();
        this.metadata.put("hpoVersion", factory.getHpoVersion());
        List<String> errors = evaluator.getErrors();
        LiricalTemplate.Builder builder = new LiricalTemplate.Builder(hcase,ontology,this.metadata)
                .prefix(this.outfilePrefix)
                .outdirectory(this.outdir)
                .errors(errors)
                .threshold(this.LR_THRESHOLD)
                .mindiff(this.minDifferentialsToShow);
        LiricalTemplate template = outputTSV ?
                builder.buildPhenotypeTsvTemplate() :
                builder.buildPhenotypeHtmlTemplate();
        template.outputFile();
    }


    @Override
    public void run() {
        // read the Phenopacket
        if (phenopacketPath==null) {
            logger.error("-p option (phenopacket) is required");
            return;
        }
        this.metadata = new HashMap<>();
        String hpoPath = String.format("%s%s%s",this.datadir, File.separator,"hp.obo");
        Ontology ontology = OntologyLoader.loadOntology(new File(hpoPath));
        PhenopacketImporter importer = PhenopacketImporter.fromJson(phenopacketPath,ontology);
        this.hasVcf = importer.hasVcf();
        if (this.hasVcf) {
            this.vcfPath = importer.getVcfPath();
            this.metadata.put("vcf_file", this.vcfPath);
        }
        this.genomeAssembly = importer.getGenomeAssembly();
        this.hpoIdList = importer.getHpoTerms();
        this.negatedHpoIdList = importer.getNegatedHpoTerms();
        metadata.put("sample_name", importer.getSamplename());


        logger.trace("Will analyze phenopacket at " + phenopacketPath);
        this.metadata = new HashMap<>();
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        this.metadata.put("analysis_date", dateFormat.format(date));
        this.metadata.put("phenopacket_file", this.phenopacketPath);



        if (this.hasVcf ) {
            runVcfAnalysis(); // try to run VCF analysis because use passed -e option.
        } else {
            // i.e., the Phenopacket has no VCF reference -- LIRICAL will work on just phenotypes!
            runPhenotypeOnlyAnalysis();
        }
    }
}
