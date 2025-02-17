/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
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

package org.jdrupes.mdoclet.internal.doclets.formats.html;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import org.jdrupes.mdoclet.internal.doclets.toolkit.AbstractDoclet;
import org.jdrupes.mdoclet.internal.doclets.toolkit.DocletException;
import org.jdrupes.mdoclet.internal.doclets.toolkit.Messages;
import org.jdrupes.mdoclet.internal.doclets.toolkit.builders.AbstractBuilder;
import org.jdrupes.mdoclet.internal.doclets.toolkit.builders.BuilderFactory;
import org.jdrupes.mdoclet.internal.doclets.toolkit.util.ClassTree;
import org.jdrupes.mdoclet.internal.doclets.toolkit.util.DeprecatedAPIListBuilder;
import org.jdrupes.mdoclet.internal.doclets.toolkit.util.DocFile;
import org.jdrupes.mdoclet.internal.doclets.toolkit.util.DocFileIOException;
import org.jdrupes.mdoclet.internal.doclets.toolkit.util.DocPath;
import org.jdrupes.mdoclet.internal.doclets.toolkit.util.DocPaths;
import org.jdrupes.mdoclet.internal.doclets.toolkit.util.IndexBuilder;
import org.jdrupes.mdoclet.internal.doclets.toolkit.util.NewAPIBuilder;
import org.jdrupes.mdoclet.internal.doclets.toolkit.util.PreviewAPIListBuilder;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

/**
 * The class with "start" method, calls individual Writers.
 */
public class HtmlDoclet extends AbstractDoclet {

    /**
     * Creates a doclet to generate HTML documentation,
     * specifying the "initiating doclet" to be used when
     * initializing any taglets for this doclet.
     * An initiating doclet is one that delegates to
     * this doclet.
     *
     * @param initiatingDoclet the initiating doclet
     */
    public HtmlDoclet(Doclet initiatingDoclet) {
        this.initiatingDoclet = initiatingDoclet;
    }

    @Override // defined by Doclet
    public String getName() {
        return "Html";
    }

    /**
     * The initiating doclet, to be specified when creating
     * the configuration.
     */
    private final Doclet initiatingDoclet;

    /**
     * The global configuration information for this run.
     * Initialized in {@link #init(Locale, Reporter)}.
     */
    private HtmlConfiguration configuration;

    /**
     * Object for generating messages and diagnostics.
     */
    private Messages messages;

    /**
     * Base path for resources for this doclet.
     */
    private static final DocPath DOCLET_RESOURCES = DocPath
        .create("/jdk/javadoc/internal/doclets/formats/html/resources");

    @Override // defined by Doclet
    public void init(Locale locale, Reporter reporter) {
        configuration
            = new HtmlConfiguration(initiatingDoclet, locale, reporter);
        messages = configuration.getMessages();
    }

    /**
     * Create the configuration instance.
     * Override this method to use a different
     * configuration.
     *
     * @return the configuration
     */
    @Override // defined by AbstractDoclet
    public HtmlConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    protected Function<String, String>
            getResourceKeyMapper(DocletEnvironment docEnv) {
        SourceVersion sv = docEnv.getSourceVersion();
        Map<String, String> map = new HashMap<>();
        String[][] pairs = {
            // in standard.properties
            { "doclet.Enum_Hierarchy", "doclet.Enum_Class_Hierarchy" },
            { "doclet.Annotation_Type_Hierarchy",
                "doclet.Annotation_Interface_Hierarchy" },
            { "doclet.Href_Annotation_Title",
                "doclet.Href_Annotation_Interface_Title" },
            { "doclet.Href_Enum_Title", "doclet.Href_Enum_Class_Title" },
            { "doclet.Annotation_Types", "doclet.Annotation_Interfaces" },
            { "doclet.Annotation_Type_Members",
                "doclet.Annotation_Interface_Members" },
            { "doclet.annotation_types", "doclet.annotation_interfaces" },
            { "doclet.annotation_type_members",
                "doclet.annotation_interface_members" },
            { "doclet.help.enum.intro", "doclet.help.enum.class.intro" },
            { "doclet.help.annotation_type.intro",
                "doclet.help.annotation_interface.intro" },
            { "doclet.help.annotation_type.declaration",
                "doclet.help.annotation_interface.declaration" },
            { "doclet.help.annotation_type.description",
                "doclet.help.annotation_interface.description" },

            // in doclets.properties
            { "doclet.Enums", "doclet.EnumClasses" },
            { "doclet.AnnotationType", "doclet.AnnotationInterface" },
            { "doclet.AnnotationTypes", "doclet.AnnotationInterfaces" },
            { "doclet.annotationtype", "doclet.annotationinterface" },
            { "doclet.annotationtypes", "doclet.annotationinterfaces" },
            { "doclet.Enum", "doclet.EnumClass" },
            { "doclet.enum", "doclet.enumclass" },
            { "doclet.enums", "doclet.enumclasses" },
            { "doclet.Annotation_Type_Member",
                "doclet.Annotation_Interface_Member" },
            { "doclet.enum_values_doc.fullbody",
                "doclet.enum_class_values_doc.fullbody" },
            { "doclet.enum_values_doc.return",
                "doclet.enum_class_values_doc.return" },
            { "doclet.enum_valueof_doc.fullbody",
                "doclet.enum_class_valueof_doc.fullbody" },
            { "doclet.enum_valueof_doc.throws_ila",
                "doclet.enum_class_valueof_doc.throws_ila" },
            { "doclet.search.types", "doclet.search.classes_and_interfaces" }
        };
        for (String[] pair : pairs) {
            if (sv.compareTo(SourceVersion.RELEASE_16) >= 0) {
                map.put(pair[0], pair[1]);
            } else {
                map.put(pair[1], pair[0]);
            }
        }
        return (k) -> map.getOrDefault(k, k);
    }

