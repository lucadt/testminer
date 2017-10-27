package testminer.tokenizer;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class Tokenizer {

    public static final Map<String, Integer> tokenize(final String inputString) {

        String[] splitWithPar = inputString.split("\\(");

        final String newInputString = splitWithPar[0];

        String[] split = newInputString.split("[0-9]|\\<|\\>|\\$|\\.|,|:|\\(|\\)|\\]|\\[");
        final Map<String, Integer> result = new HashMap<>();
        for (final String aSplit : split) {
            final String[] tokens = tokenizeIndividual(aSplit);
            for (final String t : tokens) {
                if (t.length() > 0) {
                    if (result.containsKey(t)) {
                        result.put(t, result.get(t) + 1);
                    } else {
                        result.put(t, 1);
                    }
                }
            }
        }
        return result;
    }

    public static String[] tokenizeIndividual(final String inputString) {

        if (inputString == null) {
            return new String[0];
        }

        String[] result = null;
        if (inputString.contains("_")) {
            result = inputString.split("_");
        } else {
            result = StringUtils.splitByCharacterTypeCamelCase(inputString);
        }

        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].trim().toLowerCase();
        }

        return result;

    }

}
