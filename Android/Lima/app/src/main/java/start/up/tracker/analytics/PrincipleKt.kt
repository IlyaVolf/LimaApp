package start.up.tracker.analytics

abstract class PrincipleKt(
    val name: String,
    val description: String
) {
    abstract var isEnabled: Boolean
    //val name: String

    fun setStatus(isEnabled: Boolean) {
        this.isEnabled = isEnabled
    }

    fun getStatus(): Boolean {
        return isEnabled
    }

    abstract fun logic()
}