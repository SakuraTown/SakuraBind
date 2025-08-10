package top.iseason.bukkit.sakurabind.utils

/*
    * 从List1中移除List2 index to newList
    * */
internal fun <T> removeList(
    list1: List<String>,
    collection: Collection<T>,
    op: (String, T) -> Boolean
): Pair<Int, MutableList<String>> {
    val index = indexOfList(list1, collection, op)
    val toMutableList = list1.toMutableList()
    if (index > -1) {
        repeat(collection.size) {
            toMutableList.removeAt(index)
        }
        return index to toMutableList
    }
    return index to toMutableList
}

internal fun <T> containList(
    list1: List<String>,
    collection: Collection<T>,
    op: (String, T) -> Boolean
): Boolean {
    return indexOfList(list1, collection, op) > -1
}

internal fun <T> indexOfList(
    list1: List<String>,
    collection: Collection<T>,
    op: (String, T) -> Boolean
): Int {
    var index = -1
    if (collection.size > list1.size) return index
    var iterator = collection.iterator()
    first@ for ((i, raw) in list1.withIndex()) {
        if (!iterator.hasNext()) break@first
        val next = iterator.next()
        if (!op(raw, next)) {
            index = -1
            iterator = collection.iterator()
        } else if (index == -1) index = i
    }
    return index
}
