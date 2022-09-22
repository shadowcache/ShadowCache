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

package alluxio.client.file.cache.dataset.generator;

import alluxio.client.file.cache.dataset.DatasetEntry;
import alluxio.client.file.cache.dataset.DatasetUtils;
import alluxio.client.quota.CacheScope;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ThroughputEntryGenerator implements EntryGenerator<String> {
  private static int NUM_ENTRIES_PER_LOAD = 1000;

  private final String path;
  private final AtomicLong count;
  private BufferedReader reader;
  private Queue<DatasetEntry<String>> entries;
  private AtomicBoolean fileReady;

  public ThroughputEntryGenerator(String path) {
    this.path = path;
    this.count = new AtomicLong(0);
    this.entries = new LinkedList<>();
    this.fileReady = new AtomicBoolean(false);
    openFile();
  }

  private static DatasetEntry<String> parseEntry(String line) {
    long timestamp = 0;
    String scope = "self.make"; // Scope format `Hostname.DiskNumber`
    int size = 1024;
    return new DatasetEntry<>(line, size, CacheScope.create(scope), timestamp);
  }

  @Override
  public DatasetEntry<String> next() {
    if (entries.isEmpty() && fileReady.get()) {
      loadEntry(NUM_ENTRIES_PER_LOAD);
    }
    count.incrementAndGet();
    return entries.poll();
  }

  @Override
  public boolean hasNext() {
    return !entries.isEmpty() || fileReady.get();
  }

  private void openFile() {
    try {
      FileReader reader = new FileReader(path);
      this.reader = new BufferedReader(reader);
      this.fileReady.set(true);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private void loadEntry(int num) {
    try {
      int nloaded = 0;
      while (nloaded < num) {
        String line = reader.readLine();
        if (line == null) {
          fileReady.set(false);
          return;
        }
        DatasetEntry<String> entry = parseEntry(line);
        entries.offer(entry);
        nloaded++;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
