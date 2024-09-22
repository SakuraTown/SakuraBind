/*
   Copyright 2016 Mark Gunlogson

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.github.mgunlogson.cuckoofilter4j;


import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;


/**
 * This class represents the link to access methods on the underlying BitSet.
 *
 * @author Mark Gunlogson
 */
final class FilterTable implements Serializable {
    private static final long serialVersionUID = 4172048932165857538L;
    /*
     * NOTE: Google's Guava library uses a custom BitSet implementation that
     * looks to be adapted from the Lucene project. Guava project notes show
     * this seems to be done for faster serialization and support for
     * longs(giant filters). We just use the Lucene LongBitSet directly to make
     * updates easier.
     *
     * NOTE: for speed, we don't check for inserts into invalid bucket indexes
     * or bucket positions!
     */
    private final LongBitSet memBlock;

    private final int bitsPerTag;

    private final long numBuckets;

    private FilterTable(LongBitSet memBlock, int bitsPerTag, long numBuckets) {
        this.bitsPerTag = bitsPerTag;
        this.memBlock = memBlock;
        this.numBuckets = numBuckets;
    }

    /**
     * Creates a FilterTable
     *
     * @param bitsPerTag number of bits needed for each tag
     * @param numBuckets number of buckets in filter
     */
    static FilterTable create(int bitsPerTag, long numBuckets) {
        // why would this ever happen?
        if (bitsPerTag > 47) throw new IllegalArgumentException("tagBits should be less than 48 bits");
        // shorter fingerprints don't give us a good fill capacity
        if (bitsPerTag < 5) throw new IllegalArgumentException("tagBits must be > 4");
        if (numBuckets < 2) throw new IllegalArgumentException("numBuckets must be > 1");
        // checked so our implementors don't get too.... "enthusiastic" with
        // table size
        long bitsPerBucket = checkedMultiply(CuckooFilter.BUCKET_SIZE, bitsPerTag);
        long bitSetSize = checkedMultiply(bitsPerBucket, numBuckets);
        LongBitSet memBlock = new LongBitSet(bitSetSize);
        return new FilterTable(memBlock, bitsPerTag, numBuckets);
    }

    public static int checkedMultiply(int a, int b) {
        long result = (long) a * b;
        checkNoOverflow(result == (int) result);
        return (int) result;
    }

    public static long checkedMultiply(long a, long b) {
        // Hacker's Delight, Section 2-12
        int leadingZeros = Long.numberOfLeadingZeros(a) + Long.numberOfLeadingZeros(~a)
                + Long.numberOfLeadingZeros(b) + Long.numberOfLeadingZeros(~b);
        /*
         * If leadingZeros > Long.SIZE + 1 it's definitely fine, if it's < Long.SIZE it's definitely
         * bad. We do the leadingZeros check to avoid the division below if at all possible.
         *
         * Otherwise, if b == Long.MIN_VALUE, then the only allowed values of a are 0 and 1. We take
         * care of all a < 0 with their own check, because in particular, the case a == -1 will
         * incorrectly pass the division check below.
         *
         * In all other cases, we check that either a is 0 or the result is consistent with division.
         */
        if (leadingZeros > Long.SIZE + 1) {
            return a * b;
        }
        checkNoOverflow(leadingZeros >= Long.SIZE);
        checkNoOverflow(a >= 0 | b != Long.MIN_VALUE);
        long result = a * b;
        checkNoOverflow(a == 0 || result / a == b);
        return result;
    }

    private static void checkNoOverflow(boolean condition) {
        if (!condition) {
            throw new ArithmeticException("overflow");
        }
    }

    /**
     * inserts a tag into an empty position in the chosen bucket.
     *
     * @param bucketIndex index
     * @param tag         tag
     * @return true if insert succeeded(bucket not full)
     */
    boolean insertToBucket(long bucketIndex, long tag) {

        for (int i = 0; i < CuckooFilter.BUCKET_SIZE; i++) {
            if (checkTag(bucketIndex, i, 0)) {
                writeTagNoClear(bucketIndex, i, tag);
                return true;
            }
        }
        return false;
    }

    /**
     * Replaces a tag in a random position in the given bucket and returns the
     * tag that was replaced.
     *
     * @param curIndex bucket index
     * @param tag      tag
     * @return the replaced tag
     */
    long swapRandomTagInBucket(long curIndex, long tag) {
        int randomBucketPosition = ThreadLocalRandom.current().nextInt(CuckooFilter.BUCKET_SIZE);
        return readTagAndSet(curIndex, randomBucketPosition, tag);
    }

    /**
     * Finds a tag if present in two buckets.
     *
     * @param i1  first bucket index
     * @param i2  second bucket index (alternate)
     * @param tag tag
     * @return true if tag found in one of the buckets
     */
    boolean findTag(long i1, long i2, long tag) {
        for (int i = 0; i < CuckooFilter.BUCKET_SIZE; i++) {
            if (checkTag(i1, i, tag) || checkTag(i2, i, tag))
                return true;
        }
        return false;
    }

