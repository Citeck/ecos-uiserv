package ru.citeck.ecos.uiserv.service.icon.dto;

import lombok.Data;
import lombok.ToString;
import org.springframework.util.MimeType;
import ru.citeck.ecos.commons.data.ObjectData;

import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@ToString(exclude = {"data"})
public class IconDto {

    private String id;
    private String family;

    private String type;
    private ObjectData config;
    private byte[] data;
    private MimeType mimetype;

    private Instant modified;

    public void setData(String base64Content) {
        Pattern pattern = Pattern.compile("^data:(.+?);base64,(.+)$");
        Matcher matcher = pattern.matcher(base64Content);
        if (!matcher.find()) {
            throw new IllegalStateException("Incorrect data: " + base64Content);
        }
        this.mimetype = MimeType.valueOf(matcher.group(1));
        this.data = Base64.getDecoder().decode(matcher.group(2));
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getData() {
        if (data != null && mimetype != null) {
            return "data:" + mimetype + ";base64," + Base64.getEncoder().encodeToString(data);
        }
        return null;
    }

    public byte[] getByteData() {
        return this.data;
    }
}
