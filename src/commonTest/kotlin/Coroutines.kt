package ch.softappeal.kopi

import kotlinx.coroutines.currentCoroutineContext

suspend fun printlnCC(msg: String) {
    val context = currentCoroutineContext()
    println("$context@${context.hashCode()} - $msg")
}
