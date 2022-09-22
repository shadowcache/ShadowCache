//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.google.common.hash;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.hash.BloomFilterStrategies.LockFreeBitArray;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.SignedBytes;
import com.google.common.primitives.UnsignedBytes;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.RoundingMode;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import org.checkerframework.checker.nullness.qual.Nullable;

@Beta
public final class CustomizedBloomFilter<T> implements Predicate<T>, Serializable {
    private final LockFreeBitArray bits;
    private final int numHashFunctions;
    private final Funnel<? super T> funnel;
    private final BloomFilter.Strategy strategy;

    private CustomizedBloomFilter(LockFreeBitArray bits, int numHashFunctions, Funnel<? super T> funnel, BloomFilter.Strategy strategy) {
        Preconditions.checkArgument(numHashFunctions > 0, "numHashFunctions (%s) must be > 0", numHashFunctions);
        Preconditions.checkArgument(numHashFunctions <= 255, "numHashFunctions (%s) must be <= 255", numHashFunctions);
        this.bits = (LockFreeBitArray)Preconditions.checkNotNull(bits);
        this.numHashFunctions = numHashFunctions;
        this.funnel = (Funnel)Preconditions.checkNotNull(funnel);
        this.strategy = (BloomFilter.Strategy)Preconditions.checkNotNull(strategy);
    }

    public CustomizedBloomFilter<T> copy() {
        return new CustomizedBloomFilter(this.bits.copy(), this.numHashFunctions, this.funnel, this.strategy);
    }

    public boolean mightContain(T object) {
        return this.strategy.mightContain(object, this.funnel, this.numHashFunctions, this.bits);
    }

    /** @deprecated */
    @Deprecated
    public boolean apply(T input) {
        return this.mightContain(input);
    }

    @CanIgnoreReturnValue
    public boolean put(T object) {
        return this.strategy.put(object, this.funnel, this.numHashFunctions, this.bits);
    }

    public double expectedFpp() {
        return Math.pow((double)this.bits.bitCount() / (double)this.bitSize(), (double)this.numHashFunctions);
    }

    public long approximateElementCount() {
        long bitSize = this.bits.bitSize();
        long bitCount = this.bits.bitCount();
        double fractionOfBitsSet = (double)bitCount / (double)bitSize;
        return DoubleMath.roundToLong(-Math.log1p(-fractionOfBitsSet) * (double)bitSize / (double)this.numHashFunctions, RoundingMode.HALF_UP);
    }

    @VisibleForTesting
    long bitSize() {
        return this.bits.bitSize();
    }

    public boolean isCompatible(CustomizedBloomFilter<T> that) {
        Preconditions.checkNotNull(that);
        return this != that && this.numHashFunctions == that.numHashFunctions && this.bitSize() == that.bitSize() && this.strategy.equals(that.strategy) && this.funnel.equals(that.funnel);
    }

    public void putAll(CustomizedBloomFilter<T> that) {
        Preconditions.checkNotNull(that);
        Preconditions.checkArgument(this != that, "Cannot combine a BloomFilter with itself.");
        Preconditions.checkArgument(this.numHashFunctions == that.numHashFunctions, "BloomFilters must have the same number of hash functions (%s != %s)", this.numHashFunctions, that.numHashFunctions);
        Preconditions.checkArgument(this.bitSize() == that.bitSize(), "BloomFilters must have the same size underlying bit arrays (%s != %s)", this.bitSize(), that.bitSize());
        Preconditions.checkArgument(this.strategy.equals(that.strategy), "BloomFilters must have equal strategies (%s != %s)", this.strategy, that.strategy);
        Preconditions.checkArgument(this.funnel.equals(that.funnel), "BloomFilters must have equal funnels (%s != %s)", this.funnel, that.funnel);
        this.bits.putAll(that.bits);
    }

