package testminer;

import com.google.gson.stream.JsonReader;
import testminer.simhash.FingerPrint;
import testminer.simhash.FingerPrintIndex;
import testminer.tfidf.TfIdf;
import testminer.tokenizer.Tokenizer;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class TestMiner {

    public static final int TOLERANCE = 16;
    public static final int SIZE = 64;

    FingerPrintIndex<String> index;
    TfIdf tfIdf;
    Map<String, Map<String, Double>> tuples;
    Map<String, Map<String, Long>> values;      

    protected void indexTuples() {
        for (final Map.Entry<String, Map<String, Double>> tuple : this.tuples.entrySet()) {
            final String signature = tuple.getKey();
            final Map<String, Double> tokens = tuple.getValue();
            final FingerPrint fp = FingerPrint.create(SIZE, tokens);
            this.index.index(signature, fp);
        }
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        Collections.sort( list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public Map<String, Double> query(final String signature) {

        final Map<String, Integer> defaultTokens = Tokenizer.tokenize(signature);
        final long maxFrequency = Collections.max(defaultTokens.values());

        final Map<String, Double> tokens = new HashMap<>();
        for (final Map.Entry<String, Integer> entry : defaultTokens.entrySet()) {
            final String token = entry.getKey();
            final long tokenCount = entry.getValue();
            if (tfIdf.isIndexed(token)) {
                final double tokenValue = tfIdf.value(token);
                final double tokenWeight = TfIdf.queryWeight(tokenValue, tokenCount, maxFrequency);
                tokens.put(token, tokenWeight);
            }
        }
        normalizeWeights(tokens);

        final FingerPrint fp = FingerPrint.create(SIZE, tokens);

        final Set<String> duplicates = index.duplicates(fp);

        final Map<String, Long> values = new HashMap<>();
        for (final String duplicate : duplicates) {
            final Map<String, Long> duplicateValues = this.values.get(duplicate);
            for (final Map.Entry<String, Long> entry : duplicateValues.entrySet()) {
                long valueCount = entry.getValue();
                if (values.containsKey(entry.getKey())) {
                    valueCount += values.get(entry.getKey());
                }
                values.put(entry.getKey(), valueCount);
            }
        }

        long sumOfCounts = 0L;
        for (final long tokenWeight : values.values()) {
            sumOfCounts += tokenWeight;
        }

        final Map<String, Double> valuesToDouble = new HashMap<>();
        for (final Map.Entry<String, Long> entry : values.entrySet()) {
            valuesToDouble.put(entry.getKey(), entry.getValue().doubleValue() / (double) sumOfCounts);
        }

        return sortByValue(valuesToDouble);
    }

    static public void normalizeWeights(final Map<String, Double> tokens) {
        double sumOfWeights = 0.0;
        for (final double tokenWeight : tokens.values()) {
            sumOfWeights += tokenWeight;
        }
        for (final String token : tokens.keySet()) {
            final double weight = tokens.get(token) * 100.0 / sumOfWeights;
            tokens.put(token, weight);
        }
    }
    
    public static InputStreamReader getTuplesStreamReader() {    	
    	return new InputStreamReader(ClassLoader.getSystemClassLoader().getResourceAsStream("full.json"));
    }

    public static TestMiner create(final InputStreamReader stream) {

        final Map<String, Map<String, Long>> values = new HashMap<>();

        JsonReader jr = null;
        try {
            jr = new JsonReader(stream);
            jr.beginObject();
            while (jr.hasNext()) {

                final String signature = jr.nextName();
                jr.beginObject();
                jr.nextName();
                jr.beginObject();
                while (jr.hasNext()) {
                    jr.nextName();
                    jr.nextDouble();
                }
                jr.endObject();

                final Map<String, Long> signatureValues = new HashMap<>();
                jr.nextName();
                jr.beginObject();
                while (jr.hasNext()) {
                    final String value = jr.nextName();
                    final long count = jr.nextLong();
                    signatureValues.put(value, count);
                }
                jr.endObject();
                jr.endObject();

                values.put(signature, signatureValues);

            }
            jr.endObject();
            jr.close();

        } catch (final Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("[TestMiner] Error parsing the dataset");
        } finally {
            jr = null;
        }

        final Set<String> dataSet = values.keySet();

        final TestMiner tm = new TestMiner();

        tm.tfIdf = TfIdf.create(dataSet);
        tm.index = FingerPrintIndex.create(TOLERANCE, SIZE);
        tm.tuples = new HashMap<>();
        tm.values = values;

        for (final String signature : dataSet) {

            final Map<String, Double> tokens = new HashMap<>();

            for (final Map.Entry<String, Integer> entry : Tokenizer.tokenize(signature).entrySet()) {
                final String token = entry.getKey();
                final long tokenCount = entry.getValue();
                final double tokenValue = tm.tfIdf.value(token);
                final double tokenWeight = TfIdf.indexWeight(tokenValue, tokenCount);
                tokens.put(token, tokenWeight);
            }

            normalizeWeights(tokens);

            tm.tuples.put(signature, tokens);
        }

        tm.indexTuples();

        return tm;

    }


}
