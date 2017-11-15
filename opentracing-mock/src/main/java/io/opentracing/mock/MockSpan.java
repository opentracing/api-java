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
package io.opentracing.mock;

import io.opentracing.Span;
import io.opentracing.SpanContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MockSpans are created via MockTracer.buildSpan(...), but they are also returned via calls to
 * MockTracer.finishedSpans(). They provide accessors to all Span state.
 *
 * @see MockTracer#finishedSpans()
 */
public final class MockSpan implements Span {
    // A simple-as-possible (consecutive for repeatability) id generator.
    private static AtomicLong nextId = new AtomicLong(0);

    private final MockTracer mockTracer;
    private MockContext context;
    private final long parentId; // 0 if there's no parent.
    private final long startTimestamp;
    private final TimeUnit startUnit;
    private boolean finished;
    private long finishTimestamp;
    private TimeUnit finishUnit;
    private final Map<String, Object> tags;
    private final List<LogEntry> logEntries = new ArrayList<>();
    private String operationName;

    private final List<RuntimeException> errors = new ArrayList<>();

    public String operationName() {
        return this.operationName;
    }

    @Override
    public MockSpan setOperationName(String operationName) {
        finishedCheck("Setting operationName {%s} on already finished span", operationName);
        this.operationName = operationName;
        return this;
    }

    /**
     * TODO: Support multiple parents in this API.
     *
     * @return the spanId of the Span's parent context, or 0 if no such parent exists.
     *
     * @see MockContext#spanId()
     */
    public long parentId() {
        return parentId;
    }

    /**
     * @return the start time of the Span.
     * @deprecated Use {@link #startTimestamp(TimeUnit)}
     */
    @Deprecated
    public long startMicros() {
        return startTimestamp(TimeUnit.MICROSECONDS);
    }

    /**
     * @return the start time of the Span in the specified units.
     */
    public long startTimestamp(TimeUnit unit) {
        return unit.convert(startTimestamp, startUnit);
    }

    /**
     * @return the finish time of the Span; only valid after a call to finish().
     * @deprecated Use {@link #finishTimestamp(TimeUnit)}
     */
    @Deprecated
    public long finishMicros() {
        return finishTimestamp(TimeUnit.MICROSECONDS);
    }

    /**
     * @return the finish time of the Span in the specified units; only valid after a call to finish().
     */
    public long finishTimestamp(TimeUnit units) {
        assert finishTimestamp > 0 : "must call finish() before finishTimestamp()";
        return units.convert(finishTimestamp, finishUnit);
    }

    /**
     * @return a copy of all tags set on this Span.
     */
    public Map<String, Object> tags() {
        return new HashMap<>(this.tags);
    }
    /**
     * @return a copy of all log entries added to this Span.
     */
    public List<LogEntry> logEntries() {
        return new ArrayList<>(this.logEntries);
    }

    /**
     * @return a copy of exceptions thrown by this class (e.g. adding a tag after span is finished).
     */
    public List<RuntimeException> generatedErrors() {
        return new ArrayList<>(errors);
    }

    @Override
    public synchronized MockContext context() {
        return this.context;
    }

