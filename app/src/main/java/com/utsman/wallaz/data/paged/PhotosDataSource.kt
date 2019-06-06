/*
 * Copyright 2019 Muhammad Utsman. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.utsman.wallaz.data.paged

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.paging.ItemKeyedDataSource
import com.rx2androidnetworking.Rx2AndroidNetworking
import com.utsman.wallaz.BuildConfig
import com.utsman.wallaz.data.NetworkState
import com.utsman.wallaz.data.Photos
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.lang.Exception
import java.util.concurrent.TimeUnit

class PhotosDataSource(private val disposable: CompositeDisposable, private val orderBy: String) : ItemKeyedDataSource<String, Photos>() {

    private var nextPage: Long = 1
    val networkState = MutableLiveData<NetworkState>()

    /**
     * Create rx function for using in loader function
     * */
    private fun rxNetwork(page: Long) = Rx2AndroidNetworking.get(BuildConfig.BASE_URL)
            .addPathParameter("endpoint", "photos")
            .addQueryParameter("page", page.toString())
            .addQueryParameter("order_by", orderBy)
            .addQueryParameter("client_id", BuildConfig.CLIENT_ID)
            .build()
            .getObjectListObservable(Photos::class.java)

    override fun loadInitial(params: LoadInitialParams<String>, callback: LoadInitialCallback<Photos>) {
        networkState.postValue(NetworkState.LOADING)
        disposable.add(
                rxNetwork(nextPage)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext {
                            nextPage++
                        }
                        .subscribe({ photos ->
                            callback.onResult(photos)
                            networkState.postValue(NetworkState.LOADED)
                        }, { t ->
                            try {
                                Log.e("ANJAY", t.localizedMessage)
                            } catch (e: Exception) {
                                Log.e("ANJAY", e.localizedMessage)
                            }

                            networkState.postValue(NetworkState.FAILED)

                        })
        )
    }

    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<Photos>) {
        networkState.postValue(NetworkState.LOADING)
        disposable.add(
                rxNetwork(nextPage)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext {
                            nextPage++
                        }
                        .delay(300, TimeUnit.MILLISECONDS) // Delay 300 milli second after receiving previous data
                        .subscribe({ photos ->
                            callback.onResult(photos)
                            networkState.postValue(NetworkState.LOADED)
                        }, { t ->
                            try {
                                Log.e("ANJAY", t.localizedMessage)
                            } catch (e: Exception) {
                                Log.e("ANJAY", e.localizedMessage)
                            }

                            networkState.postValue(NetworkState.FAILED)

                        })
        )
    }

    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<Photos>) {
    }

    override fun getKey(item: Photos): String = item.id
}