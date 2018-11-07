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
package org.jdbi.v3.core.internal.defaults;

import java.util.Arrays;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.JavaTimeZoneIdArgumentFactory;
import org.jdbi.v3.core.array.SqlArrayArgumentFactory;
import org.jdbi.v3.core.array.SqlArrayMapperFactory;
import org.jdbi.v3.core.internal.defaults.arguments.BoxedArgumentFactory;
import org.jdbi.v3.core.internal.defaults.arguments.EnumArgumentFactory;
import org.jdbi.v3.core.internal.defaults.arguments.EssentialsArgumentFactory;
import org.jdbi.v3.core.internal.defaults.arguments.InternetArgumentFactory;
import org.jdbi.v3.core.internal.defaults.arguments.JavaTimeArgumentFactory;
import org.jdbi.v3.core.internal.defaults.arguments.OptionalArgumentFactory;
import org.jdbi.v3.core.internal.defaults.arguments.PrimitivesArgumentFactory;
import org.jdbi.v3.core.internal.defaults.arguments.SqlArgumentFactory;
import org.jdbi.v3.core.internal.defaults.arguments.SqlTimeArgumentFactory;
import org.jdbi.v3.core.internal.defaults.arguments.UntypedNullArgumentFactory;
import org.jdbi.v3.core.internal.defaults.collectors.ArrayCollectorFactory;
import org.jdbi.v3.core.internal.defaults.collectors.EnumSetCollectorFactory;
import org.jdbi.v3.core.internal.defaults.collectors.ListCollectorFactory;
import org.jdbi.v3.core.internal.defaults.collectors.MapCollectorFactory;
import org.jdbi.v3.core.internal.defaults.collectors.OptionalCollectorFactory;
import org.jdbi.v3.core.internal.defaults.collectors.OptionalPrimitiveCollectorFactory;
import org.jdbi.v3.core.internal.defaults.collectors.SetCollectorFactory;
import org.jdbi.v3.core.internal.defaults.mappers.column.BoxedMapperFactory;
import org.jdbi.v3.core.internal.defaults.mappers.column.EssentialsMapperFactory;
import org.jdbi.v3.core.internal.defaults.mappers.column.InternetMapperFactory;
import org.jdbi.v3.core.internal.defaults.mappers.column.JavaTimeMapperFactory;
import org.jdbi.v3.core.internal.defaults.mappers.column.OptionalMapperFactory;
import org.jdbi.v3.core.internal.defaults.mappers.column.PrimitiveMapperFactory;
import org.jdbi.v3.core.internal.defaults.mappers.column.SqlTimeMapperFactory;
import org.jdbi.v3.core.internal.defaults.mappers.row.MapEntryMapperFactory;
import org.jdbi.v3.core.mapper.CaseStrategy;
import org.jdbi.v3.core.mapper.EnumMapperFactory;
import org.jdbi.v3.core.mapper.MapMappers;
import org.jdbi.v3.core.mapper.reflect.CaseInsensitiveColumnNameMatcher;
import org.jdbi.v3.core.mapper.reflect.ReflectionMappers;
import org.jdbi.v3.core.mapper.reflect.SnakeCaseColumnNameMatcher;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.core.statement.ColonPrefixSqlParser;
import org.jdbi.v3.core.statement.DefinedAttributeTemplateEngine;
import org.jdbi.v3.core.statement.SqlStatements;

public class JdbiDefaultsPlugin implements JdbiPlugin {
    @Override
    public void customizeJdbi(Jdbi jdbi) {
        installArguments(jdbi);

        installColumnMappers(jdbi);
        installRowMappers(jdbi);
        configureMappers(jdbi);

        installCollectors(jdbi);

        configureStatements(jdbi);
    }

    private static void installArguments(Jdbi jdbi) {
        // the null factory must be interrogated last to preserve types!
        jdbi.registerArgument(new UntypedNullArgumentFactory());

        jdbi.registerArgument(new InternetArgumentFactory());
        jdbi.registerArgument(new JavaTimeZoneIdArgumentFactory());
        jdbi.registerArgument(new JavaTimeArgumentFactory());
        jdbi.registerArgument(new OptionalArgumentFactory());

        jdbi.registerArgument(new SqlArgumentFactory());
        jdbi.registerArgument(new SqlArrayArgumentFactory());
        jdbi.registerArgument(new SqlTimeArgumentFactory());

        jdbi.registerArgument(new EssentialsArgumentFactory());
        jdbi.registerArgument(new EnumArgumentFactory());
        jdbi.registerArgument(new BoxedArgumentFactory());
        jdbi.registerArgument(new PrimitivesArgumentFactory());
    }

    private static void installColumnMappers(Jdbi jdbi) {
        jdbi.registerColumnMapper(new InternetMapperFactory());
        jdbi.registerColumnMapper(new JavaTimeMapperFactory());
        jdbi.registerColumnMapper(new OptionalMapperFactory());

        jdbi.registerColumnMapper(new SqlTimeMapperFactory());
        jdbi.registerColumnMapper(new SqlArrayMapperFactory());

        jdbi.registerColumnMapper(new EssentialsMapperFactory());
        jdbi.registerColumnMapper(new EnumMapperFactory());
        jdbi.registerColumnMapper(new BoxedMapperFactory());
        jdbi.registerColumnMapper(new PrimitiveMapperFactory());
    }

    private static void installRowMappers(Jdbi jdbi) {
        jdbi.registerRowMapper(new MapEntryMapperFactory());
    }

    private static void configureMappers(Jdbi jdbi) {
        // TODO law of least surprise?
        jdbi.getConfig(MapMappers.class).setCaseChange(CaseStrategy.LOCALE_LOWER);

        jdbi.getConfig(ReflectionMappers.class).setColumnNameMatchers(Arrays.asList(
            new CaseInsensitiveColumnNameMatcher(),
            new SnakeCaseColumnNameMatcher()
        ));
    }

    private static void installCollectors(Jdbi jdbi) {
        jdbi.registerCollector(new MapCollectorFactory());

        jdbi.registerCollector(new ListCollectorFactory());
        jdbi.registerCollector(new ArrayCollectorFactory());

        jdbi.registerCollector(new SetCollectorFactory());
        jdbi.registerCollector(new EnumSetCollectorFactory());

        jdbi.registerCollector(new OptionalCollectorFactory());
        jdbi.registerCollector(new OptionalPrimitiveCollectorFactory());
    }

    private static void configureStatements(Jdbi jdbi) {
        jdbi.getConfig(SqlStatements.class)
            .setTemplateEngine(new DefinedAttributeTemplateEngine())
            .setSqlParser(new ColonPrefixSqlParser());
    }
}
