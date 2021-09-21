# GraalVM CodeOnline

## Build and try it

Building and running is JDK sensitive. One may need compatible JavaFX implementation,
one may need compatible JavaScript implementation for the JVM. These components
differ on OpenJDK, GraalVM, JDKs with JavaFX and between individual versions of
the JDK. Switching between JDKs for each part of the build is an option, but
to keep things simple let's start with JDK11 on Linux or Mac:

```bash
codeonline$ export JAVA_HOME=/jdk-11
codeonline$ mvn clean install
codeonline$ mvn -f live-doc-debug -P desktop clean process-classes exec:exec
codeonline$ mvn -f live-doc-test -P codeonline-compiler-aotjs,codeonline-editor-aotjs clean package bck2brwsr:show
codeonline$ mvn -f live-doc-test -P codeonline-compiler-bck2brwsr,codeonline-editor-bck2brwsr clean package bck2brwsr:show
codeonline$ mvn -f live-doc-test -P codeonline-compiler-aotjs,codeonline-editor-bck2brwsr clean package bck2brwsr:show
codeonline$ mvn -f live-doc-test -P codeonline-compiler-bck2brwsr,codeonline-editor-aotjs clean package bck2brwsr:show
```

The `exec:exec` command launches the project in JavaFX WebView emulator mode
suitable for debugging Java code in the selected JDK. Use...

```bash
codeonline$ mvn -f live-doc-debug -P desktop process-classes exec:exec \
  -Dexec.debug.arg=-agentlib:jdwp=transport=dt_socket,server=y,address=5005,suspend=y
```

...and connect your IDE to the `5005` port to track behavior of codeonline.
