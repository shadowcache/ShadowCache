/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.client.file.cache.cuckoofilter;

/**
 * An empty cuckoo table that stores nothing.
 */
public class EmptyCuckooTable implements CuckooTable {

  public EmptyCuckooTable() {}

  @Override
  public int readTag(int bucketIndex, int slotIndex) {
    return 0;
  }

  @Override
  public void writeTag(int bucketIndex, int slotIndex, int tag) {}

  @Override
  public void clear(int bucketIndex, int slotIndex) {}

  @Override
  public void set(int bucketIndex, int slotIndex) {}

  @Override
  public TagPosition findTag(int bucketIndex, int tag) {
    return new TagPosition(-1, -1, CuckooStatus.FAILURE_KEY_NOT_FOUND);
  }

  @Override
  public TagPosition findTag(int bucketIndex1, int bucketIndex2, int tag) {
    return new TagPosition(-1, -1, CuckooStatus.FAILURE_KEY_NOT_FOUND);
  }

  @Override
  public TagPosition deleteTag(int bucketIndex, int tag) {
    return new TagPosition(-1, -1, CuckooStatus.FAILURE_KEY_NOT_FOUND);
  }

  @Override
  public int insertOrKickTag(int bucketIndex, int tag) {
    return 0;
  }

  @Override
  public int getNumTagsPerBuckets() {
    return 0;
  }

  @Override
  public int getNumBuckets() {
    return 0;
  }

  @Override
  public int getBitsPerTag() {
    return 0;
  }

  @Override
  public int getSizeInBytes() {
    return 0;
  }

  @Override
  public int getSizeInTags() {
    return 0;
  }
}
