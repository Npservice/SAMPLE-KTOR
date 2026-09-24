package com.example.common.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * Dispatcher berbasis virtual thread (JDK 21+).
 *
 * Dipasang global untuk semua request lewat `configureVirtualThreads()`, jadi kode aplikasi
 * tidak perlu memakainya langsung. Setiap tugas mendapat virtual thread sendiri, sehingga
 * request yang sedang menunggu I/O (JDBC, file, Redis) tidak menghabiskan thread OS.
 * Jumlah query yang benar-benar jalan bersamaan tetap dibatasi ukuran pool koneksi Hikari.
 */
val VirtualThreadDispatcher: CoroutineDispatcher =
    Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
