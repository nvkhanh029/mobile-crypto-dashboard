package vn.edu.usth.cryptodashboard.data.source

import vn.edu.usth.cryptodashboard.domain.model.AppError

interface DataSourceCallback<T> {
    fun onSuccess(data: T)
    fun onError(error: AppError)
}