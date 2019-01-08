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

import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.util.Collection;
import java.util.LinkedList;

/** @author tylerbenson */
public class Promise<T> {
  private final PromiseContext context;
  private final MockTracer tracer;
  private final Scope parentScope;

  private final Collection<SuccessCallback<T>> successCallbacks = new LinkedList<>();
  private final Collection<ErrorCallback> errorCallbacks = new LinkedList<>();

  public Promise(PromiseContext context, MockTracer tracer) {
    this.context = context;

    // Passed along here for testing. Normally should be referenced via GlobalTracer.get().
    this.tracer = tracer;
    parentScope = tracer.scopeManager().active();
  }

  public void onSuccess(SuccessCallback<T> successCallback) {
    successCallbacks.add(successCallback);
  }

  public void onError(ErrorCallback errorCallback) {
    errorCallbacks.add(errorCallback);
  }

  public void success(final T result) {
    for (final SuccessCallback<T> callback : successCallbacks) {
      context.submit(
          new Runnable() {
            @Override
            public void run() {
                try (Scope child =
                    tracer
                        .buildSpan("success")
                            .addReference(References.FOLLOWS_FROM, parentScope.span().context())
                        .withTag(Tags.COMPONENT.getKey(), "success")
                        .startActive(true)) {
                  callback.accept(result);
                }
              context.getPhaser().arriveAndAwaitAdvance(); // trace reported
            }
          });
    }
  }

  public void error(final Throwable error) {
    for (final ErrorCallback callback : errorCallbacks) {
      context.submit(
          new Runnable() {
            @Override
            public void run() {
                try (Scope child =
                    tracer
                        .buildSpan("error")
                            .addReference(References.FOLLOWS_FROM, parentScope.span().context())
                        .withTag(Tags.COMPONENT.getKey(), "error")
                        .startActive(true)) {
                  callback.accept(error);
                }
              context.getPhaser().arriveAndAwaitAdvance(); // trace reported
            }
          });
    }
  }

  public interface SuccessCallback<T> {
    /** @param t the result of the promise */
    void accept(T t);
  }

  public interface ErrorCallback {
    /** @param t the error result of the promise */
    void accept(Throwable t);
  }
}
