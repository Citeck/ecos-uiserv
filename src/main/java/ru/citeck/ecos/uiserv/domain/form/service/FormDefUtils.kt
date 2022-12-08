package ru.citeck.ecos.uiserv.domain.form.service

import ru.citeck.ecos.commons.data.DataValue

object FormDefUtils {

    @JvmStatic
    fun mapInputComponents(component: DataValue, action: (DataValue) -> DataValue?): DataValue? {
        return mapComponents(component, { it["input"].asBoolean(false) }, action)
    }

    @JvmStatic
    fun mapComponents(
        component: DataValue,
        condition: (DataValue) -> Boolean?,
        action: (DataValue) -> DataValue?
    ): DataValue? {

        val result = if (condition.invoke(component) == true) {
            action.invoke(component) ?: return null
        } else {
            component
        }

        val innerKey = getInnerComponentsKey(result)
        val inner = result[innerKey]
        if (inner.isArray()) {
            val mappedInner = DataValue.createArr()
            for (value in inner) {
                val mappedValue = mapComponents(value, condition, action) ?: continue
                if (!value.isArray() && mappedValue.isArray()) {
                    mappedValue.forEach { mappedElement ->
                        mapComponents(mappedElement, condition, action)?.let { mappedInner.add(it) }
                    }
                } else {
                    mappedInner.add(mappedValue)
                }
            }
            result[innerKey] = mappedInner
        }

        return result
    }

    @JvmStatic
    fun getComponentAtt(component: DataValue): String {
        val attribute = component.get("properties").get("attribute").asText()
        if (attribute.isNotBlank()) {
            return attribute
        }
        return component["key"].asText()
    }

    @JvmStatic
    fun getInnerComponents(component: DataValue): DataValue {
        return component[getInnerComponentsKey(component)]
    }

    @JvmStatic
    fun getInnerComponentsKey(component: DataValue): String {
        return if (component["type"].asText() == "columns") {
            "columns"
        } else {
            "components"
        }
    }
}
