package io.opensaber.registry.util;

import io.opensaber.registry.middleware.util.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IndexHelper {
    private static Logger logger = LoggerFactory.getLogger(IndexHelper.class);

    /**
     * Holds mapping for each shard & each definitions and its index status 
     * key = shardId+definitionName value = true/false
     */
    private Map<String, Boolean> definitionIndexMap = new ConcurrentHashMap<String, Boolean>();

    public void setDefinitionIndexMap(Map<String, Boolean> definitionIndexMap) {
        this.definitionIndexMap.putAll(definitionIndexMap);
    }

    public void updateDefinitionIndex(String label, String definitionName, boolean flag) {
        String key = label + definitionName;
        definitionIndexMap.put(key, flag);
    }

    /**
     * Checks any new index available for index creation
     * 
     * @param parentVertex
     * @param definition
     * @return
     */
    public boolean isIndexPresent(Definition definition, String shardId) {
        String defTitle = definition.getTitle();
        boolean isIndexPresent = definitionIndexMap.getOrDefault(shardId + defTitle, false);
        logger.debug("isIndexPresent flag for {}: {}", defTitle, isIndexPresent);
        return isIndexPresent;
    }
    
    /**
     * Updates INDEX_FIELDS(single, composite) and UNIQUE_INDEX_FIELDS property of parent vertex
     * @param parentVertex
     */
    public void updateParentIndexProperty(Vertex parentVertex, List<String> indexFields, List<String> uniqueIndexFields) { 

        //Non-unique
        if (indexFields.size() > 0) {
            replaceVertexProperty(parentVertex, indexFields, false);
        }
        //unique
        if(uniqueIndexFields.size() > 0){
            replaceVertexProperty(parentVertex, uniqueIndexFields, true);
  
        }
    }
    /**
     * Replace the INDEX_FIELDS and UNIQUE_INDEX_FIELDS of parent vertex
     * @param parentVertex
     * @param properties
     * @param isUnique
     */
    private void replaceVertexProperty(Vertex parentVertex, List<String> indexFields, boolean isUnique){
        String propertyName = isUnique ? Constants.UNIQUE_INDEX_FIELDS : Constants.INDEX_FIELDS;
        StringBuilder properties = new StringBuilder(String.join(",", indexFields));
        parentVertex.property(propertyName, properties.toString());
        logger.info("parent vertex property {}:{}", propertyName, properties);
    }


    /**
     * Identifies new fields for creating index. Parent vertex are always have
     * INDEX_FIELDS and UNIQUE_INDEX_FIELDS property
     * 
     * @param parentVertex
     * @param fields
     * @param isUnique
     */
    public List<String> getNewFields(Vertex parentVertex, List<String> fields, boolean isUnique) {
        List<String> newFields = new ArrayList<>();
        String propertyName = isUnique ? Constants.UNIQUE_INDEX_FIELDS : Constants.INDEX_FIELDS;
        String values = "";
        try {
            values = (String) parentVertex.property(propertyName).value();
        } catch (java.lang.IllegalStateException ise) {
            // The property doesn't exist.
            values = "";
        }
        for (String field : fields) {
            if (!values.contains(field) && !newFields.contains(field))
                newFields.add(field);
        }
        return newFields;
    }

    /**
     * extract values between "( values with comma separated )"
     * @param fields
     * @return
     */
    public static List<String> getCompositeIndexFields(List<String> fields) {
        if (fields.size() > 0) {
            StringBuilder list = new StringBuilder(String.join(",", fields));
            String subString = StringUtils.substringBetween(list.toString(), "(", ")").replaceAll("'", "");
            String[] commaSeparatedArr = subString.split("\\s*,\\s*");
            List<String> result = Arrays.stream(commaSeparatedArr).collect(Collectors.toList());
            return result;
        } else {
            return new ArrayList<>();
        }

    }
    /**
     * Remove fields with format = "( values with comma separated )"
     * @param fields
     * @return
     */
    public static List<String> getSingleIndexFields(List<String> fields) {
        if (fields.size() > 0) {
            StringBuilder list = new StringBuilder(String.join(",", fields));
            String subString = (StringUtils.substringBetween(list.toString(), "(", ")"));
            String orginal = list.toString().replaceAll(subString, "").replaceAll(Pattern.quote("()"), "");

            String[] commaSeparatedArr = orginal.split(",");
            List<String> result = Arrays.stream(commaSeparatedArr).collect(Collectors.toList());
            return result;
        } else {
            return new ArrayList<>();

        }

    }

}