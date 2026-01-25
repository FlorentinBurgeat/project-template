package com.template

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

@SpringBootApplication(scanBasePackages = ["com.template", "features", "config"])
@ComponentScan(
    basePackages = ["com.template", "features", "config"],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ["features\\.authentication\\..*"]
        )
    ]
)
@ConfigurationPropertiesScan(basePackages = ["features"])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
