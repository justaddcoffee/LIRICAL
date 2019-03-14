package org.monarchinitiative.lr2pg.likelihoodratio;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class GenotypeLikelihoodRatioTest {

    private static final double EPSILON=0.0001;

    private static final TermId autosomalDominant = TermId.of("HP:0000006");
    private static final TermId autosomalRecessive = TermId.of("HP:0000007");



    /**
     * If we find one variant that is listed as pathogenic in ClinVar, then we return the genotype
     * likelihood ratio of 1000 to 1.
     */
    @Test
    void testOneClinVarVariant() {
        Gene2Genotype g2g = mock(Gene2Genotype.class);

        when(g2g.hasPathogenicClinvarVar()).thenReturn(true);
        when(g2g.pathogenicClinVarCount()).thenReturn(1);
        Map<TermId,Double> emptyMap = ImmutableMap.of();
        GenotypeLikelihoodRatio genoLRmap = new GenotypeLikelihoodRatio(emptyMap);
        TermId fakeGeneId = TermId.of("Fake:123");
        List<TermId> emptyList= ImmutableList.of();
        Double lrOption = genoLRmap.evaluateGenotype( g2g, emptyList, fakeGeneId);
        double result = lrOption;
        double expected = (double)1000;
        assertEquals(expected,result,EPSILON);
    }


    /**
     * If we find two variants listed as pathogenic in ClinVar, then we return the genotype
     * likelihood ratio of 1000*1000 to 1.
     */
    @Test
    void testTwoClinVarVariants() {
        Gene2Genotype g2g = mock(Gene2Genotype.class);
        when(g2g.hasPathogenicClinvarVar()).thenReturn(true);
        when(g2g.pathogenicClinVarCount()).thenReturn(2);
        Map<TermId,Double> emptyMap = ImmutableMap.of();
        GenotypeLikelihoodRatio genoLRmap = new GenotypeLikelihoodRatio(emptyMap);
        TermId fakeGeneId = TermId.of("Fake:123");
        List<TermId> emptyList= ImmutableList.of();
        Double score = genoLRmap.evaluateGenotype( g2g, emptyList, fakeGeneId);
        double result = score;
        double expected = (double)1000*1000;
        assertEquals(expected,result,EPSILON);
    }


    /**
     * We want to test what happens with a gene that has lots of variants but a pathogenic variant count sum of zero,
     * a lambda-disease of 1, and a lambda-background of 8.7. This numbers are taken from the HLA-B gene.
     */
    @Test
    void testHLA_Bsituation() {
        // create a background map with just one gene for testing
        Map <TermId,Double> g2background = new HashMap<>();
        TermId HLAB = TermId.of("NCBIGene:3106");
        g2background.put(HLAB,8.7418); // very high lambda-background for HLAB
        GenotypeLikelihoodRatio glr = new GenotypeLikelihoodRatio(g2background);
        List<TermId> inheritanceModes = new ArrayList<>();
        inheritanceModes.add(autosomalDominant);
        Gene2Genotype g2g = mock(Gene2Genotype.class);
        when(g2g.getSumOfPathBinScores()).thenReturn(0.00); // mock that we find no pathogenic variant
        Double score = glr.evaluateGenotype(g2g,inheritanceModes,HLAB);
        double expected = 0.05; // heuristic score
        assertEquals(expected,score,EPSILON);
    }

    /**
     * We want to test what happens with a gene that has lots of variants but a pathogenic variant count sum of zero,
     * a lambda-disease of 2, and a lambda-background of 8.7. This numbers are taken from a made-up  gene.
     */
    @Test
    void testRecessiveManyCalledPathVariants() {
        // create a background map with just one gene for testing
        Map <TermId,Double> g2background = new HashMap<>();
        TermId madeUpGene = TermId.of("NCBIGene:42");
        g2background.put(madeUpGene,8.7418); // very high lambda-background for TTN
        GenotypeLikelihoodRatio glr = new GenotypeLikelihoodRatio(g2background);
        List<TermId> inheritanceModes = new ArrayList<>();
        inheritanceModes.add(autosomalRecessive);
        Gene2Genotype g2g = mock(Gene2Genotype.class);
        when(g2g.getSumOfPathBinScores()).thenReturn(0.00); // mock that we find no pathogenic variant
        Double score = glr.evaluateGenotype(g2g,inheritanceModes,madeUpGene);
        double expected = 0.05*0.05; // heuristic score for AR
        assertEquals(expected,score,EPSILON);
    }
}
