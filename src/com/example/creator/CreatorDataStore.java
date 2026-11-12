package com.example.creator;

public interface CreatorDataStore {
    void deleteDigitalAssetDelivery(String userId);
    void deleteSubscriberUpdates(String userId);
    void deleteContentProcessing(String userId);
}
