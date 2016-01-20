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

import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.annotation.processing.ProcessingEnvironment;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public class ServerImpl implements DtoGenerator {

    private final ProcessingEnvironment env;
    private STGroup templateGroup;

    public ServerImpl(ProcessingEnvironment env) {
        this.env = env;
    }

    @Override
    public ST generate(GeneratorData data) {
        return getTemplate().add("data", data);
    }

    private ST getTemplate() {
        if (templateGroup == null) {
            URL in = getClass().getResource("/template_server.txt");
            templateGroup = new STGroupFile(in, StandardCharsets.UTF_8.name(), '<', '>');
            templateGroup.load();
        }
        return templateGroup.getInstanceOf("impl_file");
    }

}
