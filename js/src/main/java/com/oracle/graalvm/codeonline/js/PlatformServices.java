package com.oracle.graalvm.codeonline.js;

import net.java.html.lib.Modules;
import net.java.html.lib.Objs;

public class PlatformServices {
    public void registerCodeMirrorModule() {
        new Modules.Provider() {
            @Override
            protected Objs find(String string) {
                if(string.equals("CodeMirror"))
                    return net.java.html.lib.codemirror.Exports.$as(net.java.html.lib.Exports.eval("CodeMirror"));
                return null;
            }
        };
    }
}
