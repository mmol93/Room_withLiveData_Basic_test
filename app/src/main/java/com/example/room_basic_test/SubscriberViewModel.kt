package com.example.room_basic_test

import androidx.lifecycle.*
import com.example.room_basic_test.database.Subscriber
import com.example.room_basic_test.database.SubscriberRepository
import com.example.room_basic_test.event.Event
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SubscriberViewModel(private val repository: SubscriberRepository):ViewModel() {
    val inputName = MutableLiveData<String?>()
    val inputEmail = MutableLiveData<String?>()

    val saveUpdateButtonText = MutableLiveData<String>()
    val clearDeleteButtonText = MutableLiveData<String>()

    // 항목 클릭 시 변화하게 할 요소
    private var isUpdateOrDelete = false
    private lateinit var subscriberToUpdateOrDelete: Subscriber

    // Event Wrapper 사용하기
    private val statusEvent = MutableLiveData<Event<String>>()
    // statusEvent를 받을 수 있는 liveData 만들기 -> MainActivity.kt에서 observe를 해준다
    val message : LiveData<Event<String>>
        get() = statusEvent

    init {
        saveUpdateButtonText.value = "save"
        clearDeleteButtonText.value = "clear"
    }

    // 항목을 클릭했을 때 발동
    fun initUpdateOrDelete(subscriber: Subscriber){
        // 클릭한 subscriber 데이터를 이 클래스의 subscriberToUpdateOrDelete에 넣어준다
        subscriberToUpdateOrDelete = subscriber

        inputName.value = subscriber.name
        inputEmail.value = subscriber.email
        isUpdateOrDelete = true
        saveUpdateButtonText.value = "update"
        clearDeleteButtonText.value = "delete"
    }

    // 데이터 베이스에 추가 or 업데이트
    fun saveUpdate(){
        // 어떤 항목이 클릭 되었을 때 -> 선택 항목을 입력한 내용으로 업데이트
        if (isUpdateOrDelete){
            subscriberToUpdateOrDelete.name = inputName.value!!
            subscriberToUpdateOrDelete.email = inputEmail.value!!
            update(subscriberToUpdateOrDelete)
        }
        else{
            val name = inputName.value!!
            val email = inputEmail.value!!
            insert(Subscriber(0, name, email))
        }
        inputName.value = null
        inputEmail.value = null
    }

    // 데이터 베이스 일부를 삭제 or 전부 삭제
    fun clearDelete(){
        if (isUpdateOrDelete){
            delete(subscriberToUpdateOrDelete)
        }
        else{
            clear()
        }
        inputName.value = null
        inputEmail.value = null
    }

    // ↓ repository에 있는 메서드 호출하는 영역
    fun insert(subscriber: Subscriber){
        viewModelScope.launch {
            val rowId : Long = repository.insert(subscriber)
            if (rowId > -1){
                statusEvent.value = Event("Subscriber insert successfully: $rowId")
            }else{
                statusEvent.value = Event("Subscriber insert failed")
            }

        }
    }

    fun clear(){
        viewModelScope.launch {
            repository.clear()
            statusEvent.value = Event("Subscriber clear successfully")
        }
    }

    fun update(subscriber: Subscriber){
        viewModelScope.launch {
            val rowId : Int = repository.update(subscriber)
            if (rowId > 0){
                // 한 번 실시 한 이후에 초기화
                saveUpdateButtonText.value = "save"
                clearDeleteButtonText.value = "clear"
                isUpdateOrDelete = false
                statusEvent.value = Event("Subscriber update successfully: $rowId")
            }else{
                statusEvent.value = Event("Subscriber update failed")
            }
        }
    }

    fun delete(subscriber: Subscriber){
        viewModelScope.launch {
            val rowId : Int = repository.delete(subscriber)
            if (rowId > 0){
                saveUpdateButtonText.value = "save"
                clearDeleteButtonText.value = "clear"
                isUpdateOrDelete = false
                statusEvent.value = Event("Subscriber delete successfully: $rowId")
            }else{
                statusEvent.value = Event("Subscriber delete failed")
            }
        }
    }

    fun getSubscribers() = liveData<List<Subscriber>> {
        repository.subscribers.collect {
            emit(it)
        }
    }

}