    @Override // defined by AbstractDoclet
    public void generateClassFiles(ClassTree classTree) throws DocletException {
        List<String> since = configuration.getOptions().since();
        if (!(configuration.getOptions().noDeprecated()
            || configuration.getOptions().noDeprecatedList())) {
            DeprecatedAPIListBuilder deprecatedBuilder
                = new DeprecatedAPIListBuilder(configuration, since);
            if (!deprecatedBuilder.isEmpty()) {
                configuration.deprecatedAPIListBuilder = deprecatedBuilder;
                configuration.conditionalPages
                    .add(HtmlConfiguration.ConditionalPage.DEPRECATED);
            }
        }
        if (!since.isEmpty()) {
            NewAPIBuilder newAPIBuilder
                = new NewAPIBuilder(configuration, since);
            if (!newAPIBuilder.isEmpty()) {
                configuration.newAPIPageBuilder = newAPIBuilder;
                configuration.conditionalPages
                    .add(HtmlConfiguration.ConditionalPage.NEW);
            }
        }
        PreviewAPIListBuilder previewBuilder
            = new PreviewAPIListBuilder(configuration);
        if (!previewBuilder.isEmpty()) {
            configuration.previewAPIListBuilder = previewBuilder;
            configuration.conditionalPages
                .add(HtmlConfiguration.ConditionalPage.PREVIEW);
        }

        super.generateClassFiles(classTree);
    }

