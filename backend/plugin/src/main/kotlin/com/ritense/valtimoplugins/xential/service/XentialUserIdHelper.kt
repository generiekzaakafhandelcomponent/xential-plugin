package com.ritense.valtimoplugins.xential.service

import com.ritense.valtimo.security.jwt.provider.SecretKeyResolver
import io.github.oshai.kotlinlogging.KotlinLogging
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import org.springframework.security.core.context.SecurityContextHolder

class XentialUserIdHelper(
    private val secretKeyResolver: SecretKeyResolver
) {

    fun getXentialUserId(): String {
        val context = SecurityContextHolder.getContext()
        val authentication = context.authentication
        val claims = getClaims(authentication.credentials.toString())

        logger.info { "XentialUserId from keycloak user: ${claims[XENTIALUSERID]?: "not set"}" }
        return claims[XENTIALUSERID].toString()
    }

    private fun getClaims(jwt: String): Claims {
        return jwtSignedParser().parseClaimsJws(jwt).body
    }

    private fun jwtSignedParser(): JwtParser {
        return Jwts.parser()
            .setSigningKeyResolver(secretKeyResolver)
            .build()
    }


    companion object {
        const val XENTIALUSERID = "XentialUserId"
        private val logger = KotlinLogging.logger {}
    }

}
