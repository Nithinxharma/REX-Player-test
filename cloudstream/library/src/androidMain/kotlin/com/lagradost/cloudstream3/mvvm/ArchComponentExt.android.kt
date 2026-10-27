package com.lagradost.cloudstream3.mvvm

actual fun <T> platformThrowAbleToResource(throwable: Throwable): Resource<T> {
    return safeFail(throwable)
}
