package br.com.clinicaestetica.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class Json {
    private Json() {
    }

    public static JSONObject object(String json) {
        if (json == null || json.isEmpty()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            throw new IllegalStateException("JSON inválido no banco", e);
        }
    }

    public static String array(Collection<String> values) {
        return new JSONArray(values).toString();
    }

    public static Set<String> stringSet(String json) {
        if (json == null || json.isEmpty()) {
            return new LinkedHashSet<>();
        }
        try {
            return stringSet(new JSONArray(json));
        } catch (JSONException e) {
            throw new IllegalStateException("JSON inválido no banco", e);
        }
    }

    public static Set<String> stringSet(JSONArray array) {
        Set<String> values = new LinkedHashSet<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                values.add(array.optString(i));
            }
        }
        return values;
    }
}
