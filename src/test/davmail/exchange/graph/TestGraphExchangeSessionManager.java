/*
 * DavMail POP/IMAP/SMTP/CalDav/LDAP Exchange Gateway
 * Copyright (C) 2010  Mickael Guessant
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package davmail.exchange.graph;

import davmail.exception.HttpForbiddenException;
import junit.framework.TestCase;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Test GAL manager $batch request build and response parse, no network access.
 */
public class TestGraphExchangeSessionManager extends TestCase {

    public void testBuildBatchRequest() throws IOException, JSONException {
        Map<String, String> idToMail = new HashMap<>();
        JSONObject request = GraphExchangeSession.buildManagerBatchRequest(
                Arrays.asList("user1@example.com", "user2@example.com"), idToMail);

        JSONArray requests = request.getJSONArray("requests");
        assertEquals(2, requests.length());
        assertEquals("1", requests.getJSONObject(0).getString("id"));
        assertEquals("GET", requests.getJSONObject(0).getString("method"));
        assertEquals("/users/user1@example.com/manager?$select=displayName", requests.getJSONObject(0).getString("url"));
        assertEquals("2", requests.getJSONObject(1).getString("id"));
        assertEquals("user2@example.com", idToMail.get("2"));
    }

    public void testParseBatchResponse() throws IOException, JSONException {
        Map<String, String> idToMail = new HashMap<>();
        idToMail.put("1", "user1@example.com");
        idToMail.put("2", "user2@example.com");
        idToMail.put("3", "user3@example.com");
        idToMail.put("4", "user4@example.com");

        JSONObject response = new JSONObject().put("responses", new JSONArray()
                // manager found
                .put(new JSONObject().put("id", "1").put("status", 200)
                        .put("body", new JSONObject().put("displayName", "Big Boss")))
                // no manager
                .put(new JSONObject().put("id", "2").put("status", 404)
                        .put("body", new JSONObject().put("error", new JSONObject().put("code", "Request_ResourceNotFound"))))
                // unknown id, ignored
                .put(new JSONObject().put("id", "42").put("status", 200)
                        .put("body", new JSONObject().put("displayName", "Ignored")))
                // throttled sub request, left uncached
                .put(new JSONObject().put("id", "3").put("status", 429))
                // manager without display name
                .put(new JSONObject().put("id", "4").put("status", 200)
                        .put("body", new JSONObject())));

        Map<String, String> managers = GraphExchangeSession.parseManagerBatchResponse(response, idToMail);
        assertEquals("Big Boss", managers.get("user1@example.com"));
        assertTrue(managers.containsKey("user2@example.com"));
        assertNull(managers.get("user2@example.com"));
        assertNull(managers.get("user4@example.com"));
        assertFalse(managers.containsKey("user3@example.com"));
        assertEquals(3, managers.size());
    }

    public void testParseForbidden() throws JSONException {
        Map<String, String> idToMail = new HashMap<>();
        idToMail.put("1", "user1@example.com");

        JSONObject response = new JSONObject().put("responses", new JSONArray()
                .put(new JSONObject().put("id", "1").put("status", 403)
                        .put("body", new JSONObject().put("error", new JSONObject()
                                .put("code", "Authorization_RequestDenied")
                                .put("message", "Insufficient privileges")))));

        try {
            GraphExchangeSession.parseManagerBatchResponse(response, idToMail);
            fail("Should not get there");
        } catch (HttpForbiddenException e) {
            assertEquals("Authorization_RequestDenied", e.getCode());
        } catch (IOException e) {
            fail("Unexpected exception " + e);
        }
    }

    public void testInvalidBatchResponse() throws JSONException {
        try {
            GraphExchangeSession.parseManagerBatchResponse(new JSONObject(), new HashMap<>());
            fail("Should not get there");
        } catch (IOException e) {
            // expected
        }
    }
}
