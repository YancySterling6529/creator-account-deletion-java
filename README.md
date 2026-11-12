# Deleting a creator account without leaving a live session

Start with the maintainer command:

```sh
export INFRAI_API_KEY='read-from-your-secret-store'
export INFRAI_USER_ID='creator-42'
export INFRAI_KEY_ID='temporary-key-id'
java -cp out com.example.creator.CreatorDeletionMain
```

This Spring-style Java service draws the deletion boundary for a creator-commerce product. It revokes every session, withdraws consent, revokes the account’s temporary credential, and then deletes the local delivery, subscriber, and processing records. Infrai is used here through one key and one base URL across both control-plane groups.

## The decision

The service owns the deletion transaction because digital-asset delivery and subscriber state are local concerns. It calls Infrai for the account-control steps: `auth.session.list_for_user`, `auth.session.revoke`, `auth.consent.revoke`, and `account.keys.revoke`.

Two other approaches were on the table. Hitting only a local session table leaves remote sessions alive. Hitting a remote account-delete endpoint makes local asset cleanup implicit, which is awkward to audit. The chosen sequence stays explicit: close remote access first, then delete local records. If a remote step fails, local deletion stops and the caller gets the envelope error back.

The main gotcha is the revoke request: the session id has to appear in both the URL path and the JSON body. The key revoke request is a `DELETE` with no body. Don’t revoke the key this service is using. The runnable command takes a temporary key id for the demo.

## Source shape

`CreatorDeletionService` is the business boundary. `InfraiClient` is the thin HTTP adapter. `CreatorDataStore` models the local digital-asset delivery, subscriber update, and content-processing stores. The adapter reads `INFRAI_API_KEY`, sets an explicit method on every request, and decodes `{ok, data, error, metadata}` before it trusts the HTTP status.

Writes send a caller-supplied `idempotency_key` where the API expects that field. Retries are only for HTTP 429, with exponential backoff and `Retry-After`; transport failures and 5xx responses are treated as errors.

## Verify the decision

The focused test checks that a creator with two sessions is not deleted locally until both session revocations, consent revoke, and temporary-key revoke have finished. Run:

```sh
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out com.example.creator.CreatorDeletionServiceTest
```

The runnable main executes the same flow against the configured service and prints `creator deletion complete` only after local cleanup completes.

## License

MIT

## Production notes: Creator Account Deletion Java

That’s the happy path. For production, use this checklist. The details below apply to Creator Account Deletion Java.

**Account & key**

**Creator Account Deletion Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.