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
package org.eclipse.che.dto.processor.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.che.dto.server.DtoFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class PropertyHelpers {

    public interface PropertyHelper<T> {

        JsonElement toJson(T v);

        T fromJson(JsonElement json);

        T copy(T v);
    }

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public static <T> PropertyHelper<Map<String, T>> mapOf(final PropertyHelper<T> sub) {
        return new BasePropertyHelper<Map<String, T>>() {
            @Override
            protected JsonElement toNonNullJson(Map<String, T> v) {
                JsonObject obj = new JsonObject();
                for (Entry<String, T> entry : v.entrySet()) {
                    obj.add(entry.getKey(), sub.toJson(entry.getValue()));
                }
                return obj;
            }

            @Override
            protected Map<String, T> fromNonNullJson(JsonElement v) {
                Set<Entry<String, JsonElement>> entries = v.getAsJsonObject().entrySet();
                Map<String, T> map = new HashMap<>(entries.size());
                for (Entry<String, JsonElement> entry : entries) {
                    final String key = entry.getKey();
                    final JsonElement valueJson = entry.getValue();
                    T value = sub.fromJson(valueJson);
                    map.put(key, value);
                }
                return map;
            }

            @Override
            protected Map<String, T> copyNonNull(Map<String, T> v) {
                Map<String, T> map = new HashMap<>(v.size());
                for (Entry<String, T> entry : v.entrySet()) {
                    map.put(entry.getKey(), sub.copy(entry.getValue()));
                }
                return map;
            }

        };
    }

    public static <T> PropertyHelper<List<T>> listOf(final PropertyHelper<T> sub) {
        return new BasePropertyHelper<List<T>>() {
            @Override
            public JsonElement toNonNullJson(List<T> v) {
                JsonArray arr = new JsonArray();
                v.stream().map(sub::toJson).forEach(arr::add);
                return arr;
            }

            @Override
            protected List<T> fromNonNullJson(JsonElement v) {
                JsonArray jsonArray = v.getAsJsonArray();
                Stream<JsonElement> stm = StreamSupport.stream(jsonArray.spliterator(), false);
                return stm.map(sub::fromJson).collect(Collectors.toList());
            }

            @Override
            protected List<T> copyNonNull(List<T> v) {
                return v.stream().map(sub::copy).collect(Collectors.toList());
            }
        };
    }

    public static <T> PropertyHelper<T> dto(final Class<T> dtoType) {
        return new BasePropertyHelper<T>() {
            @Override
            public JsonElement toNonNullJson(T v) {
                return DtoFactory.getInstance().toJsonElement(v);
            }

            @Override
            protected T fromNonNullJson(JsonElement v) {
                return DtoFactory.getInstance().createDtoFromJson(v, dtoType);
            }

            @Override
            protected T copyNonNull(T v) {
                return DtoFactory.getInstance().clone(v);
            }
        };
    }

    public static <T> PropertyHelper<T> primitive(Function<T, JsonElement> toJson, Function<JsonElement, T> fromJSon) {
        return new BasePropertyHelper<T>() {
            @Override
            public JsonElement toNonNullJson(T v) {
                return toJson.apply(v);
            }

            @Override
            protected T fromNonNullJson(JsonElement v) {
                return fromJSon.apply(v);
            }

            @Override
            protected T copyNonNull(T v) {
                return v;
            }
        };
    }

    public static final PropertyHelper<String> PH_String = primitive(JsonPrimitive::new, JsonElement::getAsString);
    public static final PropertyHelper<Boolean> PH_Boolean = primitive(JsonPrimitive::new, JsonElement::getAsBoolean);
    public static final PropertyHelper<Byte> PH_Byte = primitive(JsonPrimitive::new, JsonElement::getAsByte);
    public static final PropertyHelper<Short> PH_Short = primitive(JsonPrimitive::new, JsonElement::getAsShort);
    public static final PropertyHelper<Integer> PH_Int = primitive(JsonPrimitive::new, JsonElement::getAsInt);
    public static final PropertyHelper<Long> PH_Long = primitive(JsonPrimitive::new, JsonElement::getAsLong);
    public static final PropertyHelper<Double> PH_Double = primitive(JsonPrimitive::new, JsonElement::getAsDouble);
    public static final PropertyHelper<Float> PH_Float = primitive(JsonPrimitive::new, JsonElement::getAsFloat);

    public static <T extends Enum<T>> PropertyHelper<T> enumeration(final Class<T> clazz) {
        return new BasePropertyHelper<T>() {
            @Override
            public JsonElement toNonNullJson(T v) {
                return new JsonPrimitive(v.name());
            }

            @Override
            protected T fromNonNullJson(JsonElement v) {
                return gson.fromJson(v, clazz);
            }

            @Override
            protected T copyNonNull(T v) {
                return v;
            }
        };
    }

    public static PropertyHelper<Object> any() {
        return new BasePropertyHelper<Object>() {
            @Override
            public JsonElement toNonNullJson(Object v) {
                return (v instanceof JsonElement) ? cloneJsonElement((JsonElement) v) : JsonNull.INSTANCE;
            }

            @Override
            protected Object fromNonNullJson(JsonElement v) {
                return cloneJsonElement(v);
            }

            @Override
            protected Object copyNonNull(Object v) {
                return (v instanceof JsonElement) ? cloneJsonElement((JsonElement) v) : v;
            }
        };
    }

    private static abstract class BasePropertyHelper<T> implements PropertyHelper<T> {

        @Override
        public final JsonElement toJson(T v) {
            if (v == null) {
                return JsonNull.INSTANCE;
            }
            return toNonNullJson(v);
        }

        protected abstract JsonElement toNonNullJson(T v);

        @Override
        public final T fromJson(JsonElement json) {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            return fromNonNullJson(json);
        }

        protected abstract T fromNonNullJson(JsonElement v);

        @Override
        public final T copy(T v) {
            if (v == null) {
                return null;
            }
            return copyNonNull(v);
        }

        protected abstract T copyNonNull(T v);

    }

    private static JsonElement cloneJsonElement(JsonElement json) {
        if (json instanceof JsonObject) {
            JsonObject obj = new JsonObject();
            for (Entry<String, JsonElement> entry : ((JsonObject) json).entrySet()) {
                obj.add(entry.getKey(), cloneJsonElement(entry.getValue()));
            }
            return obj;
        }
        if (json instanceof JsonArray) {
            JsonArray arr = new JsonArray();
            for (JsonElement elm : (JsonArray) json) {
                arr.add(cloneJsonElement(elm));
            }
        }
        return json;
    }

}
