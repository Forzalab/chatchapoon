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

    // Source - https://stackoverflow.com/a/14997413
    public static final int mod(int i, int n) {
        return (i % n + n) % n;
    }

    public static final int lerp(int s, int e, double t) {
        int result = (int)Math.round((1 - t) * s + t * e);
//        result -= (result == e) ? 1 : 0;
        return result;
    }

}
