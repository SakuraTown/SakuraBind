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
    if (collection.isEmpty() || collection.size > list1.size) return -1
    val patterns = collection as? List<T> ?: collection.toList()
    for (start in 0..(list1.size - patterns.size)) {
        var matched = true
        for (offset in patterns.indices) {
            if (!op(list1[start + offset], patterns[offset])) {
                matched = false
                break
            }
        }
        if (matched) {
            return start
        }
    }
    return -1
}
