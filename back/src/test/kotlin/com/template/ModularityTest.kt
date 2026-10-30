package com.template

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

class ModularityTest {

    @Test
    fun `modules have no cycle and only use each other's public API`() {
        ApplicationModules.of(Application::class.java).verify()
    }
}
