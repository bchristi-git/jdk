/*
 * Copyright (c) 2012, 2025, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7021614 8078320 8132096 8273244 8352249
 * @summary extend com.sun.source API to support parsing javadoc comments
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build DocCommentTester
 * @run main DocCommentTester FirstSentenceTest.java
 * @run main DocCommentTester -useBreakIterator FirstSentenceTest.java
 */

class FirstSentenceTest {
    /** */
    void empty() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: empty
  body: empty
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: empty
  body: empty
  block tags: empty
]
*/

    /** abc def ghi */
    void no_terminator() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi]
  body: empty
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi]
  body: empty
  block tags: empty
]
*/

    /**
     * abc def ghi.
     */
    void no_body() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi.]
  body: empty
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi.]
  body: empty
  block tags: empty
]
*/
    /**
     * abc def ghi. jkl mno pqr.
     */
    void dot_space() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi.]
  body: 1
    Text[TEXT, pos:13, jkl_mno_pqr.]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi._jkl_mno_pqr.]
  body: empty
  block tags: empty
]
*/
    /**
     * abc def ghi.
     * jkl mno pqr
     */
    void dot_newline() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi.]
  body: 1
    Text[TEXT, pos:13, jkl_mno_pqr]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi.|jkl_mno_pqr]
  body: empty
  block tags: empty
]
*/
    /**
     * abc def ghi.
     * Jkl mno pqr
     */
    void dot_newline_upper() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi.]
  body: 1
    Text[TEXT, pos:13, Jkl_mno_pqr]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi.]
  body: 1
    Text[TEXT, pos:13, Jkl_mno_pqr]
  block tags: empty
]
*/
    /**
     * abc def ghi
     * <p>jkl mno pqr
     */
    void dot_p() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi]
  body: 2
    StartElement[START_ELEMENT, pos:12
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:15, jkl_mno_pqr]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi]
  body: 2
    StartElement[START_ELEMENT, pos:12
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:15, jkl_mno_pqr]
  block tags: empty
]
*/
    /**
     *
     * <p>abc def ghi.
     * jdl mno pqf
     */
    void newline_p() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 2
    StartElement[START_ELEMENT, pos:1
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:4, abc_def_ghi.]
  body: 1
    Text[TEXT, pos:17, jdl_mno_pqf]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:1
  firstSentence: 2
    StartElement[START_ELEMENT, pos:1
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:4, abc_def_ghi.|jdl_mno_pqf]
  body: empty
  block tags: empty
]
*/
    /**
     *
     * <p>abc def ghi.
     * Jdl mno pqf
     */
    void newline_p_upper() { }
