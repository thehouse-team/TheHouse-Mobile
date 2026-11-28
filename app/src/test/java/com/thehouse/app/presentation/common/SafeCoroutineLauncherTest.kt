package com.thehouse.app.presentation.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.EmptyCoroutineContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SafeCoroutineLauncherTest {

    @Test
    fun `socket timeout is contained and logged without message or PII`() = runTest {
        val markers = mutableListOf<String>()
        val previous = SafeCoroutineLauncher.logger
        SafeCoroutineLauncher.logger = { markers.add(it) }
        try {
            SafeCoroutineLauncher.handler.handleException(
                EmptyCoroutineContext,
                SocketTimeoutException("timeout https://host?email=user@test.com"),
            )
        } finally {
            SafeCoroutineLauncher.logger = previous
        }
        assertEquals(1, markers.size)
        assertTrue(markers.first().startsWith("network_contained="))
        assertTrue(markers.none { it.contains("timeout ") || it.contains("@") })
    }

    @Test
    fun `unknown host is contained too`() = runTest {
        val markers = mutableListOf<String>()
        val previous = SafeCoroutineLauncher.logger
        SafeCoroutineLauncher.logger = { markers.add(it) }
        try {
            SafeCoroutineLauncher.handler.handleException(
                EmptyCoroutineContext,
                UnknownHostException("unknown host"),
            )
        } finally {
            SafeCoroutineLauncher.logger = previous
        }
        assertEquals(listOf("network_contained=UnknownHostException"), markers)
    }

    @Test
    fun `cancellation is not reported as an error`() = runTest {
        val markers = mutableListOf<String>()
        val previous = SafeCoroutineLauncher.logger
        SafeCoroutineLauncher.logger = { markers.add(it) }
        try {
            SafeCoroutineLauncher.handler.handleException(
                EmptyCoroutineContext,
                CancellationException("cancelled"),
            )
        } finally {
            SafeCoroutineLauncher.logger = previous
        }
        assertTrue(markers.isEmpty())
    }

    @Test
    fun `unexpected error is logged as unexpected for visibility`() = runTest {
        val markers = mutableListOf<String>()
        val previous = SafeCoroutineLauncher.logger
        SafeCoroutineLauncher.logger = { markers.add(it) }
        try {
            SafeCoroutineLauncher.handler.handleException(
                EmptyCoroutineContext,
                IllegalStateException("bad state"),
            )
        } finally {
            SafeCoroutineLauncher.logger = previous
        }
        assertTrue(markers.single().startsWith("unexpected="))
    }

    @Test
    fun `generic IOException is covered`() = runTest {
        val markers = mutableListOf<String>()
        val previous = SafeCoroutineLauncher.logger
        SafeCoroutineLauncher.logger = { markers.add(it) }
        try {
            SafeCoroutineLauncher.handler.handleException(
                EmptyCoroutineContext,
                IOException("broken pipe"),
            )
        } finally {
            SafeCoroutineLauncher.logger = previous
        }
        assertTrue(markers.single().startsWith("network_contained="))
    }
}
