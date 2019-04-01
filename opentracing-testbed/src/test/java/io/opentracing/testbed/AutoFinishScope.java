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

package io.opentracing.testbed;

import io.opentracing.Scope;
import io.opentracing.Span;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The operation mode of this class contrasts with the 0.32
 * deprecation of auto finishing {@link Span}s upon {@link Scope#close()}.

 * {@link AutoFinishScope} is a {@link Scope} implementation that uses ref-counting
 * to automatically finish the wrapped {@link Span}.
 *
 * @see AutoFinishScopeManager
 */
public class AutoFinishScope implements Scope {
    final AutoFinishScopeManager manager;
    final AtomicInteger refCount;
    private final Span wrapped;
    private final AutoFinishScope toRestore;

    AutoFinishScope(AutoFinishScopeManager manager, AtomicInteger refCount, Span wrapped) {
        this.manager = manager;
        this.refCount = refCount;
        this.wrapped = wrapped;
        this.toRestore = manager.tlsScope.get();
        manager.tlsScope.set(this);
    }

    public class Continuation {
        public Continuation() {
            refCount.incrementAndGet();
        }

        public AutoFinishScope activate() {
            return new AutoFinishScope(manager, refCount, wrapped);
        }
    }

    public Continuation capture() {
        return new Continuation();
    }

    @Override
    public void close() {
        if (manager.tlsScope.get() != this) {
            return;
        }

        if (refCount.decrementAndGet() == 0) {
            wrapped.finish();
        }

        manager.tlsScope.set(toRestore);
    }

    public Span span() {
        return wrapped;
    }
}
