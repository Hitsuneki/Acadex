package com.example.acadex.ui.quiz

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.acadex.data.model.QuizSet
import com.example.acadex.data.repository.QuizRepository
import com.example.acadex.data.result.RepoResult
import kotlinx.coroutines.launch

class QuizViewModel : ViewModel() {

    private val _quizzes = MutableLiveData<List<QuizSet>>(emptyList())
    val quizzes: LiveData<List<QuizSet>> = _quizzes

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            when (val result = QuizRepository.fetchQuizSets()) {
                is RepoResult.Success -> _quizzes.postValue(result.data)
                is RepoResult.Error -> _quizzes.postValue(emptyList())
            }
        }
    }
}
