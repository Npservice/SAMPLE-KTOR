package com.example.common.plugins

import com.example.common.utils.VirtualThreadDispatcher
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import kotlinx.coroutines.withContext

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
