import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

@Configuration
class LibraryPathConfig {

    @PostConstruct
    fun setLibraryPath() {
        System.setProperty("java.library.path", "/opt/homebrew/lib")
    }
}