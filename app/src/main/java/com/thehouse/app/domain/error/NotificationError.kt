package com.thehouse.app.domain.error

class NotificationException(
    val domainError: DomainError,
    cause: Throwable? = null,
) : Exception(domainError::class.simpleName, cause)