    long getStorageSize() {
        // NOTE: checked source in current Lucene LongBitSet class for thread
        // safety, make sure it stays this way if you update the class.
        return memBlock.length();
    }

    /**
     * Deletes an item from the table if it is found in the bucket
     *
     * @param i1  bucket index
     * @param tag tag
     * @return true if item was deleted
     */
    boolean deleteFromBucket(long i1, long tag) {
        for (int i = 0; i < CuckooFilter.BUCKET_SIZE; i++) {
            if (checkTag(i1, i, tag)) {
                deleteTag(i1, i);
                return true;
            }
        }
        return false;
    }

    /**
     * Works but currently only used for testing
     */
    long readTag(long bucketIndex, int posInBucket) {
        long tagStartIdx = getTagOffset(bucketIndex, posInBucket);
        long tag = 0;
        long tagEndIdx = tagStartIdx + bitsPerTag;
        // looping over true bits per nextBitSet javadocs
        for (long i = memBlock.nextSetBit(tagStartIdx); i >= 0 && i < tagEndIdx; i = memBlock.nextSetBit(i + 1L)) {
            // set corresponding bit in tag
            tag |= 1L << (i - tagStartIdx);
        }
        return tag;
    }

    /**
     * Reads a tag and sets the bits to a new tag at same time for max
     * speedification
     */
    long readTagAndSet(long bucketIndex, int posInBucket, long newTag) {
        long tagStartIdx = getTagOffset(bucketIndex, posInBucket);
        long tag = 0;
        long tagEndIdx = tagStartIdx + bitsPerTag;
        int tagPos = 0;
        for (long i = tagStartIdx; i < tagEndIdx; i++) {
            if ((newTag & (1L << tagPos)) != 0) {
                if (memBlock.getAndSet(i)) {
                    tag |= 1L << tagPos;
                }
            } else {
                if (memBlock.getAndClear(i)) {
                    tag |= 1L << tagPos;
                }
            }
            tagPos++;
        }
        return tag;
    }

    /**
     * Check if a tag in a given position in a bucket matches the tag you passed
     * it. Faster than regular read because it stops checking if it finds a
     * non-matching bit.
     */
    boolean checkTag(long bucketIndex, int posInBucket, long tag) {
        long tagStartIdx = getTagOffset(bucketIndex, posInBucket);
        for (long i = 0; i < bitsPerTag; i++) {
            if (memBlock.get(i + tagStartIdx) == ((tag & (1L << i)) == 0))
                return false;
        }
        return true;
    }

    /**
     * Similar to checkTag() except it counts the number of matches in the
     * buckets.
     */
    int countTag(long i1, long i2, long tag) {
        int tagCount = 0;
        for (int posInBucket = 0; posInBucket < CuckooFilter.BUCKET_SIZE; posInBucket++) {
            if (checkTag(i1, posInBucket, tag))
                tagCount++;
            if (checkTag(i2, posInBucket, tag))
                tagCount++;
        }
        return tagCount;
    }

    /**
     * Writes a tag to a bucket position. Faster than regular write because it
     * assumes tag starts with all zeros, but doesn't work properly if the
     * position wasn't empty.
     */
    void writeTagNoClear(long bucketIndex, int posInBucket, long tag) {
        long tagStartIdx = getTagOffset(bucketIndex, posInBucket);
        // BIT BANGIN YEAAAARRHHHGGGHHH
        for (int i = 0; i < bitsPerTag; i++) {
            // second arg just does bit test in tag
            if ((tag & (1L << i)) != 0) {
                memBlock.set(tagStartIdx + i);
            }
        }
    }


    /**
     * Deletes (clears) a tag at a specific bucket index and position
     *
     * @param bucketIndex bucket index
     * @param posInBucket position in bucket
     */
    void deleteTag(long bucketIndex, int posInBucket) {
        long tagStartIdx = getTagOffset(bucketIndex, posInBucket);
        memBlock.clear(tagStartIdx, tagStartIdx + bitsPerTag);
    }

    /**
     * Finds the bit offset in the bitset for a tag
     *
     * @param bucketIndex the bucket index
     * @param posInBucket position in bucket
     */
    private long getTagOffset(long bucketIndex, int posInBucket) {
        return (bucketIndex * CuckooFilter.BUCKET_SIZE * bitsPerTag) + ((long) posInBucket * bitsPerTag);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof FilterTable) {
            FilterTable that = (FilterTable) object;
            return this.bitsPerTag == that.bitsPerTag && this.memBlock.equals(that.memBlock)
                    && this.numBuckets == that.numBuckets;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bitsPerTag, memBlock, numBuckets);
    }

    public FilterTable copy() {
        return new FilterTable(memBlock.clone(), bitsPerTag, numBuckets);
    }

}
