package testminer.simhash;

import java.math.BigInteger;
import java.util.*;

public class FingerPrintIndex<T> {

    private static class FingerPrintBucket {

        public String key;
        public FingerPrint fp;

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof FingerPrintBucket) {
                final FingerPrintBucket other = (FingerPrintBucket) obj;
                return other.key.equals(this.key) && other.fp.equals(this.fp);
            }
            return false;
        }

    }

    protected int size = 64;
    protected int tolerance = 0;
    protected Map<String, Set<FingerPrintBucket>> buckets = null;

    public static <T> FingerPrintIndex<T> create(int tolerance, int size) {
        final FingerPrintIndex<T> fpi = new FingerPrintIndex<T>();
        fpi.size = size;
        fpi.tolerance = tolerance;
        fpi.buckets = new HashMap<>();
        return fpi;
    }

    private FingerPrintIndex() {
    }

    protected List<Integer> offsets() {
        final List<Integer> temp = new ArrayList<>();
        for (int i = 0; i < this.tolerance + 1; i++) {
            int result = this.size / (this.tolerance + 1) * i;
            temp.add(result);
        }
        return temp;
    }

    protected List<String> keys(final FingerPrint fp) {
        assert (this.size == fp.size);
        final List<String> temp = new ArrayList<>();
        final List<Integer> offsets = this.offsets();
        for (int i = 0; i < offsets.size(); i++) {
            final int offset = offsets.get(i);
            BigInteger m = BigInteger.ZERO;
            if (i == offsets.size() - 1) {
                m = BigInteger.valueOf(2L).pow(this.size - offset).subtract(BigInteger.ONE);
            } else {
                m = BigInteger.valueOf(2L).pow(offsets.get(i + 1) - offset).subtract(BigInteger.ONE);
            }
            BigInteger index = fp.value.shiftRight(offset).and(m);
            final String key = String.format("%s:%d", index.toString(), i);
            temp.add(key);
        }
        return temp;
    }

    public Set<String> duplicates(final FingerPrint fp) {
        assert (this.size == fp.size);
        final Set<String> temp = new HashSet<>();
        for (final String key : this.keys(fp)) {
            if (this.buckets.containsKey(key)) {
                for (final FingerPrintBucket fpb : this.buckets.get(key)) {
                    final long distance = fp.distance(fpb.fp);
                    if (distance <= this.tolerance) {
                        temp.add(fpb.key);
                    }
                }
            }
        }
        return temp;
    }

    public void index(final String identifier, final FingerPrint fp) {
        for (final String key : this.keys(fp)) {
            final FingerPrintBucket fpb = new FingerPrintBucket();
            fpb.key = identifier;
            fpb.fp = fp;
            if (!this.buckets.containsKey(key)) {
                this.buckets.put(key, new HashSet<>());
            }
            this.buckets.get(key).add(fpb);
        }
    }

}
