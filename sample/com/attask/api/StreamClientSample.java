package com.attask.api;

/*
 * Copyright (c) 2011 AtTask, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class StreamClientSample {

	public static void main(String[] args) {
		StreamClient client = new StreamClient("http://localhost:8080/attask/api");

		try {
			// Login
			System.out.print("Logging in...");
			JSONObject session = client.login("admin", "user");
			System.out.println("done");

			// Get user
			System.out.print("Retrieving user...");
			JSONObject user = client.get("user", session.getString("userID"), new String[]{"ID", "homeGroupID", "emailAddr"});
			System.out.println("done");

			// Search projects
			System.out.print("Searching projects...");
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("groupID", user.getString("homeGroupID"));
			JSONArray results = client.search("proj", map, new String[]{"ID", "name"});
			System.out.println("done");

			for (int i=0; i<Math.min(10, results.length()); i++) {
				System.out.println(" - " + results.getJSONObject(i).getString("name"));
			}

			// Create project
			System.out.print("Creating project...");
			map.clear();
			map.put("name", "My Project");
			map.put("groupID", user.getString("homeGroupID"));			
			JSONObject proj = client.post("proj", map);
			System.out.println("done");

			// Get project
			System.out.print("Retrieving project...");
			proj = client.get("proj", proj.getString("ID"));
			System.out.println("done");

			// Edit project
			System.out.print("Editing project...");
			map.clear();
			map.put("name", "Your Project");
			proj = client.put("proj", proj.getString("ID"), map);
			System.out.println("done");

			// Delete project
			System.out.print("Deleting project...");
			client.delete("proj", proj.getString("ID"));
			System.out.println("done");

			// Logout
			System.out.print("Logging out...");
			client.logout();
			System.out.println("done");
		}
		catch (StreamClientException e) {
			System.out.println(e.getMessage());
		}
		catch (JSONException e) {
			System.out.println(e.getMessage());
		}
	}

}
