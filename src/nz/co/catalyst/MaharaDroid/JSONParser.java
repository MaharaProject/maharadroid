package nz.co.catalyst.MaharaDroid;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONParser {

	// Path should be of the form "<object name>/<object name>/<object name>"
	public static JSONObject getObject(JSONObject obj, String path) {
		JSONObject r_obj = null;
		String[] path_arr = path.split("/");
		
		try {
			for (int i = 0; i < path_arr.length; ++i) {
				if (obj == null || !obj.has(path_arr[i])) {
					// Something is wrong with the path, or the object
					// requested does not exist.
					break;
				}

				obj = obj.getJSONObject(path_arr[i]);

				if (i == path_arr.length - 1){
					// This is the last entry in the path.
					r_obj = obj;
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return r_obj;
	}

	public static JSONArray getArray(JSONObject obj, String path) {
		JSONArray r_obj = null;
		String partial_path = path.contains("/") ? path.substring(0,path.lastIndexOf("/")) : "";
		String array_name = path.substring(path.lastIndexOf("/") + 1);

		try {
			JSONObject json_obj = partial_path.equals("") ? obj : getObject(obj, partial_path);
			if (json_obj != null && json_obj.has(array_name)) {
				r_obj = json_obj.getJSONArray(array_name);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return r_obj;
	}

	public static String getString(JSONObject obj, String path) {
		String r_str = null;
		String partial_path = path.contains("/") ? path.substring(0,path.lastIndexOf("/")) : "";
		String string_name = path.substring(path.lastIndexOf("/") + 1);

		try {
			JSONObject json_obj = partial_path.equals("") ? obj : getObject(obj, partial_path);
			if (json_obj != null && json_obj.has(string_name)) {
				r_str = json_obj.getString(string_name);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return r_str;
	}

	public static Integer getInt(JSONObject obj, String path) {
		Integer r_int = 0;
		String partial_path = path.contains("/") ? path.substring(0,path.lastIndexOf("/")) : "";
		String string_name = path.substring(path.lastIndexOf("/") + 1);

		try {
			JSONObject json_obj = partial_path.equals("") ? obj : getObject(obj, partial_path);
			if (json_obj != null && json_obj.has(string_name)) {
				r_int = json_obj.getInt(string_name);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return r_int;
	}
}
