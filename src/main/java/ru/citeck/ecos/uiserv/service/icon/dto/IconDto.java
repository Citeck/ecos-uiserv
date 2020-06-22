package ru.citeck.ecos.uiserv.service.icon.dto;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import lombok.Data;
import lombok.ToString;
import ru.citeck.ecos.commons.data.ObjectData;

import java.time.Instant;

@Data
@ToString(exclude = {"data"})
public class IconDto {

    private String id;
    private String family;

    private String type;
    private ObjectData config;
    private byte[] data;

    private Instant modified;

    public void setData(String dataStr) {
        this.data = Base64.decode(dataStr);
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