/*
DocComment[DOC_COMMENT, pos:1
  firstSentence: 2
    StartElement[START_ELEMENT, pos:1
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:4, abc_def_ghi.]
  body: 1
    Text[TEXT, pos:17, Jdl_mno_pqf]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:1
  firstSentence: 2
    StartElement[START_ELEMENT, pos:1
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:4, abc_def_ghi.]
  body: 1
    Text[TEXT, pos:17, Jdl_mno_pqf]
  block tags: empty
]
*/
    /**
     * abc def ghi
     * </p>jkl mno pqr
     */
    void dot_end_p() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi]
  body: 2
    EndElement[END_ELEMENT, pos:12, p]
    Text[TEXT, pos:16, jkl_mno_pqr]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi]
  body: 2
    EndElement[END_ELEMENT, pos:12, p]
    Text[TEXT, pos:16, jkl_mno_pqr]
  block tags: empty
]
*/
    /**
     * abc &lt; ghi. jkl mno pqr.
     */
    void entity() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 3
    Text[TEXT, pos:0, abc_]
    Entity[ENTITY, pos:4, lt]
    Text[TEXT, pos:8, _ghi.]
  body: 1
    Text[TEXT, pos:14, jkl_mno_pqr.]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 3
    Text[TEXT, pos:0, abc_]
    Entity[ENTITY, pos:4, lt]
    Text[TEXT, pos:8, _ghi._jkl_mno_pqr.]
  body: empty
  block tags: empty
]
*/
    /**
     * abc {@code code} ghi. jkl mno pqr.
     */
    void inline_tag() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 3
    Text[TEXT, pos:0, abc_]
    Literal[CODE, pos:4, code]
    Text[TEXT, pos:16, _ghi.]
  body: 1
    Text[TEXT, pos:22, jkl_mno_pqr.]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 3
    Text[TEXT, pos:0, abc_]
    Literal[CODE, pos:4, code]
    Text[TEXT, pos:16, _ghi._jkl_mno_pqr.]
  body: empty
  block tags: empty
]
*/
    /**
     * abc def ghi
     * @author jjg
     */
    void block_tag() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi]
  body: empty
  block tags: 1
    Author[AUTHOR, pos:12
      name: 1
        Text[TEXT, pos:20, jjg]
    ]
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc_def_ghi]
  body: empty
  block tags: 1
    Author[AUTHOR, pos:12
      name: 1
        Text[TEXT, pos:20, jjg]
    ]
]
*/
    /**
     * @author jjg
     */
    void just_tag() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: empty
  body: empty
  block tags: 1
    Author[AUTHOR, pos:0
      name: 1
        Text[TEXT, pos:8, jjg]
    ]
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: empty
  body: empty
  block tags: 1
    Author[AUTHOR, pos:0
      name: 1
        Text[TEXT, pos:8, jjg]
    ]
]
*/
    /**
     * <p> abc def.
     * ghi jkl
     */
    void p_at_zero() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 2
    StartElement[START_ELEMENT, pos:0
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:3, _abc_def.]
  body: 1
    Text[TEXT, pos:13, ghi_jkl]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 2
    StartElement[START_ELEMENT, pos:0
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:3, _abc_def.|ghi_jkl]
  body: empty
  block tags: empty
]
*/
    /**
     * <p> abc def.
     * Ghi jkl
     */
    void p_at_zero_upper() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 2
    StartElement[START_ELEMENT, pos:0
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:3, _abc_def.]
  body: 1
    Text[TEXT, pos:13, Ghi_jkl]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 2
    StartElement[START_ELEMENT, pos:0
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:3, _abc_def.]
  body: 1
    Text[TEXT, pos:13, Ghi_jkl]
  block tags: empty
]
*/

    /**
     * abc <p> def. ghi jkl
     */
    void p_at_nonzero() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc]
  body: 2
    StartElement[START_ELEMENT, pos:4
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:7, _def._ghi_jkl]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    Text[TEXT, pos:0, abc]
  body: 2
    StartElement[START_ELEMENT, pos:4
      name:p
      attributes: empty
    ]
    Text[TEXT, pos:7, _def._ghi_jkl]
  block tags: empty
]
*/
    ///Abc.
    ///Def.
    void simpleMarkdown() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    RawText[MARKDOWN, pos:0, Abc.]
  body: 1
    RawText[MARKDOWN, pos:5, Def.]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    RawText[MARKDOWN, pos:0, Abc.]
  body: 1
    RawText[MARKDOWN, pos:5, Def.]
  block tags: empty
]
*/

    ///Abc `p.q` def.
    ///Ghi.
    void markdownWithCodeSpan() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    RawText[MARKDOWN, pos:0, Abc_`p.q`_def.]
  body: 1
    RawText[MARKDOWN, pos:15, Ghi.]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    RawText[MARKDOWN, pos:0, Abc_`p.q`_def.]
  body: 1
    RawText[MARKDOWN, pos:15, Ghi.]
  block tags: empty
]
*/

    ///Abc {@code p.q} def.
    ///Ghi.
    void markdownWithCodeTag() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 3
    RawText[MARKDOWN, pos:0, Abc_]
    Literal[CODE, pos:4, p.q]
    RawText[MARKDOWN, pos:15, _def.]
  body: 1
    RawText[MARKDOWN, pos:21, Ghi.]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 3
    RawText[MARKDOWN, pos:0, Abc_]
    Literal[CODE, pos:4, p.q]
    RawText[MARKDOWN, pos:15, _def.]
  body: 1
    RawText[MARKDOWN, pos:21, Ghi.]
  block tags: empty
]
*/

    ///Abc <a href="example.com">example</a> def.
    ///Ghi.
    void markdownWithHtml() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    RawText[MARKDOWN, pos:0, Abc_<a_href="example.com">example</a>_def.]
  body: 1
    RawText[MARKDOWN, pos:43, Ghi.]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    RawText[MARKDOWN, pos:0, Abc_<a_href="example.com">example</a>_def.]
  body: 1
    RawText[MARKDOWN, pos:43, Ghi.]
  block tags: empty
]
*/

    ///Abc [example.com][example] def.
    ///Ghi.
    void markdownWithLinks() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    RawText[MARKDOWN, pos:0, Abc_[example.com][example]_def.]
  body: 1
    RawText[MARKDOWN, pos:32, Ghi.]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    RawText[MARKDOWN, pos:0, Abc_[example.com][example]_def.]
  body: 1
    RawText[MARKDOWN, pos:32, Ghi.]
  block tags: empty
]
*/

    ///Abc
    ///
    ///Def.
    void markdownEndParaNoPeriod() { }
/*
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    RawText[MARKDOWN, pos:0, Abc]
  body: 1
    RawText[MARKDOWN, pos:5, Def.]
  block tags: empty
]
*/
/*
BREAK_ITERATOR
DocComment[DOC_COMMENT, pos:0
  firstSentence: 1
    RawText[MARKDOWN, pos:0, Abc]
  body: 1
    RawText[MARKDOWN, pos:5, Def.]
  block tags: empty
]
*/
}

