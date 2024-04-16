package ir.yekaan.darkoobnext.utils

open class Singleton protected constructor() {
    var uiToken = "N/A"

    companion object {
        private var mInstance: Singleton? = null

        @get:Synchronized
        val instance: Singleton?
            get() {
                if (null == mInstance) {
                    mInstance = Singleton()
                }
                return mInstance!!
            }
    }
}