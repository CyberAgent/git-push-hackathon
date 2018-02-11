package io.moatwel.github.presentation.view.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import io.moatwel.github.data.datasource.EventDataSourceFactory
import io.moatwel.github.data.network.retrofit.EventApi
import io.moatwel.github.domain.entity.event.Event
import io.moatwel.github.domain.usecase.UserUseCase
import javax.inject.Inject

class EventViewModel @Inject constructor(
  api: EventApi,
  userUseCase: UserUseCase
) : ViewModel() {

  private val factory: EventDataSourceFactory = EventDataSourceFactory(api, userUseCase)

  val events: LiveData<PagedList<Event>>

  init {
    val config = PagedList.Config.Builder()
      .setInitialLoadSizeHint(2)
      .setPrefetchDistance(PAGE_SIZE)
      .setPageSize(PAGE_SIZE)
      .build()

    events = LivePagedListBuilder(factory, config).build()
  }

  companion object {
    private const val PAGE_SIZE = 15
  }
}