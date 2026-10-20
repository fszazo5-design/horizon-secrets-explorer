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
    private val explorerParts = mutableListOf<ModelInstance>()
    private val objects = mutableListOf<ModelInstance>()
    private val cores = mutableListOf<Core>()
    private val celestial = mutableListOf<ModelInstance>()
    private val position = Vector3(0f, 1.25f, 5f)
    private var velocityY = 0f
    private var level = 0
    private var score = 0
    private var elapsed = 0f
    private var remainingTime = 145f
    private var levelCompleteSent = false
    private val colors = listOf(Color(0.10f, 0.62f, 0.72f, 1f))

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
        level = 0
        score = 0
        elapsed = 0f
        remainingTime = 145f - level * 12f
        levelCompleteSent = false
        position.set(0f, 1.25f, 5f)
        velocityY = 0f
        objects.clear()
        cores.clear()
        celestial.clear()
        explorerParts.clear()
        val builder = ModelBuilder()
        val accent = colors[0]
        val grassMaterial = Material(ColorAttribute.createDiffuse(Color(0.28f, 0.52f, 0.24f, 1f)))
        val roadMaterial = Material(ColorAttribute.createDiffuse(Color(0.22f, 0.22f, 0.20f, 1f)))
        val wallMaterial = Material(ColorAttribute.createDiffuse(Color(0.76f, 0.62f, 0.42f, 1f)))
        val roofMaterial = Material(ColorAttribute.createDiffuse(Color(0.45f, 0.16f, 0.10f, 1f)))
        val skinMaterial = Material(ColorAttribute.createDiffuse(Color(0.72f, 0.43f, 0.27f, 1f)))
        val shirtMaterial = Material(ColorAttribute.createDiffuse(Color(0.12f, 0.28f, 0.52f, 1f)))
        val pantsMaterial = Material(ColorAttribute.createDiffuse(Color(0.08f, 0.10f, 0.14f, 1f)))
        val ground = ModelInstance(builder.createBox(150f, 1.2f, 150f, grassMaterial, 1L or 8L))
        ground.transform.setToTranslation(0f, -5.8f, 0f)
        objects += ground
        val road = ModelInstance(builder.createBox(8f, 0.18f, 90f, roadMaterial, 1L or 8L))
        road.transform.setToTranslation(0f, -5.12f, 0f); objects += road
        for (i in 0..5) {
            val z = -22f + i * 8f
            val house = ModelInstance(builder.createBox(4.2f, 3.2f, 3.6f, wallMaterial, 1L or 8L))
            house.transform.setToTranslation(if (i % 2 == 0) -10f else 10f, -3.5f, z); objects += house
            val roof = ModelInstance(builder.createBox(4.8f, 1.2f, 4.2f, roofMaterial, 1L or 8L))
            roof.transform.setToTranslation(if (i % 2 == 0) -10f else 10f, -1.3f, z); objects += roof
        }
        for (i in 0..7) {
            val tree = ModelInstance(builder.createCylinder(0.45f, 3.2f, 0.45f, 10, Material(ColorAttribute.createDiffuse(Color(0.30f, 0.16f, 0.08f, 1f))), 1L or 8L))
            tree.transform.setToTranslation(if (i % 2 == 0) -5.8f else 5.8f, -3.55f, -28f + i * 7f); objects += tree
            val crown = ModelInstance(builder.createSphere(2.6f, 2.6f, 2.6f, 12, 10, Material(ColorAttribute.createDiffuse(Color(0.16f, 0.42f, 0.18f, 1f))), 1L or 8L))
            crown.transform.setToTranslation(if (i % 2 == 0) -5.8f else 5.8f, -0.8f, -28f + i * 7f); objects += crown
        }
        for (i in 0..5) {
            val corePosition = Vector3(-2.6f + (i % 2) * 5.2f, -4.45f, -22f + i * 7f)
            val core = ModelInstance(builder.createSphere(0.7f, 0.7f, 0.7f, 12, 12, Material(ColorAttribute.createDiffuse(Color.GOLD)), 1L or 8L))
            core.transform.setToTranslation(corePosition); cores += Core(core, corePosition)
        }
        explorer = ModelInstance(builder.createCylinder(1.25f, 2.6f, 1.25f, 16, shirtMaterial, 1L or 8L))
        val head = ModelInstance(builder.createSphere(1.55f, 1.55f, 1.55f, 16, 12, skinMaterial, 1L or 8L))
        val legL = ModelInstance(builder.createCylinder(0.42f, 2.3f, 0.42f, 12, pantsMaterial, 1L or 8L))
        val legR = ModelInstance(builder.createCylinder(0.42f, 2.3f, 0.42f, 12, pantsMaterial, 1L or 8L))
        val armL = ModelInstance(builder.createCylinder(0.28f, 2.1f, 0.28f, 12, skinMaterial, 1L or 8L))
        val armR = ModelInstance(builder.createCylinder(0.28f, 2.1f, 0.28f, 12, skinMaterial, 1L or 8L))
        explorerParts += explorer; explorerParts += head; explorerParts += legL; explorerParts += legR; explorerParts += armL; explorerParts += armR
        objects.addAll(explorerParts)
        updateExplorerModel()
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
        updateExplorerModel()
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
        camera.position.set(position.x + 13f, position.y + 10f, position.z + 16f)
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

    private fun updateExplorerModel() {
        if (explorerParts.size < 6) return
        val x = position.x; val y = position.y; val z = position.z
        explorerParts[0].transform.setToTranslation(x, y + 1.55f, z)
        explorerParts[1].transform.setToTranslation(x, y + 3.55f, z)
        explorerParts[2].transform.setToTranslation(x - 0.58f, y + 0.25f, z)
        explorerParts[3].transform.setToTranslation(x + 0.58f, y + 0.25f, z)
        explorerParts[4].transform.setToTranslation(x - 1.02f, y + 1.55f, z)
        explorerParts[5].transform.setToTranslation(x + 1.02f, y + 1.55f, z)
    }

    override fun dispose() { batch.dispose() }
}
