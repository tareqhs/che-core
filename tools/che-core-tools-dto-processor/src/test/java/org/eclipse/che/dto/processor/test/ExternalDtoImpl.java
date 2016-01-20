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
package org.eclipse.che.dto.processor.test;

import org.eclipse.che.dto.processor.test.dtos3.ExternalDto;
import org.eclipse.che.dto.server.DtoProvider;
import org.eclipse.che.dto.server.JsonSerializable;
import org.eclipse.che.dto.shared.DTOImpl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * A test helper that helps registering dummy implementations for non-generated DTO interfaces that are used for testing
 * references to DTO interfaces that are generated in other projects.
 * 
 * @author Tareq Sharafy (tareq.sharafy@sap.com)
 */
@SuppressWarnings({ "serial" })
@DTOImpl("server")
public class ExternalDtoImpl implements ExternalDto, JsonSerializable {

    public static final DtoProvider<ExternalDto> DTO_PROVIDER = new DtoProvider<ExternalDto>() {

        @Override
        public Class<? extends ExternalDto> getImplClass() {
            return ExternalDtoImpl.class;
        }

        @Override
        public ExternalDto fromJson(String json) {
            return fromJson(new JsonParser().parse(json));
        }

        @Override
        public ExternalDto fromJson(JsonElement jsonElem) {
            if (jsonElem == null || jsonElem.isJsonNull()) {
                return null;
            }
            ExternalDtoImpl obj = new ExternalDtoImpl();
            JsonObject json = jsonElem.getAsJsonObject();
            if (json.has("blah")) {
                obj.blah = json.getAsJsonPrimitive("blah").getAsString();
            }
            return obj;
        }

        @Override
        public ExternalDto newInstance() {
            return new ExternalDtoImpl();
        }

        @Override
        public ExternalDto clone(ExternalDto origin) {
            ExternalDtoImpl obj = new ExternalDtoImpl();
            obj.blah = origin.getBlah();
            return obj;
        }
    };

    private String blah;

    @Override
    public String toJson() {
        return toJsonElement().toString();
    }

    @Override
    public JsonElement toJsonElement() {
        JsonObject obj = new JsonObject();
        obj.add("blah", new JsonPrimitive(blah));
        return obj;
    }

    @Override
    public String getBlah() {
        return blah;
    }

    @Override
    public void setBlah(String v) {
        this.blah = v;
    }

}
