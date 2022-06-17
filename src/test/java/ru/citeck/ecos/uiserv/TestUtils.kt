package ru.citeck.ecos.uiserv

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User

object TestUtils {

    fun <T : Any> runAsUser(username: String, authorities: List<String>, action: () -> T): T {

        val result: T

        try {

            val grantedAuthorities = authorities.map { SimpleGrantedAuthority(it) }
            val user = User(username, "", grantedAuthorities)
            val auth = UsernamePasswordAuthenticationToken(user, "", grantedAuthorities)
            SecurityContextHolder.getContext().authentication = auth

            result = action.invoke()
        } finally {
            SecurityContextHolder.getContext().authentication = null
        }

        return result
    }
}
