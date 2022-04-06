import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class AppPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.create("app", AppExtension::class, project)
    }
}