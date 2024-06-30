package com.ucv.util;

import com.ucv.datamodel.xml.Item;
import com.ucv.datamodel.xml.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class XmlParser {
    Logger logger = LogManager.getLogger(XmlParser.class);
    Items xml = new Items();

    public Map<String, Item> parseItems(String xmlData) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Items.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            xml = (Items) unmarshaller.unmarshal(new StringReader(xmlData));
            Map<String, Item> items = new HashMap<>();
            if (xml != null && xml.getItems() != null) {
                for (Item item : xml.getItems()) {
                    items.put(item.getTca(), item);
                }
            }
            return items;
        } catch (Exception e) {
            logger.error(String.format("Unexpected error occurred during parsing data: %s", e.getMessage()));
            return new HashMap<>();
        }
    }
}