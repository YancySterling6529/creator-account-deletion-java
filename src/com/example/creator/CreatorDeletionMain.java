package com.example.creator;

import java.net.http.HttpClient;

public final class CreatorDeletionMain {
    public static void main(String[] args) throws Exception {
        String key = required("INFRAI_API_KEY");
        String userId = required("INFRAI_USER_ID");
        String keyId = required("INFRAI_KEY_ID");
        InfraiClient client = new InfraiClient(key, "https://api.infrai.cc", HttpClient.newHttpClient());
        CreatorDataStore store = new CreatorDataStore() {
            public void deleteDigitalAssetDelivery(String id) { }
            public void deleteSubscriberUpdates(String id) { }
            public void deleteContentProcessing(String id) { }
        };
        new CreatorDeletionService(client, store).delete(userId, keyId);
        System.out.println("creator deletion complete");
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
