package com.horizon.secrets.game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.horizon.secrets.domain.HudState
import kotlin.math.cos
import kotlin.math.sin

class HorizonGame(
    private val touch: TouchState,
    private val onHud: (HudState) -> Unit,
    private val onLevelCompleted: (level: Int, score: Int) -> Unit,
) : ApplicationAdapter() {
    private lateinit var batch: ModelBatch
    private lateinit var environment: Environment
    private lateinit var camera: PerspectiveCamera
    private lateinit var explorer: ModelInstance
    private val objects = mutableListOf<ModelInstance>()
    private val cores = mutableListOf<Core>()
    private val position = Vector3(0f, 1.25f, 5f)
    private var velocityY = 0f
    private var level = 0
    private var score = 0
    private var elapsed = 0f
    private var remainingTime = 145f
    private var levelCompleteSent = false
    private val colors = listOf(Color(0.10f, 0.62f, 0.72f, 1f), Color(0.42f, 0.28f, 0.72f, 1f), Color(0.10f, 0.72f, 0.50f, 1f), Color(0.86f, 0.45f, 0.20f, 1f))

    private data class Core(val instance: ModelInstance, val position: Vector3, var collected: Boolean = false)

    override fun create() {
        batch = ModelBatch()
        environment = Environment().apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.55f, 0.62f, 0.78f, 1f))
            add(DirectionalLight().set(0.9f, 0.85f, 0.72f, -0.5f, -1f, -0.35f))
        }
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
            near = 0.1f
            far = 180f
        }
        startLevel(0)
    }

    fun startLevel(index: Int) {
        level = index.coerceIn(0, 3)
        score = 0
        elapsed = 0f
        remainingTime = 145f - level * 12f
        levelCompleteSent = false
        position.set(0f, 1.25f, 5f)
        velocityY = 0f
        objects.clear()
        cores.clear()
        val builder = ModelBuilder()
        val accent = colors[level]
        val platformMaterial = Material(ColorAttribute.createDiffuse(Color(0.035f, 0.16f, 0.23f, 1f)))
        val edgeMaterial = Material(ColorAttribute.createDiffuse(accent))
        for (i in 0..9) {
            val angle = i * 0.63f
            val radius = 5.5f + (i % 3) * 2.2f
            val platform = ModelInstance(builder.createBox(4.2f, 0.55f, 4.2f, platformMaterial, 1L or 8L))
            platform.transform.setToTranslation(cos(angle) * radius, -0.35f + (i % 4) * 0.55f, sin(angle) * radius)
            objects += platform
            val crystal = ModelInstance(builder.createBox(0.28f, 1.2f, 0.28f, Material(ColorAttribute.createDiffuse(accent)), 1L or 8L))
            crystal.transform.setToTranslation(cos(angle) * radius, 0.45f + (i % 4) * 0.55f, sin(angle) * radius)
            objects += crystal
            if (i % 2 == 0) {
                val core = ModelInstance(builder.createSphere(0.55f, 0.55f, 0.55f, 12, 12, Material(ColorAttribute.createDiffuse(Color.GOLD)), 1L or 8L))
                val corePosition = Vector3(cos(angle) * radius, 1.3f + (i % 4) * 0.55f, sin(angle) * radius)
                core.transform.setToTranslation(corePosition)
                cores += Core(core, corePosition)
            }
        }
        val gate = ModelInstance(builder.createCylinder(3.2f, 0.45f, 0.45f, 32, edgeMaterial, 1L or 8L))
        gate.transform.setToTranslation(0f, 2.4f, -12f)
        objects += gate
        explorer = ModelInstance(builder.createBox(0.75f, 1.7f, 0.55f, Material(ColorAttribute.createDiffuse(Color(0.20f, 0.16f, 0.52f, 1f))), 1L or 8L))
        explorer.transform.setToTranslation(position)
        objects += explorer
    }

    override fun render() {
        val dt = Gdx.graphics.deltaTime.coerceAtMost(0.05f)
        elapsed += dt
        remainingTime = (remainingTime - dt).coerceAtLeast(0f)
        val speed = 5.2f + level * 0.25f
        position.x += touch.moveX * speed * dt
        position.z -= touch.moveY * speed * dt
        if (touch.consumeJump() && position.y <= 1.3f) velocityY = 8.2f
        velocityY -= 18f * dt
        position.y = (position.y + velocityY * dt).coerceAtLeast(1.25f).also { if (it <= 1.25f) velocityY = 0f }
        position.x = position.x.coerceIn(-22f, 22f)
        position.z = position.z.coerceIn(-24f, 18f)
        explorer.transform.setToTranslation(position)
        for (core in cores) {
            if (!core.collected && position.dst(core.position) < 1.65f) {
                core.collected = true
                core.instance.transform.setToTranslation(0f, -200f, 0f)
                score += 100 + level * 25
            }
        }
        val collected = cores.count { it.collected }
        if (!levelCompleteSent && collected == cores.size) {
            levelCompleteSent = true
            score += remainingTime.toInt() * 3
            onLevelCompleted(level, score)
        }
        camera.position.set(position.x + 8f, position.y + 7f, position.z + 10f)
        camera.lookAt(position.x, position.y + 0.4f, position.z)
        camera.up.set(Vector3.Y)
        camera.update()
        onHud(HudState(level, collected, cores.size, score, remainingTime.toInt(), if (collected == cores.size) "عاد النور إلى المدار" else "وازن خطواتك واتبع شظايا النور"))
        Gdx.gl.glClearColor(0.02f, 0.05f, 0.11f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        batch.begin(camera)
        objects.forEach { batch.render(it, environment) }
        cores.filterNot { it.collected }.forEach { batch.render(it.instance, environment) }
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
    }

    override fun dispose() { batch.dispose() }
}
