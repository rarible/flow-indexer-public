import io.mongock.runner.springboot.EnableMongock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableMongock
class FlowMigrationsApplication

fun main(args: Array<String>) {
    runApplication<FlowMigrationsApplication>(*args)
}