package meow0x7e.dol

import kotlinx.serialization.Serializable
import meow0x7e.dol.plugins.AbstractAddonPlugin

/**
 * 表示一个依赖 `Mod` 的信息
 *
 * @property modName [String] 依赖的 `Mod` 的名称，用于标识需要被依赖的 `Mod`
 * @property version [String] 依赖的 `Mod` 的版本号，指定了所需的最低或特定版本
 */
@Serializable
data class DependenceInfo(
    /** 依赖的 `Mod` 名称 */
    val modName: String,

    /** 依赖的 `Mod` 版本 */
    val version: String
) {
    /**
     * 从一个现有的 [AbstractAddonPlugin] 上获取数据来构建一个 [DependenceInfo]
     *
     * @param addonPlugin 一个 [AbstractAddonPlugin] 对象，将被用于初始化 [DependenceInfo]
     */
    constructor(addonPlugin: AbstractAddonPlugin) : this(
        modName = addonPlugin.modName,
        version = addonPlugin.modVersion
    )
}