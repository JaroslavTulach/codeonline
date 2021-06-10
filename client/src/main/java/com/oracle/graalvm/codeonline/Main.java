package com.oracle.graalvm.codeonline;

import com.oracle.graalvm.codeonline.js.PlatformServices;
import net.java.html.boot.BrowserBuilder;
import net.java.html.lib.Objs;
import net.java.html.lib.codemirror.CodeMirror.EditorConfiguration;
import net.java.html.lib.dom.Element;
import net.java.html.lib.dom.HTMLTextAreaElement;
import net.java.html.lib.dom.NodeListOf;

/**
 * Desktop client entry point and common client code.
 */
public final class Main {
    private Main() {
        throw new UnsupportedOperationException();
    }

    public static void main(String... args) throws Exception {
        BrowserBuilder.newBrowser().
            loadPage("pages/index.html").
            loadClass(Main.class).
            invoke("onDesktopPageLoad", args).
            showAndWait();
        System.exit(0);
    }

    public static void onPageLoad(PlatformServices services) throws Exception {
        services.registerCodeMirrorModule();
        net.java.html.lib.codemirror.CodeMirror.Exports.registerHelper("hint", "clike", new JavaHintHelper());

        EditorConfiguration conf = new Objs().$cast(EditorConfiguration.class);
        conf.lineNumbers.set(true);
        conf.mode.set("text/x-java");
        conf.extraKeys.set(Objs.$as(Objs.create(null)).$set("Ctrl-Space", "autocomplete"));

        NodeListOf<Element> elems = net.java.html.lib.dom.Exports.document.getElementsByClassName("codeonline");
        for(Element element : new NodeListWrapper<>(elems, Element::$as)) {
            if(element.nodeName.get().equalsIgnoreCase("textarea"))
                net.java.html.lib.codemirror.CodeMirror.Exports.fromTextArea(element.$cast(HTMLTextAreaElement.class), conf);
            else
                throw new RuntimeException("HTML class codeonline is only applicable to textarea elements.");
        }
    }

    public static void onDesktopPageLoad() throws Exception {
        onPageLoad(new DesktopServices());
    }

    private static final class DesktopServices extends PlatformServices {
        // default behavior is enough for now
    }
}
