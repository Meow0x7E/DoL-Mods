package meow0x7e.dol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/**
 * 表示一个插件的信息
 *
 * @property modName [String] 插件来源的 `Mod` 的名称
 * @property addonName [String] `Mod` 所注册的插件名称
 * @property modVersion [String] 插件所用 `Mod` 的版本号
 * @property params [JsonElement] 插件参数，以 [JsonElement] 形式存储，默认为 [JsonNull]
 */
@Serializable
data class AddonPlugin(
    /** 插件来源 `Mod` */
    val modName: String,

    /** 插件来源 `Mod` 所注册的插件名称 */
    val addonName: String,

    /** 插件所用 `Mod` 的版本 */
    val modVersion: String,

    /** 插件参数 */
    var params: JsonElement = JsonNull
) {
    /**
     * 通过插件名称和依赖信息来创建 [AddonPlugin]
     *
     * @param addonName 插件名称
     * @param dependenceInfo 依赖的 `Mod` 信息，用于填写 [modName] 和 [modVersion]
     */
    constructor(addonName: String, dependenceInfo: DependenceInfo) : this(
        modName = dependenceInfo.modName,
        addonName = addonName,
        modVersion = dependenceInfo.version
    )

    /**
     * 通过插件名称、插件参数和依赖信息来创建 [AddonPlugin]
     *
     * @param addonName 插件名称
     * @param params 插件参数，默认为 [JsonNull]
     * @param dependenceInfo 依赖的 `Mod` 信息，用于填写 [modName] 和 [modVersion]
     */
    constructor(addonName: String, params: JsonElement = JsonNull, dependenceInfo: DependenceInfo) : this(
        modName = dependenceInfo.modName,
        addonName = addonName,
        modVersion = dependenceInfo.version,
        params = params
    )
}