/*
 * Copyright (c) 2006, 2024, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6402062
 * @summary Tests MatteBorder encoding
 * @run main/othervm javax_swing_border_MatteBorder
 * @author Sergey Malenkov
 */

import java.awt.Color;
import javax.swing.border.MatteBorder;

public final class javax_swing_border_MatteBorder extends AbstractTest<MatteBorder> {
    public static void main(String[] args) {
        new javax_swing_border_MatteBorder().test();
    }

    protected MatteBorder getObject() {
        return new MatteBorder(1, 2, 3, 4, Color.RED);
    }

    protected MatteBorder getAnotherObject() {
        return null; // TODO: could not update property
        // return new MatteBorder(4, 3, 2, 1, Color.BLACK);
    }
}
