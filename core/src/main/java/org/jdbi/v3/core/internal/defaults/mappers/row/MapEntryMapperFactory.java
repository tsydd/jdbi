package org.jdbi.v3.core.internal.defaults.mappers.row;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Optional;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.MapEntryMapper;
import org.jdbi.v3.core.mapper.MapEntryMappers;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.mapper.SingleColumnMapper;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;
import static org.jdbi.v3.core.generic.GenericTypes.resolveType;

public class MapEntryMapperFactory implements RowMapperFactory {
    private final TypeVariable<Class<Map.Entry>> keyParam;
    private final TypeVariable<Class<Map.Entry>> valueParam;

    public MapEntryMapperFactory() {
        TypeVariable<Class<Map.Entry>>[] mapParams = Map.Entry.class.getTypeParameters();
        keyParam = mapParams[0];
        valueParam = mapParams[1];
    }

    @Override
    public Optional<RowMapper<?>> build(Type type, ConfigRegistry config) {
        if (type instanceof ParameterizedType && getErasedType(type).equals(Map.Entry.class)) {
            Type keyType = resolveType(keyParam, type);
            Type valueType = resolveType(valueParam, type);

            RowMapper<?> keyMapper = getKeyMapper(keyType, config);
            RowMapper<?> valueMapper = getValueMapper(valueType, config);

            return Optional.of(new MapEntryMapper<>(keyMapper, valueMapper));
        } else {
            return Optional.empty();
        }
    }

    private static RowMapper<?> getKeyMapper(Type keyType, ConfigRegistry config) {
        String column = config.get(MapEntryMappers.class).getKeyColumn();
        if (column == null) {
            return config.get(RowMappers.class)
                .findFor(keyType)
                .orElseThrow(() -> new NoSuchMapperException("No row mapper registered for map key " + keyType));
        } else {
            return config.get(ColumnMappers.class)
                .findFor(keyType)
                .map(mapper -> new SingleColumnMapper<>(mapper, column))
                .orElseThrow(() -> new NoSuchMapperException("No column mapper registered for map key " + keyType + " in column " + column));
        }
    }

    private static RowMapper<?> getValueMapper(Type valueType, ConfigRegistry config) {
        String column = config.get(MapEntryMappers.class).getValueColumn();
        if (column == null) {
            return config.get(RowMappers.class)
                .findFor(valueType)
                .orElseThrow(() -> new NoSuchMapperException("No row mapper registered for map value " + valueType));
        } else {
            return config.get(ColumnMappers.class)
                .findFor(valueType)
                .map(mapper -> new SingleColumnMapper<>(mapper, column))
                .orElseThrow(() -> new NoSuchMapperException("No column mapper registered for map value " + valueType + " in column " + column));
        }
    }
}
