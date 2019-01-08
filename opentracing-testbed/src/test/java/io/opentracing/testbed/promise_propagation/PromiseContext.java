/*
 * Copyright 2016-2019 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.testbed.promise_propagation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

/** @author tylerbenson */
public class PromiseContext implements AutoCloseable {

  private final Phaser phaser;
  private final ExecutorService executor;

  public PromiseContext(Phaser phaser, int concurrency) {
    this.phaser = phaser;
    executor = Executors.newFixedThreadPool(concurrency);
  }

  @Override
  public void close() {
    executor.shutdown();
  }

  public void submit(Runnable runnable) {
    phaser.register(); // register the work to be done on the executor
    executor.submit(runnable);
  }

  public Phaser getPhaser() {
    return phaser;
  }
}
