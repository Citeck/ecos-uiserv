
package ru.citeck.ecos.uiserv.domain.menu.service.format.xml.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for itemsResolver complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="itemsResolver">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="param" type="{http://www.citeck.ru/menu/config/1.0}parameter" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="item" type="{http://www.citeck.ru/menu/config/1.0}item" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "itemsResolver", namespace = "http://www.citeck.ru/menu/config/1.0", propOrder = {
    "param",
    "item"
})
public class ItemsResolver {

    @XmlElement(namespace = "http://www.citeck.ru/menu/config/1.0")
    protected List<Parameter> param;
    @XmlElement(namespace = "http://www.citeck.ru/menu/config/1.0")
    protected Item item;
    @XmlAttribute(name = "id")
    protected String id;

    /**
     * Gets the value of the param property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the param property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getParam().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Parameter }
     *
     *
     */
    public List<Parameter> getParam() {
        if (param == null) {
            param = new ArrayList<Parameter>();
        }
        return this.param;
    }

    /**
     * Gets the value of the item property.
     *
     * @return
     *     possible object is
     *     {@link Item }
     *
     */
    public Item getItem() {
        return item;
    }

    /**
     * Sets the value of the item property.
     *
     * @param value
     *     allowed object is
     *     {@link Item }
     *
     */
    public void setItem(Item value) {
        this.item = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setId(String value) {
        this.id = value;
    }

}
