/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.core.mapper.reflect.internal;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class BeanTaster implements Function<Type, Optional<PojoProperties<?>>> {

    private static final String TYPE_NOT_INSTANTIABLE =
        "A bean, %s, was mapped which was not instantiable";

    private static final String MISSING_SETTER =
        "No appropriate method to write property %s";

    private static final String SETTER_NOT_ACCESSIBLE =
        "Unable to access setter for property, %s";

    private static final String INVOCATION_TARGET_EXCEPTION =
        "Invocation target exception trying to invoker setter for the %s property";

    @Override
    public Optional<PojoProperties<?>> apply(Type t) {
        return Optional.of(new BeanPojoProperties<>(t));
    }

    static class BeanPojoProperties<T> extends PojoProperties<T> {
        private final BeanInfo info;
        private final Map<String, PojoProperty<T>> properties;

        BeanPojoProperties(Type type) {
            super(type);
            try {
                this.info = Introspector.getBeanInfo(GenericTypes.getErasedType(type));
            } catch (IntrospectionException e) {
                // BeanTaster is the fallback and throws rather than chaining on.
                throw new IllegalArgumentException("Failed to inspect bean " + type, e);
            }
            final Map<String, PojoProperty<T>> props = new LinkedHashMap<>();
            for (PropertyDescriptor property : info.getPropertyDescriptors()) {
                final BeanPojoProperty<T> bp = new BeanPojoProperty<>(property);
                props.put(bp.getName(), bp);
            }
            properties = Collections.unmodifiableMap(props);
        }

        @Override
        public Map<String, PojoProperty<T>> getProperties() {
            return properties;
        }

        @SuppressWarnings("unchecked")
        @Override
        public PojoBuilder<T> create() {
            final Class<?> type = GenericTypes.getErasedType(getType());
            final T instance;
            try {
                instance = (T) type.newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format(TYPE_NOT_INSTANTIABLE, type.getName()), e);
            }
            return new PojoBuilder<T>() {
                @Override
                public T build() {
                    return instance;
                }
            };
        }

        class BeanPojoProperty<T> implements PojoProperty<T> {

            private final PropertyDescriptor property;

            BeanPojoProperty(PropertyDescriptor property) {
                this.property = property;
            }

            @Override
            public String getName() {
                return Stream.of(property.getReadMethod(), property.getWriteMethod())
                        .filter(Objects::nonNull)
                        .map(method -> method.getAnnotation(ColumnName.class))
                        .filter(Objects::nonNull)
                        .map(ColumnName::value)
                        .findFirst()
                        .orElseGet(property::getName);
            }

            @Override
            public Type getType() {
                return Optional.ofNullable(property.getReadMethod()).map(Method::getGenericReturnType)
                        .orElseGet(() -> property.getWriteMethod().getGenericParameterTypes()[0]);
            }

            @Override
            public <A extends Annotation> Optional<A> getAnnotation(Class<A> anno) {
                return Stream.of(property.getReadMethod(), property.getWriteMethod())
                        .filter(Objects::nonNull)
                        .map(m -> m.getAnnotation(anno))
                        .filter(Objects::nonNull)
                        .findFirst();
            }

            @Override
            public Object get(T pojo) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(PojoBuilder<T> builder, Object value) {
                try {
                    Method writeMethod = property.getWriteMethod();
                    if (writeMethod == null) {
                        throw new IllegalArgumentException(String.format(MISSING_SETTER, property));
                    }
                    writeMethod.invoke(builder.build(), value);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(String.format(SETTER_NOT_ACCESSIBLE, property), e);
                } catch (InvocationTargetException e) {
                    throw new IllegalArgumentException(String.format(INVOCATION_TARGET_EXCEPTION, property), e);
                }
            }
        }
    }
}
