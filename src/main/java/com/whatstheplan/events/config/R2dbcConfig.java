package com.whatstheplan.events.config;

import io.r2dbc.postgresql.codec.Interval;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Configuration
public class R2dbcConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new DurationToIntervalConverter());
        converters.add(new IntervalToDurationConverter());

        SimpleTypeHolder simpleTypeHolder = new SimpleTypeHolder(Set.of(UUID.class), true);

        CustomConversions.StoreConversions storeConversions =
                CustomConversions.StoreConversions.of(simpleTypeHolder, Collections.emptyList());
        return new R2dbcCustomConversions(storeConversions, converters);
    }

    @ReadingConverter
    public static class IntervalToDurationConverter implements Converter<Interval, Duration> {

        @Override
        public Duration convert(Interval source) {
            return source.getDuration();
        }
    }

    @WritingConverter
    public static class DurationToIntervalConverter implements Converter<Duration, Interval> {

        @Override
        public Interval convert(Duration source) {
            return Interval.from(source);
        }
    }
}