package at.petrak.hexcasting.client.render

import at.petrak.hexcasting.api.HexAPI
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.resources.Identifier
import org.joml.Matrix4f
import org.joml.Matrix4fc

/**
 * A single vertex, expressed in the local space of the [Mesh] that owns it.
 */
class MeshVertex(
    @JvmField val x: Float,
    @JvmField val y: Float,
    @JvmField val z: Float,
    @JvmField val color: Int,
    @JvmField val u: Float,
    @JvmField val v: Float,
)

/**
 * One finished batch: a draw mode, a transform, a texture, and the vertices to push through them.
 *
 * 26.1 removed immediate-mode custom geometry (no Tesselator, no RenderSystem shader swaps), so everything
 * we draw is built into these lists and handed to whichever [MeshSink] the caller owns -- the world's
 * SubmitNodeCollector, or the GUI's render-state queue.
 */
class Mesh(
    @JvmField val mode: VertexFormat.Mode,
    @JvmField val transform: Matrix4fc,
    @JvmField val texture: Identifier,
    @JvmField val vertices: List<MeshVertex>,
)

fun interface MeshSink {
    fun submit(mesh: Mesh)
}

/**
 * Collects geometry instead of drawing it: [begin], push vertices, [end]; repeat, then [submitTo].
 */
class MeshCollector {
    private val meshes: MutableList<Mesh> = ArrayList()
    private var open: MutableList<MeshVertex>? = null
    private var openMode: VertexFormat.Mode = VertexFormat.Mode.TRIANGLES
    private var openTransform: Matrix4fc = Matrix4f()
    private var openTexture: Identifier = WHITE

    fun begin(
        mode: VertexFormat.Mode,
        transform: Matrix4fc,
        texture: Identifier = WHITE
    ): MeshCollector {
        this.end()
        this.openMode = mode
        this.openTransform = transform
        this.openTexture = texture
        this.open = ArrayList()
        return this
    }

    fun vertex(x: Float, y: Float, z: Float, color: Int, u: Float, v: Float) {
        val list = this.open ?: return
        list.add(MeshVertex(x, y, z, color, u, v))
    }

    fun end() {
        val list = this.open ?: return
        if (list.isNotEmpty()) {
            this.meshes.add(Mesh(this.openMode, this.openTransform, this.openTexture, list))
        }
        this.open = null
    }

    fun isEmpty(): Boolean = this.meshes.isEmpty() && (this.open?.isEmpty() ?: true)

    fun submitTo(sink: MeshSink) {
        this.end()
        for (mesh in this.meshes) {
            sink.submit(mesh)
        }
        this.meshes.clear()
    }

    companion object {
        @JvmField
        val WHITE: Identifier = HexAPI.modLoc("textures/entity/white.png")
    }
}
