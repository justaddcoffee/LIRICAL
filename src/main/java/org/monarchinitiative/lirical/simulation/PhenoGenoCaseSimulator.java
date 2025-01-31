package org.monarchinitiative.lirical.simulation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.analysis.VcfSimulator;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.io.PhenopacketImporter;
import org.monarchinitiative.lirical.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.output.HtmlTemplate;
import org.monarchinitiative.lirical.output.LiricalTemplate;
import org.monarchinitiative.lirical.output.TsvTemplate;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.core.Disease;
import org.phenopackets.schema.v1.core.HtsFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.monarchinitiative.phenol.formats.hpo.HpoSubOntologyRootTermIds.PHENOTYPIC_ABNORMALITY;
import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getDescendents;

public class PhenoGenoCaseSimulator {
    private static final Logger logger = LoggerFactory.getLogger(PhenoGenoCaseSimulator.class);

    private final File phenopacketFile;

    private final String templateVcfPath;

    private final LiricalFactory factory;

    private final GenomeAssembly genomeAssembly;

    private final Disease simulatedDiagnosis;

    private final TermId simulatedDiseaseId;

    private TermId simulatedDiseaseGene;

    private final String sampleName;

    private final Map<TermId, Gene2Genotype> genotypemap;

    private final Ontology ontology;

    private final Map<TermId, HpoDisease> diseaseMap;

    private final Multimap<TermId, TermId> disease2geneMultimap;

    private final  Multimap<TermId,TermId> gene2diseaseMultimap;

    private final  List<TermId> hpoIdList;
    // List of excluded HPO terms in the subject.
    private final List<TermId> negatedHpoIdList;
    /** Various metadata that will be used for the HTML org.monarchinitiative.lirical.output. */
    private final Map<String,String> metadata;


    private HpoCase hpocase=null;
    /** Rank of simulated disease */
    private int rank_of_disease;
    /** Rank of simulated disease gene (best rank of any disease associated with the correct disease gene). */
    private int rank_of_gene;
    /** The posttest probability of the simulated disease. */
    private double posttest_probability;
    /** If true, replace the HPO terms with random terms (both the observed and the included). */
    private final boolean randomize;
    /** A list of all HPO term ids in the Phenotypic abnormality subontology. */
    private final ImmutableList<TermId> phenotypeterms;


    /**
     *
     * @param phenopacket A GA4GH Phenopacket with information about a case
     * @param vcfpath Path to a template VCF file we will add a mutation to
     * @param factory {@link LiricalFactory} object
     * @param rand if true, randomize the HPO terms in the phenopacket
     */
    public PhenoGenoCaseSimulator(File phenopacket, String vcfpath, LiricalFactory factory, boolean rand) {
        phenopacketFile = phenopacket;
        templateVcfPath = vcfpath;
        this.metadata = new HashMap<>();
        this.factory = factory;
        this.genomeAssembly = factory.getAssembly();
        String phenopacketAbsolutePath = phenopacketFile.getAbsolutePath();
        PhenopacketImporter importer = PhenopacketImporter.fromJson(phenopacketAbsolutePath,this.factory.hpoOntology());
        this.sampleName = importer.getSamplename();
        simulatedDiagnosis = importer.getDiagnosis();
        String disId = simulatedDiagnosis.getTerm().getId(); // should be an ID such as OMIM:600102
        this.simulatedDiseaseId = TermId.of(disId);

        this.randomize = rand;
        if (randomize) {
            Set<TermId> descendents=getDescendents(factory.hpoOntology(),PHENOTYPIC_ABNORMALITY);
            ImmutableList.Builder<TermId> termbuilder = new ImmutableList.Builder<>();
            for (TermId t: descendents) {
                termbuilder.add(t);
            }
            this.phenotypeterms=termbuilder.build();
            ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
            for (int i=0;i<importer.getHpoTerms().size();i++) {
                builder.add(getRandomPhenotypeTerm());
            }
            this.hpoIdList = builder.build();
            builder = new ImmutableList.Builder<>();
            for (int i=0;i<importer.getNegatedHpoTerms().size();i++) {
                builder.add(getRandomPhenotypeTerm());
            }
            negatedHpoIdList = builder.build();
        } else {
            this.phenotypeterms = ImmutableList.of(); // not needed
            hpoIdList = importer.getHpoTerms();
            negatedHpoIdList = importer.getNegatedHpoTerms();
        }

        VcfSimulator vcfSimulator = new VcfSimulator(Paths.get(this.templateVcfPath));
        HtsFile simulatedVcf;
        try {
            simulatedVcf = vcfSimulator.simulateVcf(importer.getSamplename(), importer.getVariantList(), genomeAssembly.toString());
            //pp = pp.toBuilder().clearHtsFiles().addHtsFiles(htsFile).build();
        } catch (IOException e) {
            throw new LiricalRuntimeException("Could not simulate VCF for phenopacket");
        }
        String vcfPath = simulatedVcf.getUri();//File().getPath();
        this.metadata.put("vcf_file", vcfPath);
        this.genotypemap = factory.getGene2GenotypeMap(vcfPath);
        this.ontology = factory.hpoOntology();
        this.diseaseMap = factory.diseaseMap(ontology);
        this.disease2geneMultimap = factory.disease2geneMultimap();
        this.gene2diseaseMultimap = factory.gene2diseaseMultimap();




        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        this.metadata.put("analysis_date", dateFormat.format(date));
        this.metadata.put("phenopacket_file", phenopacketAbsolutePath);
        metadata.put("sample_name", this.sampleName);
        if (!importer.qcPhenopacket() ){
            System.err.println("[ERROR] Could not simulate VCF for "+phenopacketFile.getName());
            return;
        }
        this.simulatedDiseaseGene = TermId.of(importer.getGene());
        logger.trace("Running simulation from phenopacket {} with template VCF {}",
                phenopacketFile.getAbsolutePath(),
                vcfPath);


       this.metadata.put("phenopacket.diagnosisId", simulatedDiseaseId.getValue());
       this.metadata.put("phenopacket.diagnosisLabel", simulatedDiagnosis.getTerm().getLabel());
    }

