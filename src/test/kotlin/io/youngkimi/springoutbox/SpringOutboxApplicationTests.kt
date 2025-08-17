package io.youngkimi.springoutbox

import io.youngkimi.springoutbox.data.entity.TaskDependency
import io.youngkimi.springoutbox.data.repository.TaskDepRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.config.Task

@SpringBootTest
class SpringOutboxApplicationTests {

    @Autowired
    private lateinit var taskDepRepository: TaskDepRepository

    @Test
    fun contextLoads() {

        val list: List<TaskDependency> = taskDepRepository.findAll();

    }

}
