import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

open class AppExtension(project: Project, objects: ObjectFactory, layout: ProjectLayout) {

    val mainClass: Property<String> = objects.property<String>()
    val shadow: Property<AppShadowing> =
        objects.property<AppShadowing>().convention(AppShadowing.NONE)
    val skipExclusion: Property<Boolean> = objects.property<Boolean>().convention(false)

    fun shadowFirstLevel() {
        shadow.set(AppShadowing.FIRST_LEVEL)
    }

    fun shadowFull() {
        shadow.set(AppShadowing.FULL)
    }

    fun skipExclusion() {
        skipExclusion.set(true)
    }
}