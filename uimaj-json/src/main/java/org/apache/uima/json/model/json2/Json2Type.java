package org.apache.uima.json.model.json2;

import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.json.Json2Names.COMPONENT_TYPE_FIELD;
import static org.apache.uima.json.Json2Names.NAME_FIELD;
import static org.apache.uima.json.Json2Names.SUPER_TYPE_FIELD;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = { "name", "parent", "componentType" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class Json2Type
{
    @JsonProperty(value = NAME_FIELD, required = false)
    private String name;

    @JsonProperty(value = SUPER_TYPE_FIELD, required = true, defaultValue = TYPE_NAME_ANNOTATION)
    private String parent;

    @JsonProperty(value = COMPONENT_TYPE_FIELD, required = false)
    private String componentType;

    private Map<String, String> features = new LinkedHashMap<>();

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public String getParent()
    {
        return parent;
    }

    public void setParent(String aParent)
    {
        parent = aParent;
    }

    public String getComponentType()
    {
        return componentType;
    }

    public void setComponentType(String aComponentType)
    {
        componentType = aComponentType;
    }

    @JsonAnySetter
    public void setProperty(String name, Object value)
    {
        if (value instanceof String) {
            features.put(name, (String) value);
            return;
        }

        throw new IllegalArgumentException("Feature type names must be strings");
    }

    @SuppressWarnings("unchecked")
    @JsonAnyGetter
    public Map<String, Object> getProperties()
    {
        return (Map) features;
    }
}
