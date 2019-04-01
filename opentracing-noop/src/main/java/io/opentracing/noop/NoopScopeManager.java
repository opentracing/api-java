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
package io.opentracing.noop;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;

public interface NoopScopeManager extends ScopeManager {
    NoopScopeManager INSTANCE = new NoopScopeManagerImpl();

    interface NoopScope extends Scope {
        NoopScope INSTANCE = new NoopScopeManagerImpl.NoopScopeImpl();
    }
}

/**
 * A noop (i.e., cheap-as-possible) implementation of an ScopeManager.
 */
class NoopScopeManagerImpl implements NoopScopeManager {

    @Override
    public Scope activate(Span span) {
        return NoopScope.INSTANCE;
    }

    @Override
    public Span activeSpan() {
        return NoopSpan.INSTANCE;
    }

    static class NoopScopeImpl implements NoopScopeManager.NoopScope {
        @Override
        public void close() {}
    }
}
