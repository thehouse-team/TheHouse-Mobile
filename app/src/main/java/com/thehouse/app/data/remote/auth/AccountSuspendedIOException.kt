package com.thehouse.app.data.remote.auth

import java.io.IOException

/** Bloqueo local tras suspensión confirmada; no fabrica una respuesta HTTP. */
class AccountSuspendedIOException : IOException()
