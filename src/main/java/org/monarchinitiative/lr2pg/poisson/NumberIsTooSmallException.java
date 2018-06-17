package org.monarchinitiative.lr2pg.poisson;

public class NumberIsTooSmallException extends Exception {
    public NumberIsTooSmallException(double mc, double threshold) {
        super(String.format("%f exceeded %f",mc,threshold));
    }
}
