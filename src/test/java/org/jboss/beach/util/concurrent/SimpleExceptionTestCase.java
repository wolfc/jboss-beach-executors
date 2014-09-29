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

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SimpleExceptionTestCase {
    private static int currentLine() {
        return Thread.currentThread().getStackTrace()[2].getLineNumber();
    }

    @Test
    public void testException() throws InterruptedException {
        final ExecutorService service = Executors.newFixedThreadPool(1);
        final Future<Void> future = service.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                throw new Exception("throw up");
            }
        });
        try {
            future.get();
            fail("Should have thrown an ExecutionException");
        } catch (ExecutionException e) {
            // To show the difference
            e.getCause().printStackTrace();
        }
    }

    @Test
    public void testMarkedException() throws InterruptedException {
        final ExecutorService service = new MarkedExecutorService(Executors.newFixedThreadPool(1));
        final int currentLine = currentLine();
        final Future<Void> future = service.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                throw new Exception("throw up");
            }
        });
        try {
            future.get();
            fail("Should have thrown an ExecutionException");
        } catch (ExecutionException e) {
            // To show the difference
            e.getCause().printStackTrace();
            final StackTraceElement[] where = e.getCause().getStackTrace();
            // ehr, probably very dependant upon implementation details
            assertEquals(currentLine + 1, where[7].getLineNumber());
        }
    }
}
