
package ru.citeck.ecos.uiserv.web.rest.menu.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for items complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="items">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.citeck.ru/menu/config/1.0}itemsChildren" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "items", namespace = "http://www.citeck.ru/menu/config/1.0", propOrder = {
    "itemsChildren"
})
public class Items {

    @XmlElements({
        @XmlElement(name = "item", namespace = "http://www.citeck.ru/menu/config/1.0", type = Item.class),
        @XmlElement(name = "resolver", namespace = "http://www.citeck.ru/menu/config/1.0", type = ItemsResolver.class)
    })
    protected List<Object> itemsChildren;

    /**
     * Gets the value of the itemsChildren property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the itemsChildren property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getItemsChildren().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Item }
     * {@link ItemsResolver }
     *
     *
     */
    public List<Object> getItemsChildren() {
        if (itemsChildren == null) {
            itemsChildren = new ArrayList<Object>();
        }
        return this.itemsChildren;
    }

}
