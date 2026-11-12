package com.example.creator;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CreatorDeletionService {
    private final InfraiClient infrai;
    private final CreatorDataStore store;

    public CreatorDeletionService(InfraiClient infrai, CreatorDataStore store) {
        this.infrai = infrai;
        this.store = store;
    }

    public void delete(String userId, String temporaryKeyId) throws IOException, InterruptedException {
        String sessions = infrai.request("GET", "/v1/auth/session/list_for_user/" + userId, null);
        Matcher matcher = Pattern.compile("session_id\\\"\\s*:\\s*\\\"([^\"]+)").matcher(sessions);
        while (matcher.find()) {
            String sessionId = matcher.group(1);
            String body = "{\"session_id\":\"" + sessionId + "\"}";
            infrai.request("POST", "/v1/auth/session/revoke/" + sessionId, body);
        }
        infrai.request("POST", "/v1/auth/consent/revoke/" + userId,
            "{\"user_id\":\"" + userId + "\",\"category\":\"essential\"}");
        infrai.request("DELETE", "/v1/account/keys/revoke/" + temporaryKeyId, null);
        store.deleteDigitalAssetDelivery(userId);
        store.deleteSubscriberUpdates(userId);
        store.deleteContentProcessing(userId);
    }
}
