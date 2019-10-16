package network.xyo.sdk.sample

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

// this needs to be imported by sdk-ui-android (https://github.com/XYOracleNetwork/sdk-ui-android)
private class AndroidContinuation<T>(val cont: Continuation<T>) : Continuation<T> by cont {
    override fun resumeWith(result: Result<T>) {
        if (Looper.myLooper() == Looper.getMainLooper()) cont.resumeWith(result)
        else Handler(Looper.getMainLooper()).post { cont.resumeWith(result) }
    }

}

object UIThread : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
        AndroidContinuation(continuation)
}

fun ui(
    context: CoroutineContext = UIThread,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return GlobalScope.launch(context, start, block)
}