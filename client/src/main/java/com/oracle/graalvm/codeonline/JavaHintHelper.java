package com.oracle.graalvm.codeonline;

import net.java.html.lib.Function;
import net.java.html.lib.codemirror.CodeMirror.Doc;
import net.java.html.lib.codemirror.CodeMirror.Position;
import net.java.html.lib.codemirror.showhint.CodeMirror.Hints;
import net.java.html.lib.codemirror.showhint.CodeMirror.ShowHintOptions;

/**
 * Implements a CodeMirror hint helper using javac.
 */
public class JavaHintHelper implements Function.A2<Doc, ShowHintOptions, Hints> {
    @Override
    public Hints call(Doc doc, ShowHintOptions opts) {
        Position cursor = doc.getCursor();
        String line = doc.getLine(cursor.line().intValue());
        int col = cursor.ch().intValue();
        int start = col, end = col;
        while(start > 0 && Character.isJavaIdentifierPart(line.charAt(start - 1)))
            start--;
        while(end < line.length() && Character.isJavaIdentifierPart(line.charAt(end)))
            end++;
        // TODO ask javac
        System.out.println("CodeOnline: Hint requested: " + line.substring(start, col) + "|" + line.substring(col, end));
        return null;
    }
}
