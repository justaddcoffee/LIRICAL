package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.*;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermId;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermPrefix;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;
import com.google.common.collect.ImmutableList;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.io.HpoAnnotation2DiseaseParser;
import org.monarchinitiative.lr2pg.io.HpoOntologyParser;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by ravanv on 1/19/18.
 */
//In this class, we test the frequency of terms in a disease and background frequencies and finally we calculate the likelihood ratio.
// The disease is:  ADIE PUPIL 103100 and there are 3 terms for this disease, where one of them is 0000006  Autosomal dominant inheritance. The two other terms
// HP:0001265 and HP:0012074 are phenotype terms.
public class CalculatingLikelihoodRatio {
    private static TermPrefix HP_PREFIX = null;

    private static Disease2TermFrequency d2tf = null;

    private static final double EPSILON = 0.000001;

    private static HpoCase hpocase;

    private static String diseaseName = "103100";

    @BeforeClass
    public static void setup() throws IOException {
        HP_PREFIX = new ImmutableTermPrefix("HP");
        ClassLoader classLoader = Disease2TermFrequencyTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        String annotationPath = classLoader.getResource("small_phenoannot.tab").getFile();
        HpoOntologyParser parser = new HpoOntologyParser(hpoPath);
        parser.parseOntology();
        Ontology<HpoTerm, HpoTermRelation> phenotypeSubOntology = parser.getPhenotypeSubontology();
        Ontology<HpoTerm, HpoTermRelation> inheritanceSubontology = parser.getInheritanceSubontology();
        HpoAnnotation2DiseaseParser annotationParser = new HpoAnnotation2DiseaseParser(annotationPath, phenotypeSubOntology, inheritanceSubontology);
        Map<String, HpoDiseaseWithMetadata> diseaseMap = annotationParser.getDiseaseMap();
        String DEFAULT_FREQUENCY = "0040280";
        d2tf = new Disease2TermFrequency(phenotypeSubOntology, inheritanceSubontology, diseaseMap);
        TermPrefix HP_PREFIX = new ImmutableTermPrefix("HP");
        //ImmutableTermIdWithMetadata t1 = new ImmutableTermIdWithMetadata(new ImmutableTermId(HP_PREFIX, "0000006"));
        ImmutableTermIdWithMetadata t2 = new ImmutableTermIdWithMetadata(new ImmutableTermId(HP_PREFIX, "0001265"));
        ImmutableTermIdWithMetadata t3 = new ImmutableTermIdWithMetadata(new ImmutableTermId(HP_PREFIX, "0012074"));
        ImmutableList.Builder<TermIdWithMetadata> builder = new ImmutableList.Builder<>();
        builder.add(t2,t3);
        hpocase = new HpoCase(phenotypeSubOntology,d2tf,diseaseName,builder.build());
    }

    /**
     * The term HP:0001265 is a phenotype term in the disease 103100. The frequency of term in disease is 1.
     */
    @Test
    public void testGetFrequencyOfTermInDieases1_1()throws Lr2pgException {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0001265");
        double expected =1.0;
        assertEquals(expected,d2tf.getFrequencyOfTermInDisease(diseaseName,tid),EPSILON);
    }

    /**
     * The term HP:0012074 is a phenotype term in the disease 103100. The frequency of term in disease is 1.
     */
    @Test
    public void testGetFrequencyOfTermInDieases1_2()throws Lr2pgException {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0012074");
        double expected =1.0;
        assertEquals(expected,d2tf.getFrequencyOfTermInDisease(diseaseName,tid),EPSILON);
    }

    /**
     * The term HP:0012074 is a phenotype term in the disease 103100. The frequency of term in disease is 1.
     */
    @Test
    public void testGetFrequencyOfTermInDieases1_3()throws Lr2pgException {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0012074");
        double expected =1.0;
        assertEquals(expected,d2tf.getFrequencyOfTermInDisease(diseaseName,tid),EPSILON);
    }

    /**
     * The term HP:0000006 is an inheritance phenotype term in the disease 103100.
     */
    @Test
    public void testGetFrequencyOfTermInDieases1_4() throws Lr2pgException{
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0000006"); //Autosomal dominant inheritance,
        double expected =0.0;
        assertEquals(expected,d2tf.getFrequencyOfTermInDisease(diseaseName,tid),EPSILON);
    }

    //There is 1 appearance of HP:0001265 in small_phenoannot.tab. The frequency of this term is 1/196.
    @Test
    public void testGetBackgroundFrequency_1()throws Lr2pgException {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0012074");
        double expected = (double)1/196;
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }

    //There are 2 appearances of HP:0001265 in small_phenoannot.tab. The frequency of this term is 2/196.
    @Test
    public void testGetBackgroundFrequency_2() throws Lr2pgException{
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0001265");
        double expected = (double)2/196;
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }


   //Likelihood ratio for this term is calculated as 1/ (2/196) = 98.
    @Test
    public void testgetLikelihoodRatio_1()throws Lr2pgException {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0001265");
        double expected = (double)196/2;
        assertEquals(expected,d2tf.getLikelihoodRatio(tid,diseaseName),EPSILON);
    }

    //Likelihood ratio for this term is calculated as 1/ (1/196) = 196.
    @Test
    public void testgetLikelihoodRatio_2() throws Lr2pgException{
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0012074");
        double expected = (double)196;
        assertEquals(expected,d2tf.getLikelihoodRatio(tid,diseaseName),EPSILON);
    }
     //Test the likelihood ratio of the phenotype terms HP:0001265 and HP:0012074. The likelihood ratio of terms are 98 and 196, respectively.
    //So, the likelihood ratio will be the product of the terms which is 196*98 = 19208
    //Note that inheritance terms are not considered, only phenotype terms!
    @Test
    public void testPipeline()throws Lr2pgException {
        hpocase.calculateLikelihoodRatios();
        int expected=1;
        hpocase.outputResults();
        int actual=hpocase.getRank(diseaseName);
        assertEquals(expected ,actual);
    }


}