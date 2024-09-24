package meow0x7e.dol.plugins

import kotlinx.serialization.Serializable

@Serializable
sealed class AbstractAddonPlugin {
    /** 插件来源 `Mod` */
    abstract val modName: String

    /** 插件来源 `Mod` 所注册的插件名称 */
    abstract val addonName: String

    /** 插件所用 `Mod` 的版本 */
    abstract val modVersion: String

    /** 插件参数 */
    abstract val params: Any
}