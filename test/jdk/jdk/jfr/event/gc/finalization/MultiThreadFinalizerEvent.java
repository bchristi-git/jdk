/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.jfr.event.gc.finalization;

import java.io.IOException;
import java.util.*;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/*
 * @test
 * @bug 8266936
 * @summary Test the finalization JFR event on multiple threads
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run testng/othervm jdk.jfr.event.gc.finalization.MultiThreadFinalizerEvent
 */

public class MultiThreadFinalizerEvent {
    static final int NUM_TO_FINALIZE = 5;
    private static final int NUM_THREADS = 1000;
    private static final int EXPECTED = NUM_TO_FINALIZE * NUM_THREADS;
    static long useObjSoNotElided;

    @Test
    public void test() throws InterruptedException, IOException {
        try (Recording recording = new Recording()) {
            recording.enable(EventNames.FinalizationStatistics);
            recording.start();

            List<Thread> threadList = new ArrayList<>(NUM_THREADS);
            for (int i = 0; i < NUM_THREADS; i++) {
                threadList.add(new Thread(new GarbageRunnable(), "TestThread " + i));
            }
            
            threadList.forEach(t -> t.start());
            threadList.forEach(t -> {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } 
            });

            while (FinalizableClass.numFinalized < EXPECTED) {
                System.gc();
                Thread.sleep(10);
            }            
            
            recording.stop();

            List<RecordedEvent> events = Events.fromRecording(recording);
            long numRecorded = 0;
            for (RecordedEvent event : events) {
                RecordedClass clazz = event.getValue("finalizedClass");
                if (FinalizableClass.class.getName().equals(clazz.getName())) {
                    numRecorded = event.getValue("numFinalized");
                }
            }
            assertEquals(numRecorded, EXPECTED);
        }
    }
}

/* create NUM_TO_FINALIZE garbage FinalizableClass'es */
class GarbageRunnable implements Runnable {
    @Override
    public void run() {
        for (int i = 0; i < MultiThreadFinalizerEvent.NUM_TO_FINALIZE; i++) {
            FinalizableClass finalizeMe = new FinalizableClass();
            MultiThreadFinalizerEvent.useObjSoNotElided += finalizeMe.hashCode();
            finalizeMe = null;
        }
    }    
}

class FinalizableClass {
    static volatile int numFinalized = 0;
    
    @Override
    protected void finalize() throws Throwable {
        FinalizableClass.numFinalized++;
    }
}
