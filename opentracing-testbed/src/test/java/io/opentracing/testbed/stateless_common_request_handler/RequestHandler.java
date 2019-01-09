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
package io.opentracing.testbed.stateless_common_request_handler;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One instance per Client. 'beforeRequest' and 'afterResponse'
 * are executed in the same thread for one 'send',
 * but as these methods do not expose any object storing state,
 * a thread-local field in 'RequestHandler' itself is used
 * to contain the Scope related to Span activation.
 */
public class RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    static final String OPERATION_NAME = "send";

    private final Tracer tracer;

    private final ThreadLocal<Scope> tlsScope = new ThreadLocal<Scope>();

    public RequestHandler(Tracer tracer) {
        this.tracer = tracer;
    }

    public void beforeRequest(Object request) {
        logger.info("before send {}", request);

        Span span = tracer.buildSpan(OPERATION_NAME).start();
        tlsScope.set(tracer.activateSpan(span));
    }

    public void afterResponse(Object response) {
        logger.info("after response {}", response);

        // Finish the Span
        tracer.scopeManager().activeSpan().finish();

        // Deactivate the Span
        tlsScope.get().close();
    }
}