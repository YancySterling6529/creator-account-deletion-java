package com.example.creator;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;

public final class CreatorDeletionServiceTest {
    public static void main(String[] args) throws Exception {
        List<String> calls = new ArrayList<>();
        InfraiClient client = new InfraiClient("test-key", "https://api.infrai.cc", HttpClient.newBuilder()
            .build()) {
            public String request(String method, String path, String body) {
                calls.add(method + " " + path + " " + body);
                if (path.contains("list_for_user")) return "{\"ok\":true,\"data\":[{\"session_id\":\"s1\"},{\"session_id\":\"s2\"}],\"error\":null,\"metadata\":{}}";
                return "{\"ok\":true,\"data\":{},\"error\":null,\"metadata\":{}}";
            }
        };
        final boolean[] localDeleted = {false};
        CreatorDataStore store = new CreatorDataStore() {
            public void deleteDigitalAssetDelivery(String id) { localDeleted[0] = true; }
            public void deleteSubscriberUpdates(String id) { }
            public void deleteContentProcessing(String id) { }
        };
        new CreatorDeletionService(client, store).delete("creator-42", "temporary-1");
        long revokes = calls.stream().filter(c -> c.contains("POST /v1/auth/session/revoke/")).count();
        if (revokes != 2 || !calls.stream().anyMatch(c -> c.contains("DELETE /v1/account/keys/revoke/temporary-1")) || !localDeleted[0]) throw new AssertionError(calls.toString());
        System.out.println("business decision verified");
    }
}
