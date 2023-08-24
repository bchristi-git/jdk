/*
 * Copyright (c) 2015, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package java.lang.ref;

import jdk.internal.ref.CleanerImpl;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

/**
 * {@code Cleaner} manages cleanup that can happen after an object becomes
 * unreachable.
 * This is needed when some "Owner" object uses a resource that requires
 * cleanup (by being closed, for instance), but the owner
 * cannot be used within a {@code try}-with-resources block.
 * <p>
 * An owner object registers itself with an instance of Cleaner, and includes
 * a corresponding cleanup "Context".
 * The context object implements Runnable, and contains everything needed for cleanup:
 * <ul>
 * <li>references to the resources to be cleaned up</li>
 * <li>a run() method (or "cleaning action") containing code to perform the cleanup.</li>
 * </ul>
 * The cleaning action is eligible to run
 * after the registered owner object has become
 * <a href="package-summary.html#reachability-heading">phantom-reachable</a>.
 * <p>
 * Registering an owner object and its context with Cleaner returns
 * a {@link Cleanable Cleanable}.
 * The {@link Cleanable#clean clean} method can be called when the owner object
 * is no longer needed, to run the cleaning action right away. This avoids
 * having to wait for the garbage collector to determine that the owner object
 * has become unreachable. Such "prompt cleanup" is the most reliable and
 * efficient use of Cleaner, and is recommended whenever possible.
 * The cleaning action is invoked at most once when
 * the object has become phantom reachable unless it has already been explicitly cleaned.
 * <p>
 * The Context must not contain a reference to its Owner object.
 * It it does, the Cleaner mechanism will keep both strongly-reachable, and the
 * cleaning action will not be invoked automatically.
 * <p>
 * Each cleaner instance operates independently, managing pending cleaning actions
 * and handling threading and termination when the cleaner is no longer in use.
 * Cleaner instances are created using the {@link #create()} factory method. It is
 * recommended that Cleaner instances be shared - for example by all instances
 * of a class, or all classes in a library.
 * <p>
 * The execution of the cleaning action is performed
 * by a thread associated with the cleaner.
 * All exceptions thrown by the cleaning action are ignored.
 * The cleaner and other cleaning actions are not affected by
 * exceptions in a cleaning action.
 * The thread runs until all registered cleaning actions have
 * completed and the cleaner itself is reclaimed by the garbage collector.
 * <p>
 * The behavior of cleaners during {@link System#exit(int) System.exit}
 * is implementation specific. No guarantees are made relating
 * to whether cleaning actions are invoked or not.
 * <p>
 * Unless otherwise noted, passing a {@code null} argument to a constructor or
 * method in this class will cause a
 * {@link java.lang.NullPointerException NullPointerException} to be thrown.
 *
 * @apiNote
 * Cleaner invokes the cleaning action only after the associated Owner object becomes
 * phantom reachable, so it is important that the Context object not hold
 * a reference to the Owner.
 * In this example, the Context is implemented as a static inner class.
 * A non-static inner class, anonymous or not, must not be used because it
 * implicitly contains a reference to the enclosing Owner instance,
 * preventing it from becoming phantom reachable.
 * The choice of a new cleaner or sharing an existing cleaner is determined
 * by the use case.
 * <p>
 * If the CleaningOwnerExample is used in a try-finally block then the
 * {@code close} method calls the cleaning action.
 * If the {@code close} method is not called, the cleaning action is called
 * by the Cleaner after the CleaningOwnerExample instance becomes phantom reachable.
 * <pre>{@code
 * public class CleaningOwnerExample implements AutoCloseable {
 *        // A cleaner (preferably one shared within a library,
          // but for the sake of example, a new one is created here)
 *        private static final Cleaner cleaner = Cleaner.create();
 *
 *        // Context class captures information necessary for cleanup.
 *        // It must hold no reference to the Owner instance being cleaned
 *        // and therefore it is a static inner class in this example.
 *        static class Context implements Runnable {
 *            // Resources requiring cleanup
 *            SomeResource resource;
 *
 *            Context(...) {
 *                // initialize context needed for cleaning action
 *                resource = ...
 *            }
 *
 *            public void run() {
 *                // cleaning action accessing Context, executed at most once
 *                resource.close();
 *            }
 *        }
 *
 *        private final Context context;             // for accessing the resource
 *        private final Cleaner.Cleanable cleanable; // for calling clean()
 *
 *        public CleaningExample() {
 *            this.context = new Context(...);
 *            this.cleanable = cleaner.register(this, context);
 *        }
 *
 *        public void doSomething() {
 *            useResource(context.resource);
 *        }
 *
 *        public void close() {
 *            cleanable.clean();
 *        }
 *    }
 * }</pre>
 * The Context could be a lambda but all too easily will capture
 * the Owner object reference, by referring to fields of the Owner,
 * preventing the object from becoming phantom reachable.
 * Using a static nested class, as above, will avoid accidentally retaining the
 * object reference.
 * <p>
 * <a id="compatible-cleaners"></a>
 * Cleaning actions should be prepared to be invoked concurrently with
 * other cleaning actions.
 * Typically the cleaning actions should be very quick to execute
 * and not block. If the cleaning action blocks, it may delay processing
 * other cleaning actions registered to the same cleaner.
 * All cleaning actions registered to a cleaner should be mutually compatible.
 * <p>
 * It is possible for the garbage collector to mark an Owner object
 * phantom-reachable <em>while one of the object's method is still running</em>.
 * It can happen that the owner becomes unreachable, the cleaning
 * action runs on the cleaner's thread, and then the still-running method access
 * an already-closed resource. This scenario is called, "premature cleanup."
 *
 * An object's reachability can be maintained using {@link Reference#reachabilityFence(Object)}.
 * To avoid premature cleanup, it is recommended that code in the Owner's methods
 * that access a resource (or other state within the Context) be enclosed in
 * a try-finally block with a {@code reachabilityFence(this)}.
 *
 * <pre>{@code
 * public void doSomething {
 *     try {
 *         useResource(context.resource);
 *     } finally {
 *         Reference.reachabilityFence(this);
 *     }
 * }
 * }</pre>
 *
 * @since 9
 */
