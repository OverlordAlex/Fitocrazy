package com.itsabugnotafeature.fitocrazy.common

import android.content.Context
import java.util.Collections

interface DisplayListAdapter<ModelType : Comparable<ModelType>> {
    var dataList: MutableList<ModelType>
    var displayList: MutableList<ModelType>

    suspend fun loadData(applicationContext: Context, arguments: Map<String, Any>? = null)

    fun addNewItem(item: ModelType) {
        dataList.add(item)
        dataList.sort()
        updateDisplayedItems("")
    }

    fun replaceItemAt(index: Int, newItem: ModelType) {
        dataList.removeAt(index)
        dataList.add(newItem)
        dataList.sort()
        updateDisplayedItems("")
    }

    fun removeItemAt(index: Int): ModelType {
        val removed = dataList.removeAt(index)
        displayList.removeAt(index)
        notifyItemRemoved(index)
        return removed
    }

    fun swap(first: Int, second: Int) {
        if (first == second) throw IllegalArgumentException()
        val firstItem = if (first < second) first else second
        val secondItem = if (first < second) second else first
        if (secondItem - firstItem != 1) throw IllegalArgumentException()

        Collections.swap(dataList, firstItem, secondItem)
        Collections.swap(displayList, firstItem, secondItem)
        notifyItemMoved(firstItem, secondItem)
        notifyItemChanged(firstItem)
        notifyItemChanged(secondItem)
    }

    fun filterDataList(filter: String): List<ModelType>

    fun updateDisplayedItems(filter: String, ) {
        val desiredFinalState = if (filter.isEmpty()) dataList else filterDataList(filter)

        if (desiredFinalState.isEmpty()) {
            val oldDisplayLength = displayList.size
            displayList = emptyList<ModelType>().toMutableList()
            notifyItemRangeRemoved(0, oldDisplayLength)
        }

        var leftIdx = 0
        var rightIdx = 0

        while (leftIdx < desiredFinalState.size) {
            val comp =
                if (rightIdx == displayList.size) -1 else desiredFinalState[leftIdx].compareTo(displayList[rightIdx])
            when {
                comp < 0 -> {
                    displayList.add(rightIdx, desiredFinalState[leftIdx])
                    notifyItemInserted(rightIdx)
                    leftIdx++
                    rightIdx++
                }

                comp == 0 -> {
                    leftIdx++
                    rightIdx++
                }

                else -> {
                    displayList.removeAt(rightIdx)
                    notifyItemRemoved(rightIdx)
                }
            }
        }
        if (displayList.size > desiredFinalState.size) {
            val dropAmount = displayList.size - desiredFinalState.size
            displayList = displayList.dropLast(dropAmount).toMutableList()
            notifyItemRangeRemoved(displayList.size, dropAmount)
        }
    }

    // Methods shadowed from RecyclerView.ViewHolder
    fun notifyItemRangeRemoved(positionStart: Int, itemCount: Int)
    fun notifyItemMoved(from: Int, to: Int)
    fun notifyItemInserted(position: Int)
    fun notifyItemRemoved(position: Int)
    fun notifyItemChanged(position: Int)
}