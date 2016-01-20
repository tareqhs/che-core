/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.dto.processor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.eclipse.che.dto.processor.GeneratorData.ImplClass;
import org.eclipse.che.dto.server.DtoFactoryVisitor;
import org.eclipse.che.dto.shared.DTO;
import org.stringtemplate.v4.ST;

@SupportedOptions({ DtoAnnotationProcessor.OPT_PACKAGES, DtoAnnotationProcessor.OPT_GENCLASS,
        DtoAnnotationProcessor.OPT_IMPL })
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class DtoAnnotationProcessor extends AbstractProcessor {

    public static final String OPT_PACKAGES = "dto.packages";
    public static final String OPT_GENCLASS = "dto.genclass";
    public static final String OPT_IMPL = "dto.impl";

    private static final Pattern QNAME_PATTERN = Pattern.compile("^(\\w+(\\.\\w+)*)\\.(\\w+)$");
    private static final String VISITOR_SERVICE_RESOURCE = "META-INF/services/" + DtoFactoryVisitor.class.getName();

    private Messager messager;
    private Set<PackageElement> packagesToScan;
    private DtoAnalyzer analyzer;
    private DtoGenerator generator;
    private GeneratorData data;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(DTO.class.getName());
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        final Elements elementUtils = processingEnv.getElementUtils();
        // Extract names of packages to scan
        String packages = processingEnv.getOptions().get(OPT_PACKAGES);
        if (packages != null) {
            String[] rawValues = packages.split(",");
            packagesToScan = Arrays.stream(rawValues).map(String::trim).map(elementUtils::getPackageElement)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
        }
        if (packagesToScan == null || packagesToScan.isEmpty()) {
            messager.printMessage(Kind.WARNING, "Missing value for option -A" + OPT_PACKAGES);
            return;
        }
        // Target implementation
        final String implName = processingEnv.getOptions().get(OPT_IMPL);
        switch (implName) {
        case "server":
            generator = new ServerImpl(processingEnv);
            break;
        default:
            messager.printMessage(Kind.WARNING, "Unknown DTO implementation: " + implName);
            return;
        }
        // Target class name
        data = new GeneratorData();
        String targetClassNameVal = processingEnv.getOptions().get(OPT_GENCLASS);
        if (targetClassNameVal == null) {
            messager.printMessage(Kind.ERROR, "Missing value for option -A" + OPT_GENCLASS);
            return;
        }
        Matcher m = QNAME_PATTERN.matcher(targetClassNameVal);
        if (!m.matches()) {
            messager.printMessage(Kind.WARNING,
                    "Invalid value for option -A" + OPT_GENCLASS + ": " + targetClassNameVal);
            return;
        }
        data.packageName = m.group(1);
        data.containerClass = m.group(3);
        // Helper objects
        analyzer = new DtoAnalyzer(processingEnv, packagesToScan);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (analyzer == null || roundEnv.errorRaised() || roundEnv.processingOver()) {
            return false;
        }
        // Get which classes to scan
        boolean hasDtos = processRound(roundEnv);
        if (hasDtos) {
            try {
                generateResults();
            } catch (IOException e) {
                messager.printMessage(Kind.ERROR, e.getMessage());
            }
        }
        return true;
    }

    private boolean processRound(RoundEnvironment roundEnv) {
        boolean claimed = false;
        for (Element element : roundEnv.getElementsAnnotatedWith(DTO.class)) {
            // Make sure it is an interface
            if (element.getKind() != ElementKind.INTERFACE) {
                messager.printMessage(Kind.WARNING, "Not an interface: " + element.getSimpleName(), element);
            }
            // Process this interface
            ImplClass implClass = analyzer.analyzeInterface((TypeElement) element);
            if (implClass != null) {
                data.classes.add(implClass);
                claimed = true;
            } else {
                messager.printMessage(Kind.WARNING, "Ignoring DTO interface outside allowed pacakges", element);
            }
        }
        return claimed;
    }

    private void generateResults() throws IOException {
        Filer filer = processingEnv.getFiler();
        // Generate the source code
        ST contents = generator.generate(data);
        // Write the target class file
        final String containerQName = data.packageName + "." + data.containerClass;
        JavaFileObject targetClassObj = filer.createSourceFile(containerQName);
        try (OutputStream output = targetClassObj.openOutputStream()) {
            byte[] bytes = contents.render().getBytes(StandardCharsets.UTF_8);
            output.write(bytes);
        }
        // Add the providers
        FileObject res1 = filer.createResource(StandardLocation.CLASS_OUTPUT, "", VISITOR_SERVICE_RESOURCE);
        try (OutputStream output = res1.openOutputStream()) {
            PrintStream ps = new PrintStream(output);
            ps.print(containerQName);
            ps.flush();
        }
    }

}
