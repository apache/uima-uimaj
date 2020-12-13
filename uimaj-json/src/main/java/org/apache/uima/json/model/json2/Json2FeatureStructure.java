package org.apache.uima.json.model.json2;

import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.json.Json2Names.ID_FIELD;
import static org.apache.uima.json.Json2Names.TYPE_FIELD;
import static org.apache.uima.json.Json2Names.VIEWS_FIELD;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.uima.json.Json2Names;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = { "id", "type", "views" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class Json2FeatureStructure
{
    @JsonProperty(value = ID_FIELD, required = false)
    private String id;

    @JsonProperty(value = TYPE_FIELD, required = true, defaultValue = TYPE_NAME_ANNOTATION)
    private String type;

    @JsonProperty(value = Json2Names.FLAGS_FIELD, required = false)
    private LinkedHashSet<String> flags;

    @JsonProperty(value = VIEWS_FIELD, required = false)
    private LinkedHashSet<String> views;

    private Map<String, Object> features = new LinkedHashMap<>();

    public String getId()
    {
        return id;
    }

    public void setId(String aId)
    {
        id = aId;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String aType)
    {
        type = aType;
    }

    public LinkedHashSet<String> getViews()
    {
        return views;
    }

    public void setViews(LinkedHashSet<String> aViews)
    {
        views = aViews;
    }
    
    public LinkedHashSet<String> getFlags()
    {
        return flags;
    }

    public void setFlags(LinkedHashSet<String> aFlags)
    {
        flags = aFlags;
    }

    @JsonAnySetter
    public void setFeature(String name, Object value)
    {
        if (name.startsWith(Json2Names.REF)) {
            name = name.substring(1);
        }
        
        features.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getFeatures()
    {
        return features;
    }
}
