package top.iseason.bukkit.bukkittemplate.ui.container

interface Pageable {
    var container: UIContainer?
    fun getUI(): BaseUI

    /**
     * 唯一id
     */
    var serializeId: String
}