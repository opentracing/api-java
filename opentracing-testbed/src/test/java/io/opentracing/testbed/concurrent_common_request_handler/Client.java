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
package io.opentracing.testbed.concurrent_common_request_handler;

import io.opentracing.testbed.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final RequestHandler requestHandler;


    public Client(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }


    public Future<String> send(final Object message) {

        final Context context = new Context();
        return executor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                logger.info("send {}", message);
                TestUtils.sleep();
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        TestUtils.sleep();
                        requestHandler.beforeRequest(message, context);
                    }
                }).get();

                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        TestUtils.sleep();
                        requestHandler.afterResponse(message, context);
                    }
                }).get();

                return message + ":response";
            }
        });

    }
}
