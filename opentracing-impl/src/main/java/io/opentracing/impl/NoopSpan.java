/**
 * Copyright 2016 The OpenTracing Authors
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
package io.opentracing.impl;

import io.opentracing.NoopSpanContext;
import io.opentracing.Span;

final class NoopSpan extends AbstractSpan implements io.opentracing.NoopSpan, NoopSpanContext {

    static final NoopSpan INSTANCE = new NoopSpan("noop");

    public NoopSpan(String operationName) {
        super(operationName);
    }

    @Override
    public void finish() {
    }

    @Override
    public void finish(long finishMicros) {
    }

    @Override
    public String getBaggageItem(String key) {
        return io.opentracing.NoopSpan.INSTANCE.getBaggageItem(key);
    }

    @Override
    public Span setOperationName(String operationName) {
        return this;
    }

}
