package com.example.core.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Dispatcher berbasis virtual thread (JDK 21+). Setiap tugas mendapat virtual thread sendiri, sehingga
 * request yang menunggu I/O (JDBC, file, Redis) tidak menghabiskan thread OS. Jumlah query yang benar-benar
 * jalan bersamaan tetap dibatasi ukuran pool koneksi Hikari.
 */
val VirtualThreadDispatcher: CoroutineDispatcher = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()

/**
 * Jalankan seluruh pemrosesan request (plugin, route, service, repository) di virtual thread.
 *
 * Dipasang sekali di fase paling awal pipeline, jadi kode di bawahnya boleh langsung memanggil
 * I/O blocking (JDBC, file, Redis) tanpa `withContext(...)` di tiap tempat.
 * Harus dipanggil sebelum plugin lain dipasang.
 */
fun Application.configureVirtualThreads() {
    intercept(ApplicationCallPipeline.Setup) {
        withContext(VirtualThreadDispatcher) { proceed() }
    }
}
