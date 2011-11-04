/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.beach.util.concurrent;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MarkedExecutorService extends AbstractExecutorService {
    private final ExecutorService delegate;

    private class MarkedFuture<V> extends FutureTask<V> {
        private final StackTraceElement[] mark;

        MarkedFuture(final StackTraceElement[] mark, final Callable<V> callable) {
            super(callable);
            this.mark = mark;
        }

        MarkedFuture(final StackTraceElement[] mark, final Runnable task, final V value) {
            super(task, value);
            this.mark = mark;
        }

        @Override
        protected void setException(Throwable t) {
            glueStackTraces(t, mark, 3, "asynchronous invocation");
            super.setException(t);
        }
    }

    public MarkedExecutorService(final ExecutorService delegate) {
        if (delegate == null)
            throw new NullPointerException();
        this.delegate = delegate;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(command);
    }

    /**
     * Glue two stack traces together.
     *
     * @param exception the exception which occurred in another thread
     * @param userStackTrace the stack trace of the current thread from {@link Thread#getStackTrace()}
     * @param trimCount the number of frames to trim
     * @param msg the message to use
     * @author David M. Lloyd
     */
    static void glueStackTraces(final Throwable exception, final StackTraceElement[] userStackTrace, final int trimCount, final String msg) {
        final StackTraceElement[] est = exception.getStackTrace();
        final StackTraceElement[] fst = Arrays.copyOf(est, est.length + userStackTrace.length - trimCount + 1);
        fst[est.length] = new StackTraceElement("..." + msg + "..", "", null, -1);
        System.arraycopy(userStackTrace, trimCount, fst, est.length + 1, userStackTrace.length - trimCount);
        exception.setStackTrace(fst);
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        final StackTraceElement[] mark = Thread.currentThread().getStackTrace();
        return new MarkedFuture(mark, runnable, value);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        final StackTraceElement[] mark = Thread.currentThread().getStackTrace();
        return new MarkedFuture(mark, callable);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }
}