    public boolean equals(@Nullable Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof CustomizedBloomFilter)) {
            return false;
        } else {
            CustomizedBloomFilter<?> that = (CustomizedBloomFilter)object;
            return this.numHashFunctions == that.numHashFunctions && this.funnel.equals(that.funnel) && this.bits.equals(that.bits) && this.strategy.equals(that.strategy);
        }
    }

    public int hashCode() {
        return Objects.hashCode(new Object[]{this.numHashFunctions, this.funnel, this.strategy, this.bits});
    }

    public static <T> Collector<T, ?, CustomizedBloomFilter<T>> toBloomFilter(Funnel<? super T> funnel, long expectedInsertions) {
        return toBloomFilter(funnel, expectedInsertions, 0.03D);
    }

    public static <T> Collector<T, ?, CustomizedBloomFilter<T>> toBloomFilter(Funnel<? super T> funnel, long expectedInsertions, double fpp) {
        Preconditions.checkNotNull(funnel);
        Preconditions.checkArgument(expectedInsertions >= 0L, "Expected insertions (%s) must be >= 0", expectedInsertions);
        Preconditions.checkArgument(fpp > 0.0D, "False positive probability (%s) must be > 0.0", fpp);
        Preconditions.checkArgument(fpp < 1.0D, "False positive probability (%s) must be < 1.0", fpp);
        return Collector.of(() -> {
            return create(funnel, expectedInsertions, fpp);
        }, CustomizedBloomFilter::put, (bf1, bf2) -> {
            bf1.putAll(bf2);
            return bf1;
        }, Characteristics.UNORDERED, Characteristics.CONCURRENT);
    }

    public static <T> CustomizedBloomFilter<T> create(Funnel<? super T> funnel, int expectedInsertions, double fpp) {
        return create(funnel, (long)expectedInsertions, fpp);
    }

    public static <T> CustomizedBloomFilter<T> create(Funnel<? super T> funnel, long expectedInsertions, double fpp) {
        return create(funnel, expectedInsertions, fpp, BloomFilterStrategies.MURMUR128_MITZ_64);
    }

    @VisibleForTesting
    static <T> CustomizedBloomFilter<T> create(Funnel<? super T> funnel, long expectedInsertions, double fpp, BloomFilter.Strategy strategy) {
        Preconditions.checkNotNull(funnel);
        Preconditions.checkArgument(expectedInsertions >= 0L, "Expected insertions (%s) must be >= 0", expectedInsertions);
        Preconditions.checkArgument(fpp > 0.0D, "False positive probability (%s) must be > 0.0", fpp);
        Preconditions.checkArgument(fpp < 1.0D, "False positive probability (%s) must be < 1.0", fpp);
        Preconditions.checkNotNull(strategy);
        if (expectedInsertions == 0L) {
            expectedInsertions = 1L;
        }

        long numBits = optimalNumOfBits(expectedInsertions, fpp);
        int numHashFunctions = optimalNumOfHashFunctions(expectedInsertions, numBits);

        try {
            return new CustomizedBloomFilter(new LockFreeBitArray(numBits), numHashFunctions, funnel, strategy);
        } catch (IllegalArgumentException var10) {
            throw new IllegalArgumentException("Could not create BloomFilter of " + numBits + " bits", var10);
        }
    }

    public static <T> CustomizedBloomFilter<T> create(Funnel<? super T> funnel, int expectedInsertions) {
        return create(funnel, (long)expectedInsertions);
    }

    public static <T> CustomizedBloomFilter<T> create(Funnel<? super T> funnel, long expectedInsertions) {
        return create(funnel, expectedInsertions, 0.03D);
    }

    @VisibleForTesting
    static int optimalNumOfHashFunctions(long n, long m) {
        return Math.max(1, (int)Math.round((double)m / (double)n * Math.log(2.0D)));
    }

    @VisibleForTesting
    static long optimalNumOfBits(long n, double p) {
        if (p == 0.0D) {
            p = 4.9E-324D;
        }

        return (long)((double)(-n) * Math.log(p) / (Math.log(2.0D) * Math.log(2.0D)));
    }

    private Object writeReplace() {
        return new CustomizedBloomFilter.SerialForm(this);
    }

    public void writeTo(OutputStream out) throws IOException {
        DataOutputStream dout = new DataOutputStream(out);
        dout.writeByte(SignedBytes.checkedCast((long)this.strategy.ordinal()));
        dout.writeByte(UnsignedBytes.checkedCast((long)this.numHashFunctions));
        dout.writeInt(this.bits.data.length());

        for(int i = 0; i < this.bits.data.length(); ++i) {
            dout.writeLong(this.bits.data.get(i));
        }

    }

    public static <T> CustomizedBloomFilter<T> readFrom(InputStream in, Funnel<? super T> funnel) throws IOException {
        Preconditions.checkNotNull(in, "InputStream");
        Preconditions.checkNotNull(funnel, "Funnel");
        int strategyOrdinal = -1;
        int numHashFunctions = -1;
        int dataLength = -1;

        try {
            DataInputStream din = new DataInputStream(in);
            strategyOrdinal = din.readByte();
            numHashFunctions = UnsignedBytes.toInt(din.readByte());
            dataLength = din.readInt();
            BloomFilter.Strategy strategy = BloomFilterStrategies.values()[strategyOrdinal];
            long[] data = new long[dataLength];

            for(int i = 0; i < data.length; ++i) {
                data[i] = din.readLong();
            }

            return new CustomizedBloomFilter(new LockFreeBitArray(data), numHashFunctions, funnel, strategy);
        } catch (RuntimeException var9) {
            String message = "Unable to deserialize BloomFilter from InputStream. strategyOrdinal: " + strategyOrdinal + " numHashFunctions: " + numHashFunctions + " dataLength: " + dataLength;
            throw new IOException(message, var9);
        }
    }

    private static class SerialForm<T> implements Serializable {
        final long[] data;
        final int numHashFunctions;
        final Funnel<? super T> funnel;
        final BloomFilter.Strategy strategy;
        private static final long serialVersionUID = 1L;

        SerialForm(CustomizedBloomFilter<T> bf) {
            this.data = LockFreeBitArray.toPlainArray(bf.bits.data);
            this.numHashFunctions = bf.numHashFunctions;
            this.funnel = bf.funnel;
            this.strategy = bf.strategy;
        }

        Object readResolve() {
            return new CustomizedBloomFilter(new LockFreeBitArray(this.data), this.numHashFunctions, this.funnel, this.strategy);
        }
    }
}
