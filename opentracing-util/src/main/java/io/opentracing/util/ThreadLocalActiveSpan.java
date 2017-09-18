/*
 * Copyright 2016-2017 The OpenTracing Authors
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
package io.opentracing.util;

import java.util.Map;

import io.opentracing.ActiveSpan;
import io.opentracing.ActiveSpanSource;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;

/**
 * {@link ThreadLocalActiveSpan} is a simple {@link ActiveSpan} implementation that relies on Java's
 * thread-local storage primitive.
 *
 * @see ActiveSpanSource
 * @see Tracer#activeSpan()
 */
public class ThreadLocalActiveSpan implements ActiveSpan {
    private final ThreadLocalActiveSpanSource source;
    private final Span wrapped;
    private final ThreadLocalActiveSpan toRestore;
    private final Observer observer;

    ThreadLocalActiveSpan(ThreadLocalActiveSpanSource source, Span wrapped, Observer observer) {
        this.source = source;
        this.wrapped = wrapped;
        this.toRestore = source.tlsSnapshot.get();
        this.observer = observer;
        source.tlsSnapshot.set(this);
    }

    @Override
    public void deactivate() {
        if (source.tlsSnapshot.get() != this) {
            // This shouldn't happen if users call methods in the expected order. Bail out.
            return;
        }
        source.tlsSnapshot.set(toRestore);
        if (observer != null) {
            observer.afterDeactivate(this, this.wrapped);
        }
    }

    @Override
    public Continuation capture() {
        Continuation cont = new ThreadLocalActiveSpan.Continuation();
        if (observer != null) {
            observer.afterCapture(ThreadLocalActiveSpan.this, cont);
        }
        return cont;
    }

    @Override
    public SpanContext context() {
        return wrapped.context();
    }

    @Override
    public ThreadLocalActiveSpan setTag(String key, String value) {
        wrapped.setTag(key, value);
        return this;
    }

    @Override
    public ThreadLocalActiveSpan setTag(String key, boolean value) {
        wrapped.setTag(key, value);
        return this;
    }

    @Override
    public ThreadLocalActiveSpan setTag(String key, Number value) {
        wrapped.setTag(key, value);
        return this;
    }

    @Override
    public ThreadLocalActiveSpan log(Map<String, ?> fields) {
        wrapped.log(fields);
        return this;
    }

    @Override
    public ThreadLocalActiveSpan log(long timestampMicroseconds, Map<String, ?> fields) {
        wrapped.log(timestampMicroseconds, fields);
        return this;
    }

    @Override
    public ThreadLocalActiveSpan log(String event) {
        wrapped.log(event);
        return this;
    }

    @Override
    public ThreadLocalActiveSpan log(long timestampMicroseconds, String event) {
        wrapped.log(timestampMicroseconds, event);
        return this;
    }

    @Override
    public ThreadLocalActiveSpan setBaggageItem(String key, String value) {
        wrapped.setBaggageItem(key, value);
        return this;
    }

    @Override
    public String getBaggageItem(String key) {
        return wrapped.getBaggageItem(key);
    }

    @Override
    public ThreadLocalActiveSpan setOperationName(String operationName) {
        wrapped.setOperationName(operationName);
        return this;
    }

    @Override
    public void close() {
        deactivate();
    }

    @Override
    public String toString() {
        return wrapped.toString();
    }

    private final class Continuation implements ActiveSpan.Continuation {
        @Override
        public ThreadLocalActiveSpan activate() {
            ThreadLocalActiveSpan rval = new ThreadLocalActiveSpan(source, wrapped, observer);
            if (observer != null) {
                observer.afterActivate(this, rval);
            }
            return rval;
        }
    }

}
