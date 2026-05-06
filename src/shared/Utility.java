package shared;

import java.io.*;

import org.json.*;

public class Utility {
    // https://stackoverflow.com/a/23377941
    public static final String optString(JSONObject json, String key)           {
        // http://code.google.com/p/android/issues/detail?id=13830
        if (json.isNull(key))
            return null;
        else
            return json.optString(key, null);
    }

    // https://stackoverflow.com/a/10174938
    public static final Boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {                                                                           new JSONArray(test);
            } catch (JSONException ex1) {
                System.out.println("Exception caught GameServer parsing JSON: " + ex1);
            }
        }
        return true;
    }
}