    /**
     * This is a term that was observed in the simulated patient (note that it should not be a HpoTermId, which
     * contains metadata about the term in a disease entity, such as overall frequency. Instead, we are simulating an
     * individual patient and this is a definite observation.
     * @return a random term from the phenotype subontology.
     */
    private TermId getRandomPhenotypeTerm() {
        int n=phenotypeterms.size();
        int r = (int)Math.floor(n*Math.random());
        return phenotypeterms.get(r);
    }


    /**
     * This method coordinates
     */
    public void run()  {


        GenotypeLikelihoodRatio genoLr = factory.getGenotypeLR();
        PhenotypeLikelihoodRatio phenoLr =  new PhenotypeLikelihoodRatio(ontology, diseaseMap);


        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(hpoIdList)
                .ontology(factory.hpoOntology())
                .negated(negatedHpoIdList)
                .diseaseMap(diseaseMap)
                .disease2geneMultimap(disease2geneMultimap)
                .genotypeMap(genotypemap)
                .phenotypeLr(phenoLr)
                .gene2idMap(factory.geneId2symbolMap())
                .global(factory.global())
                .genotypeLr(genoLr);

        CaseEvaluator evaluator = caseBuilder.build();
        this.hpocase = evaluator.evaluate();

        Optional<Integer> optRank = this.hpocase.getRank(simulatedDiseaseId);
        this.rank_of_disease = optRank.orElseGet(() -> this.hpocase.getRankOfUnrankedDisease());
        this.rank_of_gene = this.rank_of_disease;
        this.posttest_probability = this.hpocase.getPosttestProbability(simulatedDiseaseId);
        for (TermId diseaseId : this.gene2diseaseMultimap.get(this.simulatedDiseaseGene)) {
            optRank = this.hpocase.getRank(diseaseId);
            int r = optRank.orElseGet(() -> this.hpocase.getRankOfUnrankedDisease());
            if (r < rank_of_gene) {
                rank_of_gene = r;
            }
        }


        this.metadata.put("genesWithVar", String.valueOf(genotypemap.size()));
        this.metadata.put("exomiserPath", factory.getExomiserPath());
        this.metadata.put("hpoVersion", factory.getHpoVersion());

    }


    public void outputHtml(String prefix, double lrThreshold,int minDiff, String outdir) {
        LiricalTemplate.Builder builder = new LiricalTemplate.Builder(hpocase,ontology,metadata)
                .genotypeMap(genotypemap)
                .geneid2symMap(factory.geneId2symbolMap())
                .threshold(lrThreshold)
                .mindiff(minDiff)
                .outdirectory(outdir)
                .prefix(prefix);
        HtmlTemplate htemplate = builder.buildGenoPhenoHtmlTemplate();
        htemplate.outputFile();
    }


    public void outputTsv(String prefix, double lrThreshold,int minDiff, String outdir) {
        String outname=String.format("%s.tsv",prefix);
        LiricalTemplate.Builder builder = new LiricalTemplate.Builder(this.hpocase,ontology,metadata)
                .genotypeMap(genotypemap)
                .geneid2symMap(factory.geneId2symbolMap())
                .threshold(lrThreshold)
                .mindiff(minDiff)
                .outdirectory(outdir)
                .prefix(prefix);
        TsvTemplate tsvtemplate = builder.buildGenoPhenoTsvTemplate();
        tsvtemplate.outputFile(outname);

    }


    public String getDiagnosisLabel() {
        return this.simulatedDiagnosis.getTerm().getLabel();
    }

    public int getRank_of_disease() {
        return rank_of_disease;
    }

    public int getRank_of_gene() {
        return rank_of_gene;
    }

    public double getPosttestProbabilityOfSimulatedDisease() { return this.posttest_probability; }


    public static String getHeader() {
        String [] fields = {"Phenopacket", "Diagnosis", "Diagnosis-ID", "Gene", "Disease-Rank", "Gene-Rank", "Posttest-prob"};
        return String.join("\t",fields);

    }

    public String getDetails() {
        String simulatedGene;
        if (simulatedDiseaseGene==null)
            simulatedGene = "n/a";
        else
            simulatedGene = simulatedDiseaseGene.getValue();
        return String.format("%s\t%s\t%s\t%s\t%d\t%d\t%f", phenopacketFile.getName(),
                simulatedDiagnosis.getTerm().getLabel(),
                simulatedDiagnosis.getTerm().getId(),
                simulatedGene,
                rank_of_disease,
                rank_of_gene,
                posttest_probability);
    }



}
