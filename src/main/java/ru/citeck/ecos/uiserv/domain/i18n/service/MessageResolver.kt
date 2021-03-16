package ru.citeck.ecos.uiserv.domain.i18n.service

interface MessageResolver {

    fun getMessage(key: String): String
}
