package com.itsabugnotafeature.fitocrazy.ui.exercises_and_components

import com.itsabugnotafeature.fitocrazy.common.ExerciseComponentType
import com.itsabugnotafeature.fitocrazy.ui.exercises_and_components.components.ComponentDisplayListAdapter
import com.itsabugnotafeature.fitocrazy.ui.exercises_and_components.components.ComponentFragment
import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

class ComponentDisplayListAdapterTest {

    @Test
    fun updateDisplayedItems() {
        // GIVEN
        val componentsData = listOf(
            ComponentFragment.ComponentView(1L, "A", emptyList()),
            ComponentFragment.ComponentView(2L, "AB", emptyList()),
            ComponentFragment.ComponentView(3L, "B", emptyList()),
            ComponentFragment.ComponentView(4L, "CA", emptyList()),
            ComponentFragment.ComponentView(5L, "D", emptyList()),
        )
        val componentAdapter = spyk<ComponentDisplayListAdapter>(ComponentDisplayListAdapter(ExerciseComponentType.LOCATION))
        every { componentAdapter.updateDisplayedItems(any()) } answers { callOriginal() }
        every { componentAdapter.notifyItemRangeRemoved(any(), any()) } returns Unit
        every { componentAdapter.notifyItemRangeInserted(any(), any()) } returns Unit
        every { componentAdapter.notifyItemInserted(any()) } returns Unit
        every { componentAdapter.notifyItemRemoved(any()) } returns Unit

        componentAdapter.dataList = componentsData.toMutableList()
        componentAdapter.displayList = componentsData.toMutableList()

        componentAdapter.updateDisplayedItems("A")
        assertTrue(componentAdapter.displayList.size == 3)
        assertIterableEquals(componentAdapter.displayList.map { it.id }, listOf(1L, 2L, 4L))
        /*verify(exactly = 1) { componentAdapter.notifyItemRemoved(2) }
        verify(exactly = 1) { componentAdapter.notifyItemRangeRemoved(3, 1) }*/

        componentAdapter.updateDisplayedItems("AB")
        assertTrue(componentAdapter.displayList.size == 1)
        assertIterableEquals(componentAdapter.displayList.map { it.id }, listOf(2L))
        /*verify(exactly = 2) { componentAdapter.notifyItemRemoved(any()) }*/

        componentAdapter.updateDisplayedItems("")
        assertTrue(componentAdapter.displayList.size == 5)
        assertIterableEquals(componentAdapter.displayList, componentsData)
        /*verify(exactly = 1) { componentAdapter.notifyItemInserted(0) }
        verify(exactly = 1) { componentAdapter.notifyItemRangeInserted(2, 3) }*/

        componentAdapter.updateDisplayedItems("unsearchable")
        assertTrue(componentAdapter.displayList.size == 0)
        assertIterableEquals(componentAdapter.displayList, emptyList<ComponentFragment.ComponentView>())
        /*verify(exactly = 1) { componentAdapter.notifyItemRangeRemoved(0, 5) }*/

        // TODO: test should actually check all permutations+combinations
    }
}