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
package org.jdbi.v3.core.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Map;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Maps rows to {@link Map.Entry Map.Entry&lt;K, V&gt;}, provided there are mappers registered for types K and V. This mapper is registered out of the box.
 *
 * @param <K> entry key type
 * @param <V> entry value type
 */
public class MapEntryMapper<K, V> implements RowMapper<Map.Entry<K, V>> {
    private final RowMapper<K> keyMapper;
    private final RowMapper<V> valueMapper;

    // TODO move into Defaults package
    public MapEntryMapper(RowMapper<K> keyMapper, RowMapper<V> valueMapper) {
        this.keyMapper = keyMapper;
        this.valueMapper = valueMapper;
    }

    @Override
    public Map.Entry<K, V> map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new AbstractMap.SimpleImmutableEntry<>(keyMapper.map(rs, ctx), valueMapper.map(rs, ctx));
    }

    @Override
    public RowMapper<Map.Entry<K, V>> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        return new MapEntryMapper<>(keyMapper.specialize(rs, ctx), valueMapper.specialize(rs, ctx));
    }
}
