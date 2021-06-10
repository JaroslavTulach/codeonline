package com.oracle.graalvm.codeonline;

import com.oracle.graalvm.codeonline.js.PlatformServices;

/**
 * Web browser entry point.
 */
public class BrowserMain {
    private BrowserMain() {
        throw new UnsupportedOperationException();
    }

    public static void main(String... args) throws Exception {
        Main.onPageLoad(new HTML5Services());
    }

    private static final class HTML5Services extends PlatformServices {
        // default behavior is enough for now
    }
}
