package org.monarchinitiative.lr2pg.hpo;


import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.io.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.io.obo.hpo.HpoOboParser;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermId;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermPrefix;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test whether we can successfully create HpoCase objects.
 */
public class HpoCaseTest {
    private static final Logger logger = LogManager.getLogger();
    /** Name of the disease we are simulating in this test, i.e., OMIM:108500. */
    private static String diseasename="108500";
    private static HpoCase hpocase;
    private static HpoOntology ontology;
    private static BackgroundForegroundTermFrequency backforeFreq;


    @BeforeClass
    public static void setup() throws IOException,PhenolException {
        ClassLoader classLoader = BackgroundForegroundTermFrequencyTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        String annotationPath = classLoader.getResource("small.hpoa").getFile();
        /* parse ontology */
        HpoOboParser parser = new HpoOboParser(new File(hpoPath));
        ontology =parser.parse();
        HpoDiseaseAnnotationParser annotationParser=new HpoDiseaseAnnotationParser(annotationPath,ontology);
        Map<String,HpoDisease> diseaseMap=annotationParser.parse();
        backforeFreq=new BackgroundForegroundTermFrequency(ontology,diseaseMap);

        /* these are the phenotypic abnormalties of our "case" */
        TermPrefix HP_PREFIX=new ImmutableTermPrefix("HP");
        TermId t1 = new ImmutableTermId(HP_PREFIX,"0006855");
        TermId t2 = new ImmutableTermId(HP_PREFIX,"0000651");
        TermId t3 = new ImmutableTermId(HP_PREFIX,"0010545");
        TermId t4 = new ImmutableTermId(HP_PREFIX,"0001260");
        TermId t5 = new ImmutableTermId(HP_PREFIX,"0001332");
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        builder.add(t1,t2,t3,t4,t5);

        hpocase = new HpoCase(ontology,backforeFreq,diseasename,builder.build());
    }


    @Test
    public void testNotNull() {
        assertNotNull(hpocase);
    }

    /**
     * Our test case has
     * OMIM:108500
     HP:0006855
     HP:0000651
     HP:0010545
     HP:0001260
     Thus there are five Hpo annotations.
     */
    @Test
    public void testNumberOfAnnotations() {
        int expected=5;
        assertEquals(expected,hpocase.getNumberOfAnnotations());
    }



    @Test
    public void testAnotherCase() throws Lr2pgException{
        /* these are the phenpotypic abnormalties of our "case" */
        TermId t1 = ImmutableTermId.constructWithPrefix("HP:0000750");
        TermId t2 = ImmutableTermId.constructWithPrefix("HP:0001258");
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        builder.add(t1,t2);
        HpoCase case1 = new HpoCase(ontology,backforeFreq,diseasename,builder.build());
        assertNotNull(case1);
        int expected=2;
        assertEquals(expected,case1.getNumberOfAnnotations());
    }


    @Test
    public void testKniestDysplasia() {
        ImmutableList<TermId> lst = ImmutableList.of( ImmutableTermId.constructWithPrefix("HP:0410009"),
                ImmutableTermId.constructWithPrefix( "HP:0002812"),
                ImmutableTermId.constructWithPrefix("HP:0003521"),
                ImmutableTermId.constructWithPrefix("HP:0000541"),
                ImmutableTermId.constructWithPrefix("HP:0011800"),
                ImmutableTermId.constructWithPrefix("HP:0003015"),
                ImmutableTermId.constructWithPrefix("HP:0008271"));
        String kniestDysplasia = "OMIM:156550";
        HpoCase kniestCase = new HpoCase(ontology, backforeFreq, kniestDysplasia, lst);
       //kniestCase.debugPrint();
        int expected = 7;
        assertEquals(expected, kniestCase.getNumberOfAnnotations());
    }



}
