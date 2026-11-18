package com.thehouse.app.domain.connectivity

import kotlinx.coroutines.flow.StateFlow

interface ConnectivityObserver {
    val isOnline: StateFlow<Boolean>
}