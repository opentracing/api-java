/**
 * Copyright 2016-2017 The OpenTracing Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.util;

import io.opentracing.NoopTracer;
import io.opentracing.NoopTracerFactory;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Forwards all methods to another tracer that can be configured by calling {@link #register(Tracer)}.
 * <p>
 * When the tracer is needed it delegates all tracing methods to
 * either the <em>registered</em> global tracer
 * or the {@link io.opentracing.NoopTracer NoopTracer}.
 */
public final class GlobalTracer implements Tracer {
    private static final Logger LOGGER = Logger.getLogger(GlobalTracer.class.getName());

    /**
     * Singleton instance.
     * <p>
     * Since we cannot prevent people using {@linkplain #get() GlobalTracer.get()} as a constant,
     * this guarantees that references obtained before, during or after initialization
     * all behave as if obtained <em>after</em> initialization once properly initialized.<br>
     * As a minor additional benefit it makes it harder to circumvent the {@link Tracer} API.
     */
    private static final GlobalTracer INSTANCE = new GlobalTracer();

    /**
     * The resolved {@link Tracer} to delegate to or the {@link NoopTracer} if none was registered yet.
     * Never {@code null}.
     */
    private static volatile Tracer globalTracer = NoopTracerFactory.create();

    private GlobalTracer() {
    }

    /**
     * Returns the constant {@linkplain GlobalTracer}.
     * <p>
     * All methods are forwarded to the currently configured tracer.<br>
     * Until a tracer is {@link #register(Tracer) explicitly configured},
     * the {@link io.opentracing.NoopTracer NoopTracer} is used.
     *
     * @return The global tracer constant.
     * @see #register(Tracer)
     */
    public static Tracer get() {
        return INSTANCE;
    }

    /**
     * Explicitly configures a {@link Tracer} to back the behaviour of the {@link #get() global tracer}.
     * <p>
     * Registration is a one-time operation, attempting to call it more often will result in a runtime exception.
     * <p>
     * Every application intending to use the global tracer is responsible for registering it once
     * during its initialization.
     *
     * @param tracer Tracer to use as global tracer.
     * @return The previous global tracer or <code>null</code> if there was none.
     */
    public static void register(final Tracer tracer) {
        if (tracer == null) throw new NullPointerException("Cannot register GlobalTracer <null>.");
        else if (tracer instanceof GlobalTracer) {
            LOGGER.log(Level.FINE, "Attempted to register the GlobalTracer as delegate of itself.");
            return; // no-op
        } else if (!(globalTracer instanceof NoopTracer) && !globalTracer.equals(tracer)) {
            throw new IllegalStateException("There is already a current globalTracer registered.");
        }
        globalTracer = tracer;
    }

    @Override
    public SpanBuilder buildSpan(String operationName) {
        return globalTracer.buildSpan(operationName);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        globalTracer.inject(spanContext, format, carrier);
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) {
        return globalTracer.extract(format, carrier);
    }

    @Override
    public String toString() {
        return GlobalTracer.class.getSimpleName();
    }

}
