package testminer.tfidf;

import testminer.tokenizer.Tokenizer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TfIdf {

    Map<String, Double> values = new HashMap<>();
    long numberOfDocuments = Long.MIN_VALUE;

    TfIdf() { }

    public static TfIdf create(final Set<String> dataSet) {

        final TfIdf tfIdf = new TfIdf();

        final Map<String, Long> occurrences = new HashMap<>();
        for (final String signature : dataSet) {
            for (final String token: Tokenizer.tokenize(signature).keySet()) {
                if (occurrences.containsKey(token)) {
                    final long value = occurrences.get(token);
                    occurrences.put(token, value + 1L);
                } else {
                    occurrences.put(token, 1L);
                }
            }
        }

        tfIdf.numberOfDocuments = dataSet.size();

        for (final Map.Entry<String, Long> entry : occurrences.entrySet()) {
            tfIdf.values.put(entry.getKey(), tfIdf.tfIdf(entry.getValue()));
        }

        return tfIdf;

    }

    public double value(final String token) {
        if (this.values.containsKey(token)) {
            return this.values.get(token);
        }
        throw new RuntimeException("Token \"" + token + "\" not indexed");
    }

    public boolean isIndexed(final String token) {
        return this.values.containsKey(token);
    }

    public double tfIdf(final long tokenCount) {
        return Math.log( (double) this.numberOfDocuments / (double) (tokenCount + 1L));
    }

    public static double indexWeight(final double value, final long tokenCount) {
        return ((double) tokenCount) * value;
    }

    public static double queryWeight(final double value, final long tokenCount, final long maxFrequency) {
        return 0.5 + 0.5 * (((double) tokenCount) / ((double) maxFrequency)) * value;
    }

}