public final class Cleaner {

    /**
     * The Cleaner implementation.
     */
    final CleanerImpl impl;

    static {
        CleanerImpl.setCleanerImplAccess(new Function<Cleaner, CleanerImpl>() {
            @Override
            public CleanerImpl apply(Cleaner cleaner) {
                return cleaner.impl;
            }
        });
    }

    /**
     * Construct a Cleaner implementation and start it.
     */
    private Cleaner() {
        impl = new CleanerImpl();
    }

    /**
     * Returns a new {@code Cleaner}.
     * <p>
     * The cleaner creates a {@link Thread#setDaemon(boolean) daemon thread}
     * to process the phantom reachable objects and to invoke cleaning actions.
     * The {@linkplain java.lang.Thread#getContextClassLoader context class loader}
     * of the thread is set to the
     * {@link ClassLoader#getSystemClassLoader() system class loader}.
     * The thread has no permissions, enforced only if a
     * {@link java.lang.System#setSecurityManager(SecurityManager) SecurityManager is set}.
     * <p>
     * The cleaner terminates when it is phantom reachable and all of the
     * registered cleaning actions are complete.
     *
     * @return a new {@code Cleaner}
     *
     * @throws  SecurityException  if the current thread is not allowed to
     *               create or start the thread.
     */
    public static Cleaner create() {
        Cleaner cleaner = new Cleaner();
        cleaner.impl.start(cleaner, null);
        return cleaner;
    }

    /**
     * Returns a new {@code Cleaner} using a {@code Thread} from the {@code ThreadFactory}.
     * <p>
     * A thread from the thread factory's {@link ThreadFactory#newThread(Runnable) newThread}
     * method is set to be a {@link Thread#setDaemon(boolean) daemon thread}
     * and started to process phantom reachable objects and invoke cleaning actions.
     * On each call the {@link ThreadFactory#newThread(Runnable) thread factory}
     * must provide a Thread that is suitable for performing the cleaning actions.
     * <p>
     * The cleaner terminates when it is phantom reachable and all of the
     * registered cleaning actions are complete.
     *
     * @param threadFactory a {@code ThreadFactory} to return a new {@code Thread}
     *                      to process cleaning actions
     * @return a new {@code Cleaner}
     *
     * @throws  IllegalThreadStateException  if the thread from the thread
     *               factory was {@link Thread.State#NEW not a new thread}.
     * @throws  SecurityException  if the current thread is not allowed to
     *               create or start the thread.
     */
    public static Cleaner create(ThreadFactory threadFactory) {
        Objects.requireNonNull(threadFactory, "threadFactory");
        Cleaner cleaner = new Cleaner();
        cleaner.impl.start(cleaner, threadFactory);
        return cleaner;
    }

    /**
     * Registers an object and a {@link Runnable} to run when the object
     * becomes phantom reachable.
     * Refer to the <a href="#compatible-cleaners">API Note</a> above for
     * cautions about the behavior of cleaning actions.
     *
     * @param obj   the object to monitor
     * @param action a {@code Runnable} to invoke when the object becomes phantom reachable
     * @return a {@code Cleanable} instance
     */
    public Cleanable register(Object obj, Runnable action) {
        Objects.requireNonNull(obj, "obj");
        Objects.requireNonNull(action, "action");
        return new CleanerImpl.PhantomCleanableRef(obj, this, action);
    }

    /**
     * {@code Cleanable} represents an object and a
     * cleaning action registered in a {@code Cleaner}.
     * @since 9
     */
    public interface Cleanable {
        /**
         * Unregisters the cleanable and invokes the cleaning action.
         * The cleanable's cleaning action is invoked at most once
         * regardless of the number of calls to {@code clean}.
         */
        void clean();
    }

}
