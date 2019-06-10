
package ru.citeck.ecos.uiserv.web.rest.menu.xml;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the ru.citeck.ecos.menu.xml package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: ru.citeck.ecos.menu.xml
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link MenuConfig }
     *
     */
    public MenuConfig createMenuConfig() {
        return new MenuConfig();
    }

    /**
     * Create an instance of {@link Items }
     *
     */
    public Items createItems() {
        return new Items();
    }

    /**
     * Create an instance of {@link Item }
     *
     */
    public Item createItem() {
        return new Item();
    }

    /**
     * Create an instance of {@link Parameter }
     *
     */
    public Parameter createParameter() {
        return new Parameter();
    }

    /**
     * Create an instance of {@link Action }
     *
     */
    public Action createAction() {
        return new Action();
    }

    /**
     * Create an instance of {@link ItemsResolver }
     *
     */
    public ItemsResolver createItemsResolver() {
        return new ItemsResolver();
    }

    /**
     * Create an instance of {@link Evaluator }
     *
     */
    public Evaluator createEvaluator() {
        return new Evaluator();
    }

}
