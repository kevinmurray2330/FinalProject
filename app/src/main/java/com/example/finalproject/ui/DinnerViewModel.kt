package com.example.finalproject.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.finalproject.data.Dinner
import com.example.finalproject.data.DinnerDao
import com.example.finalproject.data.FamilyMember
import com.example.finalproject.data.Topic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DinnerViewModel(private val dao: DinnerDao) : ViewModel() {

    // FIX: Added <Dinner> and <FamilyMember> to emptyList() so Kotlin knows the types.
    val allDinners = dao.getAllDinners().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList<Dinner>()
    )

    val allFamilyMembers = dao.getAllFamilyMembers().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList<FamilyMember>()
    )

    // Actions
    fun addDinner(date: String, time: String, attendees: List<String>) = viewModelScope.launch {
        val attendeesString = attendees.joinToString(", ")
        dao.insertDinner(Dinner(date = date, time = time, attendees = attendeesString))
    }

    fun addFamilyMember(name: String, role: String) = viewModelScope.launch {
        dao.insertFamilyMember(FamilyMember(name = name, role = role))
    }

    fun deleteFamilyMember(member: FamilyMember) = viewModelScope.launch {
        dao.deleteFamilyMember(member)
    }

    fun toggleMemberStatus(member: FamilyMember) = viewModelScope.launch {
        dao.updateMemberStatus(member.id, !member.isOnline)
    }

    // Helper to get a random topic on demand
    fun getRandomTopic(onResult: (Topic?) -> Unit) = viewModelScope.launch {
        val topic = dao.getRandomTopic()
        onResult(topic)
    }
}

// Factory to create ViewModel with Dependencies
class DinnerViewModelFactory(private val dao: DinnerDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DinnerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DinnerViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}