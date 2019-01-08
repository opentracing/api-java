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
package io.opentracing.testbed.multiple_callbacks;

import io.opentracing.Scope;
import io.opentracing.util.AutoFinishScopeManager;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.mock.MockTracer.Propagator;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.opentracing.testbed.TestUtils.finishedSpansSize;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MultipleCallbacksTest {

    private final MockTracer tracer = new MockTracer(new AutoFinishScopeManager(),
            Propagator.TEXT_MAP);

    @Test
    public void test() throws Exception {
        Client client = new Client(tracer);
        try (Scope scope = tracer.buildSpan("parent").startActive(false)) {
            client.send("task1", 300);
            client.send("task2", 200);
            client.send("task3", 100);
        }

        await().atMost(15, TimeUnit.SECONDS).until(finishedSpansSize(tracer), equalTo(4));

        List<MockSpan> spans = tracer.finishedSpans();
        assertEquals(4, spans.size());
        assertEquals("parent", spans.get(3).operationName());

        MockSpan parentSpan = spans.get(3);
        for (int i = 0; i < 3; i++) {
            assertEquals(true, parentSpan.finishMicros() >= spans.get(i).finishMicros());
            assertEquals(parentSpan.context().traceId(), spans.get(i).context().traceId());
            assertEquals(parentSpan.context().spanId(), spans.get(i).parentId());
        }

        assertNull(tracer.scopeManager().active());
    }
}
