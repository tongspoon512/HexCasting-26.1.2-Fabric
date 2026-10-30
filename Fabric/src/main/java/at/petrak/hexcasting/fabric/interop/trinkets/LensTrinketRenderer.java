package at.petrak.hexcasting.fabric.interop.trinkets;

import dev.emi.trinkets.api.client.TrinketRenderer;

/**
 * 26.1 replaced the static item renderer with the render-state / submit pipeline, so the trinkets
 * renderer stub no longer has anything to hook into. Kept as a registered no-op renderer.
 *
 * @author WireSegal
 * Created at 9:50 AM on 7/25/22.
 */
public class LensTrinketRenderer implements TrinketRenderer {
}
