package testminer.simhash;

import java.math.BigInteger;
import java.util.Map;
import java.security.*;

public class FingerPrint {

    public int size = 64;
    public BigInteger value = BigInteger.ZERO;

    public static FingerPrint create(final int size, final BigInteger value) {
        final FingerPrint fp = new FingerPrint();
        fp.size = size;
        fp.value = value;
        return fp;
    }

    public static FingerPrint create(final int size, final Map<String, Double> features) {

        final BigInteger[] masks = new BigInteger[size];
        for (int j = 0; j < masks.length; j++) {
            masks[j] = BigInteger.valueOf(1L << j);
        }

        final double[] values = new double[size];
        for (final String feature : features.keySet()) {

            final double weight = features.get(feature);

            MessageDigest m = null;
            byte[] digest = null;
            try {
                m = MessageDigest.getInstance("MD5");
                m.reset();
                m.update(feature.getBytes());
                digest = m.digest();
            } catch (NoSuchAlgorithmException ex) {
                ex.printStackTrace();
            }

            final BigInteger hash = new BigInteger(1, digest);

            for (int j = 0; j < values.length; j++) {
                final int compare = masks[j].and(hash).compareTo(BigInteger.ZERO);
                if (compare > 0) {
                    values[j] += weight;
                } else {
                    values[j] -= weight;
                }
            }

        }

        BigInteger value = BigInteger.ZERO;
        for (int j = 0; j < values.length; j++) {
            if (values[j] > 0.0) {
                value = value.or(masks[j]);
            }
        }

        return create(size, value);

    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof FingerPrint) {
            final FingerPrint other = (FingerPrint) obj;
            return other.size == this.size && other.value.equals(this.value);
        }
        return false;
    }

    FingerPrint() {
    }

    long distance(final FingerPrint another) {
        final String thisString = this.value.toString(2);
        final String anotherString = another.value.toString(2);
        final String thisPadding = new String(new char[64 - thisString.length()]).replace("\0", "0");
        final String anotherPadding = new String(new char[64 - anotherString.length()]).replace("\0", "0");
        long distance = 0L;
        final String thisStringNew = thisPadding + thisString;
        final String anotherStringNew = anotherPadding + anotherString;
        for (int i = 0; i < thisStringNew.length(); i++) {
            if (anotherStringNew.charAt(i) != thisStringNew.charAt(i)) {
                distance++;
            }
        }
        return distance;
    }

}
