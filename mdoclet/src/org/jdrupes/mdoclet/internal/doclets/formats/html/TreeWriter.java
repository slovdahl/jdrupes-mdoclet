/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.SortedSet;

import javax.lang.model.element.PackageElement;

import org.jdrupes.mdoclet.internal.doclets.formats.html.Navigation.PageMode;
import org.jdrupes.mdoclet.internal.doclets.formats.html.markup.BodyContents;
import org.jdrupes.mdoclet.internal.doclets.formats.html.markup.ContentBuilder;
import org.jdrupes.mdoclet.internal.doclets.formats.html.markup.HtmlStyle;
import org.jdrupes.mdoclet.internal.doclets.formats.html.markup.HtmlTree;
import org.jdrupes.mdoclet.internal.doclets.toolkit.Content;
import org.jdrupes.mdoclet.internal.doclets.toolkit.util.ClassTree;
import org.jdrupes.mdoclet.internal.doclets.toolkit.util.DocFileIOException;
import org.jdrupes.mdoclet.internal.doclets.toolkit.util.DocPath;
import org.jdrupes.mdoclet.internal.doclets.toolkit.util.DocPaths;

/**
 * Generate Class Hierarchy page for all the Classes in this run.  Use
 * ClassTree for building the Tree. The name of
 * the generated file is "overview-tree.html" and it is generated in the
 * current or the destination directory.
 */
public class TreeWriter extends AbstractTreeWriter {

    /**
     * Packages in this run.
     */
    SortedSet<PackageElement> packages;

    /**
     * True if there are no packages specified on the command line,
     * False otherwise.
     */
    private final boolean classesOnly;

    protected BodyContents bodyContents;

    /**
     * Constructor to construct TreeWriter object.
     *
     * @param configuration the current configuration of the doclet.
     * @param filename String filename
     * @param classTree the tree being built.
     */
    public TreeWriter(HtmlConfiguration configuration, DocPath filename,
            ClassTree classTree) {
        super(configuration, filename, classTree);
        packages = configuration.packages;
        classesOnly = packages.isEmpty();
        this.bodyContents = new BodyContents();
    }

    /**
     * Create a TreeWriter object and use it to generate the
     * "overview-tree.html" file.
     *
     * @param configuration the configuration for this doclet
     * @param classTree the class tree being documented.
     * @throws  DocFileIOException if there is a problem generating the overview tree page
     */
    public static void generate(HtmlConfiguration configuration,
            ClassTree classTree) throws DocFileIOException {
        DocPath filename = DocPaths.OVERVIEW_TREE;
        TreeWriter treegen = new TreeWriter(configuration, filename, classTree);
        treegen.generateTreeFile();
    }

    /**
     * Generate the interface hierarchy and class hierarchy.
     *
     * @throws DocFileIOException if there is a problem generating the overview tree page
     */
    public void generateTreeFile() throws DocFileIOException {
        HtmlTree body = getBody();
        Content headContent = contents.hierarchyForAllPackages;
        var heading = HtmlTree.HEADING(Headings.PAGE_TITLE_HEADING,
            HtmlStyle.title, headContent);
        var div = HtmlTree.DIV(HtmlStyle.header, heading);
        Content mainContent = new ContentBuilder();
        mainContent.add(div);
        addPackageTreeLinks(mainContent);
        addTree(classTree.classes(), "doclet.Class_Hierarchy", mainContent);
        addTree(classTree.interfaces(), "doclet.Interface_Hierarchy",
            mainContent);
        addTree(classTree.annotationInterfaces(),
            "doclet.Annotation_Type_Hierarchy", mainContent);
        addTree(classTree.enumClasses(), "doclet.Enum_Hierarchy", mainContent);
        addTree(classTree.recordClasses(), "doclet.Record_Class_Hierarchy",
            mainContent);
        body.add(bodyContents
            .addMainContent(mainContent)
            .setFooter(getFooter()));
        printHtmlDocument(null, "class tree", body);
    }

    /**
     * Add the links to all the package tree files.
     *
     * @param content the content to which the links will be added
     */
    protected void addPackageTreeLinks(Content content) {
        // Do nothing if only unnamed package is used
        if (isUnnamedPackage()) {
            return;
        }
        if (!classesOnly) {
            var span = HtmlTree.SPAN(HtmlStyle.packageHierarchyLabel,
                contents.packageHierarchies);
            content.add(span);
            var ul = HtmlTree.UL(HtmlStyle.horizontal)
                .addStyle(HtmlStyle.contentsList);
            int i = 0;
            for (PackageElement pkg : packages) {
                // If the package name length is 0 or if -nodeprecated option
                // is set and the package is marked as deprecated, do not
                // include
                // the page in the list of package hierarchies.
                if (pkg.isUnnamed() ||
                    (options.noDeprecated() && utils.isDeprecated(pkg))) {
                    i++;
                    continue;
                }
                DocPath link = pathString(pkg, DocPaths.PACKAGE_TREE);
                var li = HtmlTree.LI(links.createLink(link,
                    getLocalizedPackageName(pkg)));
                if (i < packages.size() - 1) {
                    li.add(", ");
                }
                ul.add(li);
                i++;
            }
            content.add(ul);
        }
    }

    /**
     * {@return a new HTML BODY element}
     */
    private HtmlTree getBody() {
        String title = resources.getText("doclet.Window_Class_Hierarchy");
        HtmlTree bodyTree = getBody(getWindowTitle(title));
        bodyContents.setHeader(getHeader(PageMode.TREE));
        return bodyTree;
    }

    private boolean isUnnamedPackage() {
        return packages.size() == 1 && packages.first().isUnnamed();
    }
}