    /**
     * Start the generation of files. Call generate methods in the individual
     * writers, which will in turn generate the documentation files. Call the
     * TreeWriter generation first to ensure the Class Hierarchy is built
     * first and then can be used in the later generation.
     *
     * For new format.
     *
     * @throws DocletException if there is a problem while writing the other files
     */
    @Override // defined by AbstractDoclet
    protected void generateOtherFiles(ClassTree classTree)
            throws DocletException {
        super.generateOtherFiles(classTree);
        HtmlOptions options = configuration.getOptions();
        if (options.linkSource()) {
            SourceToHTMLConverter.convertRoot(configuration,
                DocPaths.SOURCE_OUTPUT);
        }
        // Modules with no documented classes may be specified on the
        // command line to specify a service provider, allow these.
        if (configuration.getSpecifiedModuleElements().isEmpty() &&
            configuration.topFile.isEmpty()) {
            messages.error("doclet.No_Non_Deprecated_Classes_To_Document");
            return;
        }
        boolean nodeprecated = options.noDeprecated();
        performCopy(options.helpFile(), DocPath.empty);
        performCopy(options.stylesheetFile(), DocPath.empty);
        for (String stylesheet : options.additionalStylesheets()) {
            performCopy(stylesheet, DocPath.empty);
        }
        for (String script : options.additionalScripts()) {
            performCopy(script, DocPaths.SCRIPT_DIR);
        }
        // do early to reduce memory footprint
        if (options.classUse()) {
            ClassUseWriter.generate(configuration, classTree);
        }

        if (options.createTree()) {
            TreeWriter.generate(configuration, classTree);
        }

        if (configuration.conditionalPages
            .contains((HtmlConfiguration.ConditionalPage.DEPRECATED))) {
            DeprecatedListWriter.generate(configuration);
        }

        if (configuration.conditionalPages
            .contains((HtmlConfiguration.ConditionalPage.PREVIEW))) {
            PreviewListWriter.generate(configuration);
        }

        if (configuration.conditionalPages
            .contains((HtmlConfiguration.ConditionalPage.NEW))) {
            NewAPIListWriter.generate(configuration);
        }

        if (options.createOverview()) {
            if (configuration.showModules) {
                ModuleIndexWriter.generate(configuration);
            } else {
                PackageIndexWriter.generate(configuration);
            }
        }

        if (options.createIndex()) {
            if (!options.noExternalSpecsPage()) {
                ExternalSpecsWriter.generate(configuration);
            }
            SystemPropertiesWriter.generate(configuration);
            configuration.mainIndex.addElements();
            IndexBuilder allClassesIndex
                = new IndexBuilder(configuration, nodeprecated, true);
            allClassesIndex.addElements();
            AllClassesIndexWriter.generate(configuration, allClassesIndex);
            if (!configuration.packages.isEmpty()) {
                AllPackagesIndexWriter.generate(configuration);
            }
            configuration.mainIndex.createSearchIndexFiles();
            IndexWriter.generate(configuration);
            SearchWriter.generate(configuration);
        }

        if (options.createOverview()) {
            IndexRedirectWriter.generate(configuration,
                DocPaths.OVERVIEW_SUMMARY, DocPaths.INDEX);
        } else {
            IndexRedirectWriter.generate(configuration);
        }

        if (options.helpFile().isEmpty() && !options.noHelp()) {
            HelpWriter.generate(configuration);
        }
        // If a stylesheet file is not specified, copy the default stylesheet
        // and replace newline with platform-specific newline.
        DocFile f;
        if (options.stylesheetFile().length() == 0) {
            f = DocFile.createFileForOutput(configuration, DocPaths.STYLESHEET);
            f.copyResource(DocPaths.RESOURCES.resolve(DocPaths.STYLESHEET),
                true, true);
        }
        f = DocFile.createFileForOutput(configuration, DocPaths.JAVASCRIPT);
        f.copyResource(DocPaths.RESOURCES.resolve(DocPaths.JAVASCRIPT), true,
            true);
        f = DocFile.createFileForOutput(configuration, DocPaths.CLIPBOARD_SVG);
        f.copyResource(DocPaths.RESOURCES.resolve(DocPaths.CLIPBOARD_SVG), true,
            true);
        f = DocFile.createFileForOutput(configuration, DocPaths.LINK_SVG);
        f.copyResource(DocPaths.RESOURCES.resolve(DocPaths.LINK_SVG), true,
            true);
        if (options.createIndex()) {
            f = DocFile.createFileForOutput(configuration, DocPaths.SEARCH_JS);
            f.copyResource(
                DOCLET_RESOURCES.resolve(DocPaths.SEARCH_JS_TEMPLATE),
                configuration.docResources);

            f = DocFile.createFileForOutput(configuration,
                DocPaths.SEARCH_PAGE_JS);
            f.copyResource(DOCLET_RESOURCES.resolve(DocPaths.SEARCH_PAGE_JS),
                configuration.docResources);

            f = DocFile.createFileForOutput(configuration,
                DocPaths.RESOURCES.resolve(DocPaths.GLASS_IMG));
            f.copyResource(DOCLET_RESOURCES.resolve(DocPaths.GLASS_IMG), true,
                false);

            f = DocFile.createFileForOutput(configuration,
                DocPaths.RESOURCES.resolve(DocPaths.X_IMG));
            f.copyResource(DOCLET_RESOURCES.resolve(DocPaths.X_IMG), true,
                false);
            copyJqueryFiles();
        }

        copyLegalFiles(options.createIndex());
    }

    private void copyJqueryFiles() throws DocletException {
        List<String> files = Arrays.asList(
            DocPaths.JQUERY_JS.getPath(),
            DocPaths.JQUERY_UI_JS.getPath(),
            DocPaths.JQUERY_UI_CSS.getPath());
        DocFile f;
        for (String file : files) {
            DocPath filePath = DocPaths.SCRIPT_DIR.resolve(file);
            f = DocFile.createFileForOutput(configuration, filePath);
            f.copyResource(DOCLET_RESOURCES.resolve(filePath), true, false);
        }
    }

