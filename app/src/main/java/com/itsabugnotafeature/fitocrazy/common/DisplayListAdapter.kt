package com.itsabugnotafeature.fitocrazy.common

import android.content.Context

interface DisplayListAdapter<ModelType : Comparable<ModelType>> {
    var dataList: MutableList<ModelType>
    var displayList: MutableList<ModelType>

    suspend fun loadData(applicationContext: Context)

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

    fun removeItemAt(index: Int) {
        dataList.removeAt(index)
        displayList.removeAt(index)
        notifyItemRemoved(index)
    }

    fun filterDataList(filter: String): List<ModelType>

    fun notifyItemRangeRemoved(positionStart: Int, itemCount: Int)
    fun notifyItemInserted(position: Int)
    fun notifyItemRemoved(position: Int)

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
}