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
package io.opentracing.testbed.listener_per_request;

import io.opentracing.Span;

/**
 * Response listener per request. Executed in a thread different from 'send' thread
 */
public class ResponseListener {

    private final Span span;

    public ResponseListener(Span span) {
        this.span = span;
    }

    /**
     * executed when response is received from server. Any thread.
     */
    public void onResponse(Object response) {
        span.finish();
    }
}