    private void copyLegalFiles(boolean includeJQuery) throws DocletException {
        Path legalNoticesDir;
        String legalNotices = configuration.getOptions().legalNotices();
        switch (legalNotices) {
        case "", "default" -> {
            if (getClass().getModule().getName() == null) {
                return;
            }
            Path javaHome = Path.of(System.getProperty("java.home"));
            legalNoticesDir = javaHome.resolve("legal")
                .resolve(getClass().getModule().getName());
        }

        case "none" -> {
            return;
        }

        default -> {
            try {
                legalNoticesDir = Path.of(legalNotices);
            } catch (InvalidPathException e) {
                messages.error("doclet.Error_invalid_path_for_legal_notices",
                    legalNotices, e.getMessage());
                return;
            }
        }
        }

        if (Files.exists(legalNoticesDir)) {
            try (DirectoryStream<Path> ds
                = Files.newDirectoryStream(legalNoticesDir)) {
                for (Path entry : ds) {
                    if (!Files.isRegularFile(entry)) {
                        continue;
                    }
                    if (entry.getFileName().toString().startsWith("jquery")
                        && !includeJQuery) {
                        continue;
                    }
                    DocPath filePath = DocPaths.LEGAL
                        .resolve(entry.getFileName().toString());
                    DocFile df
                        = DocFile.createFileForOutput(configuration, filePath);
                    df.copyFile(
                        DocFile.createFileForInput(configuration, entry));
                }
            } catch (IOException e) {
                messages.error("doclet.Error_copying_legal_notices", e);
            }
        }
    }

    @Override // defined by AbstractDoclet
    protected void generateClassFiles(SortedSet<TypeElement> typeElems,
            ClassTree classTree)
            throws DocletException {
        BuilderFactory f = configuration.getBuilderFactory();
        for (TypeElement te : typeElems) {
            if (utils.hasHiddenTag(te) ||
                !(configuration.isGeneratedDoc(te) && utils.isIncluded(te))) {
                continue;
            }
            f.getClassBuilder(te, classTree).build();
        }
    }

    @Override // defined by AbstractDoclet
    protected void generateModuleFiles() throws DocletException {
        if (configuration.showModules) {
            List<ModuleElement> mdles
                = new ArrayList<>(configuration.modulePackages.keySet());
            for (ModuleElement mdle : mdles) {
                AbstractBuilder moduleSummaryBuilder = configuration
                    .getBuilderFactory().getModuleSummaryBuilder(mdle);
                moduleSummaryBuilder.build();
            }
        }
    }

    @Override // defined by AbstractDoclet
    protected void generatePackageFiles(ClassTree classTree)
            throws DocletException {
        HtmlOptions options = configuration.getOptions();
        Set<PackageElement> packages = configuration.packages;
        List<PackageElement> pList = new ArrayList<>(packages);
        for (PackageElement pkg : pList) {
            // if -nodeprecated option is set and the package is marked as
            // deprecated, do not generate the package-summary.html,
            // package-frame.html
            // and package-tree.html pages for that package.
            if (!(options.noDeprecated() && utils.isDeprecated(pkg))) {
                AbstractBuilder packageSummaryBuilder = configuration
                    .getBuilderFactory().getPackageSummaryBuilder(pkg);
                packageSummaryBuilder.build();
                if (options.createTree()) {
                    PackageTreeWriter.generate(configuration, pkg,
                        options.noDeprecated());
                }
            }
        }
    }

    @Override // defined by Doclet
    public Set<? extends Option> getSupportedOptions() {
        return configuration.getOptions().getSupportedOptions();
    }

    private void performCopy(String filename, DocPath targetPath)
            throws DocFileIOException {
        if (filename.isEmpty()) {
            return;
        }

        DocFile fromfile = DocFile.createFileForInput(configuration, filename);
        DocPath path = targetPath.resolve(fromfile.getName());
        DocFile toFile = DocFile.createFileForOutput(configuration, path);
        if (toFile.isSameFile(fromfile)) {
            return;
        }

        messages.notice("doclet.Copying_File_0_To_File_1",
            fromfile.getPath(), path.getPath());
        toFile.copyFile(fromfile);
    }
}
