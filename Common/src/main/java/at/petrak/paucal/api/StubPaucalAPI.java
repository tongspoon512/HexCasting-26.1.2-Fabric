package at.petrak.paucal.api;

/**
 * No-op implementation of PaucalAPI used for the 26.1.2 port.
 */
final class StubPaucalAPI implements PaucalAPI {
    static final StubPaucalAPI INSTANCE = new StubPaucalAPI();

    private StubPaucalAPI() {
    }
}
