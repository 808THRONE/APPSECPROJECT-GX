package com.securegate.api.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.MediaType;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

@Provider
@PreMatching
public class XssSanitizationFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(XssSanitizationFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        MediaType mediaType = requestContext.getMediaType();

        if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))
                && mediaType != null && mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {

            InputStream is = requestContext.getEntityStream();
            byte[] body = is.readAllBytes();
            if (body.length == 0)
                return;

            try {
                InputStream bis = new ByteArrayInputStream(body);
                JsonValue jsonValue = Json.createReader(bis).read();
                JsonValue sanitizedValue = sanitizeJson(jsonValue);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Json.createWriter(baos).write(sanitizedValue);
                requestContext.setEntityStream(new ByteArrayInputStream(baos.toByteArray()));

                LOGGER.fine("JSON payload sanitized for XSS protection");
            } catch (Exception e) {
                // If parsing fails, we reset the stream so original body can be processed (or
                // fail downstream gracefully)
                requestContext.setEntityStream(new ByteArrayInputStream(body));
                LOGGER.warning("Failed to sanitize JSON body: " + e.getMessage());
            }
        }
    }

    private JsonValue sanitizeJson(JsonValue value) {
        switch (value.getValueType()) {
            case OBJECT:
                JsonObject obj = value.asJsonObject();
                JsonObjectBuilder objBuilder = Json.createObjectBuilder();
                obj.forEach((key, val) -> objBuilder.add(key, sanitizeJson(val)));
                return objBuilder.build();
            case ARRAY:
                JsonArray arr = value.asJsonArray();
                JsonArrayBuilder arrBuilder = Json.createArrayBuilder();
                arr.forEach(val -> arrBuilder.add(sanitizeJson(val)));
                return arrBuilder.build();
            case STRING:
                return Json.createValue(sanitizeString(value.toString().replaceAll("^\"|\"$", "")));
            default:
                return value;
        }
    }

    private String sanitizeString(String input) {
        if (input == null)
            return null;
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '&':
                    out.append("&amp;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                case '\'':
                    out.append("&#39;");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }
}
