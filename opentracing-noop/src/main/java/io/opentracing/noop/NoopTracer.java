/*
 * Copyright 2016-2020 The OpenTracing Authors
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
package io.opentracing.noop;

import io.opentracing.ScopeManager;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

public interface NoopTracer extends Tracer {
}

final class NoopTracerImpl implements NoopTracer {
    final static NoopTracer INSTANCE = new NoopTracerImpl();

    @Override
    public ScopeManager scopeManager() {
        return NoopScopeManager.INSTANCE;
    }

    @Override
    public Span activeSpan() {
        return NoopSpanImpl.INSTANCE;
    }

    @Override
    public Scope activateSpan(Span span) {
        return NoopScopeManager.NoopScope.INSTANCE;
    }

    @Override
    public SpanBuilder buildSpan(String operationName) { return NoopSpanBuilderImpl.INSTANCE; }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {}

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) { return NoopSpanContextImpl.INSTANCE; }

    @Override
    public void close() {}

    @Override
    public String toString() { return NoopTracer.class.getSimpleName(); }
}

