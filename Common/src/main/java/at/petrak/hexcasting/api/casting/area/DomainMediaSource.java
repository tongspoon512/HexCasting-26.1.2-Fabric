package at.petrak.hexcasting.api.casting.area;

/**
 * Something that can pay a domain's media cost instead of the caster, such as a domain core block.
 */
public interface DomainMediaSource {
    /**
     * Tries to take {@code cost} media out of this source.
     *
     * @return how much media was actually taken (or could be taken, if {@code simulate} is true)
     */
    long extractMedia(long cost, boolean simulate);

    /**
     * Called when the domain this source was bound to is closed, so the source can clean up.
     */
    void onDomainClosed();
}
