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
 * @summary Test the finalization JFR event
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run testng/othervm jdk.jfr.event.gc.finalization.TestFinalizerEvent
 */

public class TestFinalizerEvent {
    private static final int NUM_TO_FINALIZE = 5;
    static long useObjSoNotElided;

    @Test
    public void test() throws InterruptedException, IOException {
        try (Recording recording = new Recording()) {
            recording.enable(EventNames.FinalizationStatistics);
            recording.start();

            for (int i = 0; i < NUM_TO_FINALIZE; i++) {
                FinalizableClass finalizeMe = new FinalizableClass();
                useObjSoNotElided += finalizeMe.hashCode();
                
                OtherFinalizableClass ofc = new OtherFinalizableClass();
                useObjSoNotElided += ofc.hashCode();
                
                finalizeMe = null;
                ofc = null;
            }
            
            while (FinalizableClass.numFinalized < NUM_TO_FINALIZE) {
                System.gc();
                Thread.sleep(10);
            }
            
            for (int i = 0; i < NUM_TO_FINALIZE; i++) {
                // Do 2x of OtherFinalizableClass
                OtherFinalizableClass ofc = new OtherFinalizableClass();
                useObjSoNotElided += ofc.hashCode();
                ofc = null;
            }
            
            while (OtherFinalizableClass.otherNumFinalized < 2 * NUM_TO_FINALIZE) {
                System.gc();
                Thread.sleep(10);
            }
            
            recording.stop();

            List<RecordedEvent> events = Events.fromRecording(recording);
            long numRecorded = 0;
            long otherNumRecorded = 0;
            for (RecordedEvent event : events) {
                System.out.println("Event: " + event);
                RecordedClass clazz = event.getValue("finalizedClass");
                if (FinalizableClass.class.getName().equals(clazz.getName())) {
                    numRecorded = event.getValue("numFinalized");
                } else if (OtherFinalizableClass.class.getName().equals(clazz.getName())) {
                    otherNumRecorded = event.getValue("numFinalized");
                }
            }
            
            assertEquals(numRecorded, NUM_TO_FINALIZE, "Expected: " + NUM_TO_FINALIZE + ", got: " + numRecorded);
            assertEquals(otherNumRecorded, NUM_TO_FINALIZE * 2, "Expected: " + (NUM_TO_FINALIZE * 2) + ", got: " + otherNumRecorded);            
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

class OtherFinalizableClass extends FinalizableClass {
    static volatile int otherNumFinalized = 0;
    
    @Override
    protected void finalize() throws Throwable {
        // Don't call super.finalize()!
        OtherFinalizableClass.otherNumFinalized++;
    }
}
