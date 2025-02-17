A Doclet that enables Markdown in JavaDoc comments. 

MDoclet
=======

The {@link org.jdrupes.mdoclet.MDoclet} converts all JavaDoc
documentation to HTML using a configurable Markdown processor
([flexmark-java](https://github.com/vsch/flexmark-java) by default)

Basically, the doclet intercepts the standard doclet's access to the AST 
generated by the javadoc tool and converts the texts from the comments 
from markdown to HTML.

Javadoc Tags
------------

The following known tags are processed as Markdown:

 * `@author`
 * `@deprecated`
 * `@param`
 * `@return`
 * `@since`
 * `@throws`
 * `@version`

### `@see` Tags

The `@see` tag is a special case, as there are several variants of this tag. 
These two variants will remain unchanged:

 * Javadoc-Links: `@see Foo#bar()`
 * Links: `@see <a href="http://www.example.com/">Example</a>`

The third variant however, which is originally meant to refer to a printed book, may
also contain Markdown-style links:

 * `@see "[Example](http://www.example.com/)"`

This is rendered as `@see <a href="http://www.example.com/">LABEL</a>`, where
LABEL falls back to the link's URL, if no label is given.

### Inline Tags

Inline tags will be removed before processing the Markdown source and 
re-inserted afterwards. Therefore, Markdown within inline tags won't work.

Syntax Highlighting
-------------------

MDoclet integrates
[highlight.js](http://softwaremaniacs.org/soft/highlight/en/) to enable syntax
highlighting for fenced blocks.

Invoking
--------

Specify the Doclet on JavaDoc's command line:

<div class="tab-nav" data-tab-group="usage">
  <button class="selected" data-tab-name="v3" 
    onclick="openTab(event)">v3.x / Java-17</button>
  <button data-tab-name="v4" 
    onclick="openTab(event)">v4.x / Java-21</button>
</div>

<div class="tab-content" style="display: block;" data-tab-group="usage" data-tab-name="v3">

```sh
javadoc -J--add-exports=jdk.javadoc/jdk.javadoc.internal.tool=ALL-UNNAMED \
    -J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
    -doclet org.jdrupes.mdoclet.MDoclet -docletpath /path/to/org.jdrupes.mdoclet.jar:another.jar
```

</div>

<div class="tab-content" style="display: none;" data-tab-group="usage" data-tab-name="v4">

```sh
javadoc -J--add-exports=jdk.compiler/com.sun.tools.doclint=ALL-UNNAMED \
    -J--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
    -J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
    -J--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
    -J--add-exports=jdk.javadoc/jdk.javadoc.internal.tool=ALL-UNNAMED \
    -J--add-exports=jdk.javadoc/jdk.javadoc.internal.doclets.toolkit=ALL-UNNAMED \
    -J--add-opens=jdk.javadoc/jdk.javadoc.internal.doclets.toolkit.resources.releases=ALL-UNNAMED \
    -doclet org.jdrupes.mdoclet.MDoclet -docletpath /path/to/org.jdrupes.mdoclet.jar:another.jar
```

</div>

A prebuilt version can be downloaded from Maven Central
(use the JAR with the suffix "-all" for a JAR file that includes all dependencies).

`--markdown-processor`
:   Specify the Markdown processor, see below.

`-overview <file>`
:   Specify an overview page. This is an option from the standard doclet.
    If the file name ends with ".md", the file will be converted by 
    the Markdown processor.

`-highlight-style <style>`
:   The style to be used for syntax highlighting.

`-disable-highlight`
:   Disable syntax highlighting entirely.

`-disable-auto-highlight`
:   Disable auto-highlighting. If no language is specified for a fenced block, 
    the highlighter will not try to guess the correct language. This option has
    to be implemented by the Markdown processor.

### Gradle

Because the standard doclet relies on an implementation class instead of
the interface `DocletEnvironment`, a module must be made accessible. This
can only be done when forking a new JVM, which is not supported by the
gradle JavaDoc task (see the 
[corresponding issue](https://github.com/gradle/gradle/issues/16602)). Until
this is fixed, the only way to run the doclet is by using a JavaExec
task.

<div class="tab-nav" data-tab-group="usage">
  <button class="selected" data-tab-name="v3" 
    onclick="openTab(event)">v3.x / Java-17</button>
  <button data-tab-name="v4" 
    onclick="openTab(event)">v4.x / Java-21</button>
</div>

<div class="tab-content" style="display: block;" data-tab-group="usage" data-tab-name="v3">

```gradle
configurations {
    markdownDoclet
}

dependencies {
    markdownDoclet "org.jdrupes.mdoclet:doclet:3.0.0"
}

task apidocs(type: JavaExec) {
    enabled = JavaVersion.current() == JavaVersion.VERSION_17

    dependsOn classes
    inputs.file "overview.md"

    jvmArgs = ['--add-exports=jdk.javadoc/jdk.javadoc.internal.tool=ALL-UNNAMED',
        '--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED']
    classpath sourceSets.main.compileClasspath
    main = 'jdk.javadoc.internal.tool.Main'
    args = ['-doctitle', "My Code",
        '-overview', "overview.md",
        '-use',
        '-linksource',
        '-link', 'https://docs.oracle.com/en/java/javase/17/docs/api/',
        '--add-exports', 'jdk.javadoc/jdk.javadoc.internal.tool=ALL-UNNAMED',
        '--add-exports', 'jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED',
        '-doclet', 'org.jdrupes.mdoclet.MDoclet',
        '-docletpath', configurations.markdownDoclet.files.asType(List).join(":"),
        '-d', file("${project.buildDir}/javadoc"),
        // Specify sources to be processed
        ]
}
```

</div>

<div class="tab-content" style="display: none;" data-tab-group="usage" data-tab-name="v4"><code class="language-gradle">

```gradle
configurations {
    markdownDoclet
}

dependencies {
    markdownDoclet "org.jdrupes.mdoclet:doclet:4.1.0"
}

task testJavadoc(type: Javadoc) {
    enabled = JavaVersion.current() == JavaVersion.VERSION_21

    source = fileTree(dir: 'testfiles', include: '**/*.java')
    destinationDir = project.file("build/testfiles-gradle")
    options.docletpath = configurations.markdownDoclet.files.asType(List)
    options.doclet = 'org.jdrupes.mdoclet.MDoclet'
    options.overview = 'testfiles/overview.md'
    options.addStringOption('Xdoclint:-html', '-quiet')

    options.setJFlags([
        '--add-exports=jdk.compiler/com.sun.tools.doclint=ALL-UNNAMED',
        '--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED',
        '--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED',
        '--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED',
        '--add-exports=jdk.javadoc/jdk.javadoc.internal.tool=ALL-UNNAMED',
        '--add-exports=jdk.javadoc/jdk.javadoc.internal.doclets.toolkit=ALL-UNNAMED',
        '--add-opens=jdk.javadoc/jdk.javadoc.internal.doclets.toolkit.resources.releases=ALL-UNNAMED'])
}
```

</div>

The latest version available on maven central is shown in the badge on the 
[project page](https://github.com/mnlipp/jdrupes-mdoclet).

### Maven
 
I don't use maven, but according to 
[this contribution](https://github.com/mnlipp/jdrupes-mdoclet/issues/11#issuecomment-1446857303) (and updates provided by others)
it should be sufficient to add the following to your pom:

<div class="tab-nav" data-tab-group="usage">
  <button class="selected" data-tab-name="v3" 
    onclick="openTab(event)">v3.x / Java-17</button>
  <button data-tab-name="v4" 
    onclick="openTab(event)">v4.x / Java-21</button>
</div>

<div class="tab-content" style="display: block;" data-tab-group="usage" data-tab-name="v3">

```xml
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-javadoc-plugin</artifactId>
    &lt;!-- do not use 3.5.1: transitive dependencies of docletArtifact are not added to 
         docletpath, version 3.5.1 resolves this issue. https://issues.apache.org/jira/browse/MJAVADOC-742 --&gt;
    <version>3.4.1</version>
    <executions>
      <execution>
        <id>attach-sources</id>
        <goals>
            <goal>jar</goal>
            <goal>test-jar</goal>
        </goals>
      </execution>
    </executions>
    <configuration>
      <useStandardDocletOptions>true</useStandardDocletOptions>
      <doclet>org.jdrupes.mdoclet.MDoclet</doclet>
      <docletArtifacts>
        <docletArtifact>
          <groupId>org.jdrupes.mdoclet</groupId>
          <artifactId>doclet</artifactId>
          &lt;!-- version 2.2.0 for java 11 and version 1.0.10 for java 8 --&gt;
          <version>3.1.0</version>
        </docletArtifact>
        <docletArtifact>
          <groupId>com.vladsch.flexmark</groupId>
          <artifactId>flexmark-all</artifactId>
          <version>0.64.0</version>   
        </docletArtifact>   
      </docletArtifacts>      
      &lt;!--  Note: additionalDependencies are added to the -classpath, not the docletpath --&gt;
      <additionalDependencies>
      </additionalDependencies>
      <additionalJOptions>
        <additionalJOption>-J--add-exports=jdk.javadoc/jdk.javadoc.internal.tool=ALL-UNNAMED</additionalJOption>
        <additionalJOption>-J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED</additionalJOption>
      </additionalJOptions>
    </configuration>
  </plugin>
```

</div>

<div class="tab-content" style="display: none;" data-tab-group="usage" data-tab-name="v4">

```xml
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-javadoc-plugin</artifactId>
    &lt;!-- do not use 3.5.1: transitive dependencies of docletArtifact are not added to 
         docletpath, version 3.5.1 resolves this issue. https://issues.apache.org/jira/browse/MJAVADOC-742 --&gt;
    <version>3.4.1</version>
    <executions>
      <execution>
        <id>attach-sources</id>
        <goals>
            <goal>jar</goal>
            <goal>test-jar</goal>
        </goals>
      </execution>
    </executions>
    <configuration>
      <useStandardDocletOptions>true</useStandardDocletOptions>
      <doclet>org.jdrupes.mdoclet.MDoclet</doclet>
      <docletArtifacts>
        <docletArtifact>
          <groupId>org.jdrupes.mdoclet</groupId>
          <artifactId>doclet</artifactId>
          &lt;!-- version 2.2.0 for java 11 and version 1.0.10 for java 8 --&gt;
          <version>3.1.0</version>
        </docletArtifact>
        <docletArtifact>
          <groupId>com.vladsch.flexmark</groupId>
          <artifactId>flexmark-all</artifactId>
          <version>0.64.0</version>   
        </docletArtifact>   
      </docletArtifacts>      
      &lt;!--  Note: additionalDependencies are added to the -classpath, not the docletpath --&gt;
      <additionalDependencies>
      </additionalDependencies>
      <additionalJOptions>
        <additionalJOption>-J--add-exports=jdk.javadoc/jdk.javadoc.internal.tool=ALL-UNNAMED</additionalJOption>
        <additionalJOption>-J--add-exports=jdk.javadoc/jdk.javadoc.internal.doclets.toolkit=ALL-UNNAMED</additionalJOption>
        <additionalJOption>-J--add-opens=jdk.javadoc/jdk.javadoc.internal.doclets.toolkit.resources.releases=ALL-UNNAMED</additionalJOption>            
        <additionalJOption>-J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED</additionalJOption>
        <additionalJOption>-J--add-exports=jdk.compiler/com.sun.tools.doclint=ALL-UNNAMED</additionalJOption>
        <additionalJOption>-J--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED</additionalJOption>
        <additionalJOption>-J--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED</additionalJOption>
      </additionalJOptions>
    </configuration>
  </plugin>
```

</div>

Selecting a Markdown processor
------------------------------

The doclet accesses the Markdown processor using the interface
{@link org.jdrupes.mdoclet.MarkdownProcessor}. If you want to use another
Markdown processor than the default flexmark-java processor, you must provide
an adapter class that implements the interface and has a default (no parameters) 
constructor. To make the doclet use your class, supply its fully qualified class 
name as parameter to the option `--markdown-processor`. The class 
(and all its dependencies) must be in the doclet classpath.

The default behavior is equivalent to "``--markdown-processor 
{@link org.jdrupes.mdoclet.processors.FlexmarkProcessor 
org.jdrupes.mdoclet.processors.FlexmarkProcessor}``".
 
Configuring the Markdown processor
----------------------------------
 
Markdown processors may support further configuration. As the available options
are unknown to this doclet, it uses a "flag forwarding" mechanism. The 
argument of flag `-M` is forwarded to the Markdown processor. E.g. 
"`-M -profile=kramdown`" is passed to the Markdown processor as "`-profile=kramdown`".
The option may be used multiple times.
 
The flags supported by the default Markdown processor can be found in the 
description of its {@linkplain org.jdrupes.mdoclet.processors.FlexmarkProcessor 
adapter class}.

Notes
-----

While based on JDK 1.8 (doclet version < 2.0), this project was an 
architectural redesign and extension of [Abnaxos'](https://github.com/Abnaxos) 
great [pegdown-doclet](https://github.com/Abnaxos/pegdown-doclet).
Aside from making the Markdown processor configurable, the PlantUML
functionality had been factored out in a 
[project of its own](https://github.com/mnlipp/jdrupes-taglets). 

Starting with doclet version 2.0.0, this project is an independent
development based on the API introduced in JDK 9.

This Doclet is released under the
[AGPL 3.0](http://www.gnu.org/licenses/#AGPL).

The project's sources can be found on 
[GitHub](https://github.com/mnlipp/jdrupes-mdoclet).