    @Override
    public void finish() {
        this.finish(nowMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    @Deprecated
    public synchronized void finish(long finishMicros) {
        finish(finishMicros, TimeUnit.MICROSECONDS);
    }

    @Override
    public synchronized void finish(long finishTimestamp, TimeUnit finishUnit) {
        finishedCheck("Finishing already finished span");
        this.finishTimestamp = finishTimestamp;
        this.finishUnit = finishUnit;
        this.mockTracer.appendFinishedSpan(this);
        this.finished = true;
    }

    @Override
    public MockSpan setTag(String key, String value) {
        return setObjectTag(key, value);
    }

    @Override
    public MockSpan setTag(String key, boolean value) {
        return setObjectTag(key, value);
    }

    @Override
    public MockSpan setTag(String key, Number value) {
        return setObjectTag(key, value);
    }

    private synchronized MockSpan setObjectTag(String key, Object value) {
        finishedCheck("Adding tag {%s:%s} to already finished span", key, value);
        tags.put(key, value);
        return this;
    }

    @Override
    public final Span log(Map<String, ?> fields) {
        return log(nowMillis(), TimeUnit.MILLISECONDS, fields);
    }

    @Override
    @Deprecated
    public final synchronized MockSpan log(long timestampMicros, Map<String, ?> fields) {
        return log(timestampMicros, TimeUnit.MICROSECONDS, fields);
    }

    @Override
    public final synchronized MockSpan log(long timestamp, TimeUnit timestampUnit, Map<String, ?> fields) {
        finishedCheck("Adding logs %s at %d %s to already finished span", fields, timestamp, timestampUnit);
        this.logEntries.add(new LogEntry(timestamp, timestampUnit, fields));
        return this;
    }

    @Override
    public MockSpan log(String event) {
        return this.log(nowMillis(), TimeUnit.MILLISECONDS, event);
    }

    @Override
    @Deprecated
    public MockSpan log(long timestampMicroseconds, String event) {
        return this.log(timestampMicroseconds, TimeUnit.MICROSECONDS, event);
    }

    @Override
    public MockSpan log(long timestamp, TimeUnit timestampUnit, String event) {
        return this.log(timestamp, timestampUnit, Collections.singletonMap("event", event));
    }

    @Override
    public synchronized Span setBaggageItem(String key, String value) {
        finishedCheck("Adding baggage {%s:%s} to already finished span", key, value);
        this.context = this.context.withBaggageItem(key, value);
        return this;
    }

    @Override
    public synchronized String getBaggageItem(String key) {
        return this.context.getBaggageItem(key);
    }

    /**
     * MockContext implements a Dapper-like opentracing.SpanContext with a trace- and span-id.
     *
     * Note that parent ids are part of the MockSpan, not the MockContext (since they do not need to propagate
     * between processes).
     */
    public static final class MockContext implements SpanContext {
        private final long traceId;
        private final Map<String, String> baggage;
        private final long spanId;

        /**
         * A package-protected constructor to create a new MockContext. This should only be called by MockSpan and/or
         * MockTracer.
         *
         * @param baggage the MockContext takes ownership of the baggage parameter
         *
         * @see MockContext#withBaggageItem(String, String)
         */
        public MockContext(long traceId, long spanId, Map<String, String> baggage) {
            this.baggage = baggage;
            this.traceId = traceId;
            this.spanId = spanId;
        }

        public String getBaggageItem(String key) { return this.baggage.get(key); }
        public long traceId() { return traceId; }
        public long spanId() { return spanId; }

        /**
         * Create and return a new (immutable) MockContext with the added baggage item.
         */
        public MockContext withBaggageItem(String key, String val) {
            Map<String, String> newBaggage = new HashMap<>(this.baggage);
            newBaggage.put(key, val);
            return new MockContext(this.traceId, this.spanId, newBaggage);
        }

        @Override
        public Iterable<Map.Entry<String, String>> baggageItems() {
            return baggage.entrySet();
        }
    }

    public static final class LogEntry {
        private final long timestamp;
        private final TimeUnit timestampUnit;
        private final Map<String, ?> fields;

        public LogEntry(long timestamp, TimeUnit timestampUnit, Map<String, ?> fields) {
            this.timestamp = timestamp;
            this.timestampUnit = timestampUnit;
            this.fields = fields;
        }

        public long timestamp(TimeUnit unit) {
            return unit.convert(timestamp, timestampUnit);
        }

        public Map<String, ?> fields() {
            return fields;
        }
    }

    MockSpan(MockTracer tracer, String operationName, long startTimestamp, TimeUnit startUnit, Map<String, Object> initialTags, MockContext parent) {
        this.mockTracer = tracer;
        this.operationName = operationName;
        this.startTimestamp = startTimestamp;
        this.startUnit = startUnit;
        if (initialTags == null) {
            this.tags = new HashMap<>();
        } else {
            this.tags = new HashMap<>(initialTags);
        }
        if (parent == null) {
            // We're a root Span.
            this.context = new MockContext(nextId(), nextId(), new HashMap<String, String>());
            this.parentId = 0;
        } else {
            // We're a child Span.
            this.context = new MockContext(parent.traceId, nextId(), parent.baggage);
            this.parentId = parent.spanId;
        }
    }

    static long nextId() {
        return nextId.addAndGet(1);
    }

    static long nowMillis() {
        return System.currentTimeMillis();
    }

    private synchronized void finishedCheck(String format, Object... args) {
        if (finished) {
            RuntimeException ex = new IllegalStateException(String.format(format, args));
            errors.add(ex);
            throw ex;
        }
    }

    @Override
    public String toString() {
        return "{" +
                "traceId:" + context.traceId() +
                ", spanId:" + context.spanId() +
                ", parentId:" + parentId +
                ", operationName:\"" + operationName + "\"}";
    }
}
