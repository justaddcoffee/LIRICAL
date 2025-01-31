package org.monarchinitiative.lirical.gt2git;

import org.monarchinitiative.exomiser.core.model.frequency.FrequencySource;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class is meant to hold all of the data on a population background for the GNOMAD/ExAC datasets
 * (e.g., one object of this class might hold data for GNOMAD_E_FIN).
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
class Background {
    /** An object that represents the source of frequency data (a population background, e.g., GNOMAD_E_FIN). */
    private final FrequencySource frequencySource;
    /** key: A gene symbol; value: corresponding {@link org.monarchinitiative.lirical.gt2git.Gene2Bin} object. */
    private final Map<String, Gene2Bin> background2binMap = new HashMap<>();

    Background(FrequencySource fSource){
        this.frequencySource=fSource;
    }


    public FrequencySource getFrequencySource() {
        return frequencySource;
    }

    Map<String, Gene2Bin> getBackground2binMap() {
        return background2binMap;
    }
    /** @return an optional containing the {@link org.monarchinitiative.lirical.gt2git.Gene2Bin} object for the gene symbol.*/
    Optional<Gene2Bin> getGene2Bin(String symbol) {
        if (! background2binMap.containsKey(symbol)) return Optional.empty();
        else return Optional.of(background2binMap.get(symbol));
    }
}
