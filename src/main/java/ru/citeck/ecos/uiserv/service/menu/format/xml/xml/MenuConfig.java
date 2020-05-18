package ru.citeck.ecos.uiserv.service.menu.format.xml.xml;

import javax.xml.bind.annotation.*;
import java.io.Serializable;


/**
 * <p>Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="id" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="type" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="authorities" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="items" type="{http://www.citeck.ru/menu/config/1.0}items"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "id",
    "type",
    "authorities",
    "items"
})
@XmlRootElement(name = "menu-config", namespace = "http://www.citeck.ru/menu/config/1.0")
public class MenuConfig implements Serializable  {

    @XmlElement(namespace = "http://www.citeck.ru/menu/config/1.0", required = true)
    protected String id;
    @XmlElement(namespace = "http://www.citeck.ru/menu/config/1.0", required = true)
    protected String type;
    @XmlElement(namespace = "http://www.citeck.ru/menu/config/1.0", required = true)
    protected String authorities;
    @XmlElement(namespace = "http://www.citeck.ru/menu/config/1.0", required = true)
    protected Items items;

    /**
     * Gets the value of the id property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the type property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the authorities property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getAuthorities() {
        return authorities;
    }

    /**
     * Sets the value of the authorities property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setAuthorities(String value) {
        this.authorities = value;
    }

    /**
     * Gets the value of the items property.
     *
     * @return possible object is
     * {@link Items }
     */
    public Items getItems() {
        return items;
    }

    /**
     * Sets the value of the items property.
     *
     * @param value allowed object is
     *              {@link Items }
     */
    public void setItems(Items value) {
        this.items = value;
    }

}
