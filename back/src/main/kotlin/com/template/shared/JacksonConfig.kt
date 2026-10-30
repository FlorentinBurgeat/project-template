package com.template.shared

import com.fasterxml.jackson.databind.cfg.CoercionAction
import com.fasterxml.jackson.databind.cfg.CoercionInputShape
import com.fasterxml.jackson.databind.type.LogicalType
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Strict JSON typing: `allow-coercion-of-scalars: false` does not cover number/boolean -> String, so a text
 * field must refuse a JSON number or boolean explicitly (otherwise `"name": 123` is stored as "123").
 */
@Configuration
class JacksonConfig {

    @Bean
    fun strictTextCoercion() = Jackson2ObjectMapperBuilderCustomizer { builder ->
        builder.postConfigurer { mapper ->
            mapper.coercionConfigFor(LogicalType.Textual).apply {
                setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
            }
        }
    }
}
