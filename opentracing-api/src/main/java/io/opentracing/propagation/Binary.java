/*
 * Copyright 2016-2018 The OpenTracing Authors
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
package io.opentracing.propagation;

import java.nio.ByteBuffer;

/**
 * Binary is an interface defining the required operations for a binary carrier for
 * Tracer.inject() and Tracer.extract(). Binary can be defined either as inbound (extraction)
 * or outbound (injection).
 *
 * When Binary is defined as inbound, extractionBuffer() will be called to retrieve the ByteBuffer
 * containing the data used for SpanContext extraction.
 *
 * When Binary is defined as outbound, setInjectBufferLength() will be called in order to hint
 * the required buffer length to inject the SpanContext, and injectionBuffer() will be called
 * afterwards to retrieve the actual ByteBuffer used for the SpanContext injection.
 *
 * @see Format.Builtin#BINARY
 * @see io.opentracing.Tracer#inject(SpanContext, Format, Object)
 * @see io.opentracing.Tracer#extract(Format, Object)
 */
public interface Binary {
    /**
     * Gets the buffer used to store data as part of {@link SpanContext} injection.
     *
     * The lenght parameter hints the buffer length required for
     * {@link SpanContext} injection. The user may use this to allocate a new
     * ByteBuffer or resize an existing one.
     *
     * It is an error to call this method when Binary is used
     * for {@link SpanContext} extraction.
     *
     * @param length The buffer length required for {@link SpanContext} injection.
     *               It needs to be larger than zero.
     *
     * @return The buffer used for {@link SpanContext} injection.
     */
    ByteBuffer injectionBuffer(int lenght);

    /**
     * Gets the buffer containing the data used for {@link SpanContext} extraction.
     *
     * It is an error to call this method when Binary is used
     * for {@link SpanContext} injection.
     *
     * @return The buffer used for {@link SpanContext} extraction.
     */
    ByteBuffer extractionBuffer();
}
