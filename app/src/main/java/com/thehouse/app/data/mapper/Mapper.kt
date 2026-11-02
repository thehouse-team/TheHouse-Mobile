package com.thehouse.app.data.mapper

/**
 * Contrato de transformación entre modelos de Data y Domain.
 *
 * La dirección arquitectónica permanece Data → Domain. Un mapper puede transformar
 * en ambos sentidos de modelo, pero eso no autoriza a Domain a depender de Data.
 */
fun interface Mapper<in From, out To> {
    fun map(value: From): To
}
