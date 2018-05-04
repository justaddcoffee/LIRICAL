package org.monarchinitiative.lr2pg.likelihoodratio;

import com.google.common.collect.ImmutableList;
import org.monarchinitiative.lr2pg.hpo.HpoCaseOld;

import java.util.stream.Collectors;

/**
 * This class organizes information about the result of a test. For instance,  a GDx VCC test for
 * glaucoma may result in a measurement of 48, a value that is known to
 * have a 60% sensitivity and 97% specificity for glaucoma. In general, we need to have the sensitivity and the
 * specificity of a test result in order to perform a likelihood ratio test. The numerical value of the test
 * (in this case, 48) is not important.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @author <a href="mailto:vida.ravanmehr@jax.org">Vida Ravanmehr</a>
 * @version 0.2.2 (2017-12-15)
 */
public class TestResult implements Comparable<TestResult> {

    /**
     * A list of results for the tests performed in a {@link HpoCaseOld} case.
     * To save space, we only record the result of the test, and we assume that the order of the test is the same
     * as indicated in the case object. In the intended use case, there will be one test result for each disease
     * that is tested for the {@link HpoCaseOld} object.
     */
    private ImmutableList<Double> results;
    /** TODO document me! */
    private double compositeLR;
    /** The name of the disease that we are testing for this time. */
    private final String diseasename;


    public TestResult(ImmutableList<Double> reslist, String name) {
        results = reslist;
        diseasename = name;
        calculateCompositeLikelihoodRatio();
    }

    /** @return the composite likelihood ratio (product of the LRs of the individual tests). */
    public double getCompositeLR() {
        return compositeLR;
    }
    /** @return the total count of tests performed. */
    public int getNumberOfTests() { return results.size();  }

    /**
     * TODO Should this be a class variable?
     * @param pretestProb
     * @return
     */
    public double pretestodds(double pretestProb) {return pretestProb/(1-pretestProb);}

    /**
     * TODO Should this be a class variable?
     * @param pretestProb
     * @return
     */
    public double posttestodds(double pretestProb) {
        double pretestodds=pretestodds(pretestProb);
        return pretestodds * getCompositeLR();
    }


    /**
     * TODO what if specificity and/or sensitivity is 100%?
     *
     * @return
     */
//    public double PositivelikelihoodRatio() {
//        double LR = 0;
//        //Put a threshold |sensitivity|<\epsilon
//        if (sensitivity == 0)
//            return 0;
//        try {
//            LR = sensitivity / (1 - specificity);
//            return LR;
//        } catch (ArithmeticException e) {
//            System.err.println(e);
//            return 0;
//        } catch (Exception e) {
//            System.err.println(e);
//            return 0;
//        }
//    }

    public void calculateCompositeLikelihoodRatio() {
        compositeLR = 1.0;
        for (Double tres : results) {
            compositeLR *= tres;
        }
    }

    /**
     * TODO what if pretest prob is 100% ?
     *
     * @return
     */
//    public double getPretestOdds() {
//        return pretestProbability / (1 - pretestProbability);
//    }


//    public double getPosttestProbability(double pretestpb) {
//        double po=getPosttestOdds();
//        return po/(1+po);
//
//    }


//    public double getPosttestOdds() {
//        return getCompositeLikelihoodRatio() * getPretestOdds();
//    }

//    /**
//     * TODO revise this!
//     * @return
//     */
//    public double NegativelikelihoodRatio() {
//
////        double LR = 0;
////        //Put a threshold, if |sensitivity-1|<\epsilon
////        if (sensitivity == 1)
////            return 0;
////        try {
////            LR = specificity / (1 - sensitivity);
////            return LR;
////        } catch (ArithmeticException e) {
////            System.err.println(e);
////            return 0;
////        } catch (Exception e) {
////            System.err.println(e);
////            return 0;
////        }
//
//
//    }
    @Override
    public int compareTo(TestResult other) {
        if (compositeLR>other.compositeLR) return 1;
        else if (other.compositeLR>compositeLR) return -1;
        else return 0;
    }


    @Override
    public String toString() {
        String resultlist=results.stream().map(r -> String.valueOf(r)).collect(Collectors.joining(";"));
        return String.format("%s: %.2f [%s]",diseasename,compositeLR,resultlist);
    }


    public double getRatio(int i) {
        return this.results.get(i);
    }

    public String getDiseasename() {
        return diseasename;
    }
}
