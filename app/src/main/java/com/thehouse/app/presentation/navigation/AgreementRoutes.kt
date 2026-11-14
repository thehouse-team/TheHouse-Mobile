package com.thehouse.app.presentation.navigation

import java.net.URLDecoder
import java.net.URLEncoder

data class LandlordAgreementDetailRoute(val agreementId: String) : AppRoute {
    override val route: String = "acuerdos_arrendador/" + URLEncoder.encode(agreementId, "UTF-8").replace("+", "%20")

    companion object {
        const val PATTERN = "acuerdos_arrendador/{id}"
        const val ID_ARGUMENT = "id"
    }
}

data class CreateAgreementRoute(val requestId: String) : AppRoute {
    override val route: String = "acuerdo/crear/" + URLEncoder.encode(requestId, "UTF-8").replace("+", "%20")

    companion object {
        const val PATTERN = "acuerdo/crear/{requestId}"
        const val ID_ARGUMENT = "requestId"
    }
}

data class AgreementPreviewRoute(val code: String) : AppRoute {
    override val route: String = "acuerdo/preview/" + URLEncoder.encode(code, "UTF-8").replace("+", "%20")

    companion object {
        const val PATTERN = "acuerdo/preview/{code}"
        const val ID_ARGUMENT = "code"
        fun decode(raw: String): String = URLDecoder.decode(raw, "UTF-8")
    }
}
