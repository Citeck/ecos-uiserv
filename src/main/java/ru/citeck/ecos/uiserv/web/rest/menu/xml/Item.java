
package ru.citeck.ecos.uiserv.web.rest.menu.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for item complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="item">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="type" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="label" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="icon" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="mobile-visible" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="param" type="{http://www.citeck.ru/menu/config/1.0}parameter" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="action" type="{http://www.citeck.ru/menu/config/1.0}action" minOccurs="0"/>
 *         &lt;element name="evaluator" type="{http://www.citeck.ru/menu/config/1.0}evaluator" minOccurs="0"/>
 *         &lt;element name="items" type="{http://www.citeck.ru/menu/config/1.0}items" minOccurs="0"/>
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
@XmlType(name = "item", namespace = "http://www.citeck.ru/menu/config/1.0", propOrder = {
    "type",
    "label",
    "icon",
    "mobileVisible",
    "param",
    "action",
    "evaluator",
    "items"
})
public class Item {

    @XmlElement(namespace = "http://www.citeck.ru/menu/config/1.0")
    protected String type;
    @XmlElement(namespace = "http://www.citeck.ru/menu/config/1.0")
    protected String label;
    @XmlElement(namespace = "http://www.citeck.ru/menu/config/1.0")
    protected String icon;
    @XmlElement(name = "mobile-visible", namespace = "http://www.citeck.ru/menu/config/1.0")
    protected Boolean mobileVisible;
    @XmlElement(namespace = "http://www.citeck.ru/menu/config/1.0")
    protected List<Parameter> param;
    @XmlElement(namespace = "http://www.citeck.ru/menu/config/1.0")
    protected Action action;
    @XmlElement(namespace = "http://www.citeck.ru/menu/config/1.0")
    protected Evaluator evaluator;
    @XmlElement(namespace = "http://www.citeck.ru/menu/config/1.0")
    protected Items items;
    @XmlAttribute(name = "id")
    protected String id;

    /**
     * Gets the value of the type property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the label property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the value of the label property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setLabel(String value) {
        this.label = value;
    }

    /**
     * Gets the value of the icon property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Sets the value of the icon property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setIcon(String value) {
        this.icon = value;
    }

    /**
     * Gets the value of the mobileVisible property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    public Boolean isMobileVisible() {
        return mobileVisible;
    }

    /**
     * Sets the value of the mobileVisible property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    public void setMobileVisible(Boolean value) {
        this.mobileVisible = value;
    }

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
     * Gets the value of the action property.
     *
     * @return
     *     possible object is
     *     {@link Action }
     *
     */
    public Action getAction() {
        return action;
    }

    /**
     * Sets the value of the action property.
     *
     * @param value
     *     allowed object is
     *     {@link Action }
     *
     */
    public void setAction(Action value) {
        this.action = value;
    }

    /**
     * Gets the value of the evaluator property.
     *
     * @return
     *     possible object is
     *     {@link Evaluator }
     *
     */
    public Evaluator getEvaluator() {
        return evaluator;
    }

    /**
     * Sets the value of the evaluator property.
     *
     * @param value
     *     allowed object is
     *     {@link Evaluator }
     *
     */
    public void setEvaluator(Evaluator value) {
        this.evaluator = value;
    }

    /**
     * Gets the value of the items property.
     *
     * @return
     *     possible object is
     *     {@link Items }
     *
     */
    public Items getItems() {
        return items;
    }

    /**
     * Sets the value of the items property.
     *
     * @param value
     *     allowed object is
     *     {@link Items }
     *
     */
    public void setItems(Items value) {
        this.items = value;
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
