package config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

@SpringBootApplication(
    exclude = [
        DataSourceAutoConfiguration::class,
        SecurityAutoConfiguration::class
    ]
)
@ComponentScan(
    basePackages = ["features.authentication.controller", "features.authentication.dto", "exception"],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ["config\\.SecurityConfig", "security\\..*"]
        )
    ]
)
class TestApplication
