package com.horizon.secrets.game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.horizon.secrets.domain.HudState
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneManager
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class HorizonGame(
    private val touch: TouchState,
    private val onHud: (HudState) -> Unit,
    private val onLevelCompleted: (level: Int, score: Int) -> Unit,
) : ApplicationAdapter() {
    private lateinit var batch: ModelBatch
    private lateinit var environment: Environment
    private lateinit var camera: PerspectiveCamera
    private lateinit var sceneManager: SceneManager
    private val objects = mutableListOf<ModelInstance>()
    private val cores = mutableListOf<Core>()
    private val worldModels = mutableListOf<Model>()
    private val glbLoader = GLBLoader()
    private var heroAsset: SceneAsset? = null
    private var playerScene: Scene? = null
    private var selectedCharacter = 0
    private var currentAnimation: String? = null
    private var facingAngle = 0f
    private val position = Vector3(0f, 1.25f, 5f)
    private var velocityY = 0f
    private var level = 0
    private var score = 0
    private var elapsed = 0f
    private var remainingTime = 145f
    private var levelCompleteSent = false
    private val heroFiles = listOf("harper", "sam", "maya")
    private val colors = listOf(
        Color(0.10f, 0.62f, 0.72f, 1f),
        Color(0.42f, 0.28f, 0.72f, 1f),
        Color(0.10f, 0.72f, 0.50f, 1f),
        Color(0.86f, 0.45f, 0.20f, 1f),
    )

    private data class Core(val instance: ModelInstance, val position: Vector3, var collected: Boolean = false)

    override fun create() {
        batch = ModelBatch()
        environment = Environment().apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.55f, 0.62f, 0.78f, 1f))
            add(DirectionalLight().set(0.9f, 0.85f, 0.72f, -0.5f, -1f, -0.35f))
        }
        sceneManager = SceneManager(24).apply {
            setAmbientLight(0.78f)
            environment.add(DirectionalLight().set(0.85f, 0.82f, 0.76f, -0.45f, -1f, -0.35f))
        }
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
            near = 0.1f
            far = 180f
        }
        sceneManager.setCamera(camera)
        startLevel(0)
        selectCharacter(0)
    }

    /** Must be called on LibGDX's render thread. */
    fun selectCharacter(index: Int) {
        val nextCharacter = index.coerceIn(0, heroFiles.lastIndex)
        if (nextCharacter == selectedCharacter && playerScene != null) return
        playerScene?.let { previous ->
            previous.animations.stopAll()
            sceneManager.removeScene(previous)
        }
        heroAsset?.dispose()
        selectedCharacter = nextCharacter
        heroAsset = glbLoader.load(Gdx.files.internal("characters/${heroFiles[selectedCharacter]}.glb"))
        playerScene = Scene(heroAsset!!.scene).also { sceneManager.addScene(it, false) }
        currentAnimation = null
        setPlayerAnimation("Idle_Standing")
        updatePlayerTransform()
    }

    fun startLevel(index: Int) {
        level = index.coerceIn(0, 3)
        score = 0
        elapsed = 0f
        remainingTime = 145f - level * 12f
        levelCompleteSent = false
        position.set(0f, 1.25f, 5f)
        velocityY = 0f
        clearWorld()

        val builder = ModelBuilder()
        val accent = colors[level]
        val platformMaterial = Material(ColorAttribute.createDiffuse(Color(0.035f, 0.16f, 0.23f, 1f)))
        val edgeMaterial = Material(ColorAttribute.createDiffuse(accent))
        val crystalMaterial = Material(ColorAttribute.createDiffuse(accent))
        val goldMaterial = Material(ColorAttribute.createDiffuse(Color.GOLD))
        val paleMaterial = Material(ColorAttribute.createDiffuse(Color(0.58f, 0.84f, 1f, 1f)))
        val rockMaterial = Material(ColorAttribute.createDiffuse(Color(0.12f, 0.20f, 0.31f, 1f)))
        val platformModel = ownWorld(builder.createBox(4.2f, 0.55f, 4.2f, platformMaterial, 1L or 8L))
        val crystalModel = ownWorld(builder.createBox(0.28f, 1.2f, 0.28f, crystalMaterial, 1L or 8L))
        val coreModel = ownWorld(builder.createSphere(0.55f, 0.55f, 0.55f, 12, 12, goldMaterial, 1L or 8L))
        val shardModel = ownWorld(builder.createCone(0.42f, 1.15f, 0.42f, 6, crystalMaterial, 1L or 8L))
        val smallShardModel = ownWorld(builder.createCone(0.26f, 0.72f, 0.26f, 5, paleMaterial, 1L or 8L))
        val beaconModel = ownWorld(builder.createCylinder(0.30f, 1.75f, 0.30f, 8, edgeMaterial, 1L or 8L))
        val beaconLightModel = ownWorld(builder.createSphere(0.52f, 0.52f, 0.52f, 10, 10, paleMaterial, 1L or 8L))
        val archPostModel = ownWorld(builder.createBox(0.34f, 2.35f, 0.38f, edgeMaterial, 1L or 8L))
        val archTopModel = ownWorld(builder.createBox(2.75f, 0.38f, 0.48f, goldMaterial, 1L or 8L))
        val rubbleModel = ownWorld(builder.createBox(0.54f, 0.48f, 0.62f, rockMaterial, 1L or 8L))
        val moonModel = ownWorld(builder.createSphere(0.72f, 0.72f, 0.72f, 10, 10, rockMaterial, 1L or 8L))
        val starModels = listOf(
            ownWorld(builder.createSphere(0.16f, 0.16f, 0.16f, 8, 8, paleMaterial, 1L or 8L)),
            ownWorld(builder.createSphere(0.24f, 0.24f, 0.24f, 8, 8, goldMaterial, 1L or 8L)),
        )

        for (i in 0..9) {
            val angle = i * 0.63f
            val radius = 5.5f + (i % 3) * 2.2f
            val x = cos(angle) * radius
            val z = sin(angle) * radius
            val baseY = -0.35f + (i % 4) * 0.55f
            addWorld(platformModel, x, baseY, z)
            addWorld(crystalModel, x, baseY + 0.80f, z)

            // Three smaller crystal spires add a distinct silhouette to every floating island.
            for (shard in 0..2) {
                val model = if (shard == 1) shardModel else smallShardModel
                val offset = (shard - 1) * 0.66f
                addWorld(model, x + offset, baseY + 0.92f, z + if (shard == 1) 0.72f else -0.58f, rotationZ = (shard - 1) * 11f)
            }

            if (i % 2 == 0) {
                val corePosition = Vector3(x, baseY + 1.65f, z)
                val core = addWorld(coreModel, corePosition.x, corePosition.y, corePosition.z)
                cores += Core(core, corePosition)
            }
            if (i % 3 == 1) {
                addWorld(beaconModel, x + 1.38f, baseY + 1.25f, z - 1.30f)
                addWorld(beaconLightModel, x + 1.38f, baseY + 2.32f, z - 1.30f)
            }
        }

        val gateModel = ownWorld(builder.createCylinder(3.2f, 0.45f, 0.45f, 32, edgeMaterial, 1L or 8L))
        addWorld(gateModel, 0f, 2.4f, -12f)

        // Four broken portal arches are landmarks between the islands.
        for (i in 0..3) {
            val angle = 0.4f + i * 1.72f
            val radius = 11.7f
            val x = cos(angle) * radius
            val z = sin(angle) * radius
            val y = 0.55f + (i % 2) * 0.48f
            addWorld(archPostModel, x - 1.18f, y + 1.10f, z, rotationY = -angle * 57.3f)
            addWorld(archPostModel, x + 1.18f, y + 1.10f, z, rotationY = -angle * 57.3f)
            addWorld(archTopModel, x, y + 2.32f, z, rotationY = -angle * 57.3f)
            addWorld(shardModel, x, y + 3.02f, z)
        }

        // Floating rubble and distant stars make the full 3D space feel populated.
        for (i in 0 until 18) {
            val angle = i * 2.399f
            val radius = 12.5f + (i % 5) * 1.55f
            val y = 1.0f + (i % 6) * 0.72f
            addWorld(rubbleModel, cos(angle) * radius, y, sin(angle) * radius, rotationY = i * 23f, rotationZ = (i % 4) * 12f)
        }
        for (i in 0 until 14) {
            val angle = i * 2.17f
            val radius = 18f + (i % 4) * 2.6f
            addWorld(moonModel, cos(angle) * radius, 5f + (i % 5) * 1.8f, sin(angle) * radius)
        }
        for (i in 0 until 32) {
            val angle = i * 2.399f
            val radius = 22f + (i % 7) * 2.4f
            val y = 7f + (i % 8) * 1.45f
            addWorld(starModels[i % starModels.size], cos(angle) * radius, y, sin(angle) * radius)
        }
    }

    private fun setPlayerAnimation(name: String) {
        val scene = playerScene ?: return
        if (currentAnimation == name) return
        if (scene.modelInstance.animations.none { it.id.equals(name, ignoreCase = true) }) return
        scene.animationController?.setAnimation(name, -1)
        currentAnimation = name
    }

    private fun updatePlayerTransform() {
        val scene = playerScene ?: return
        scene.modelInstance.transform.idt()
            .translate(position.x, position.y - 0.82f, position.z)
            .rotate(Vector3.Y, facingAngle)
    }

    private fun updatePlayerAnimation() {
        val movement = sqrt(touch.moveX * touch.moveX + touch.moveY * touch.moveY)
        val animation = when {
            movement < 0.08f -> "Idle_Standing"
            movement < 0.65f -> "Walk_Relaxed"
            else -> "Run"
        }
        setPlayerAnimation(animation)
        if (movement >= 0.08f) facingAngle = atan2(touch.moveX, -touch.moveY) * 57.29578f
        updatePlayerTransform()
    }

    private fun ownWorld(model: Model): Model = model.also(worldModels::add)

    private fun clearWorld() {
        objects.clear()
        cores.clear()
        worldModels.forEach { it.dispose() }
        worldModels.clear()
    }

    private fun addWorld(
        model: Model,
        x: Float,
        y: Float,
        z: Float,
        rotationY: Float = 0f,
        rotationZ: Float = 0f,
    ): ModelInstance {
        val instance = ModelInstance(model)
        instance.transform.setToTranslation(x, y, z)
        if (rotationY != 0f) instance.transform.rotate(Vector3.Y, rotationY)
        if (rotationZ != 0f) instance.transform.rotate(Vector3.Z, rotationZ)
        objects += instance
        return instance
    }

    private fun updatePlayer() {
        updatePlayerAnimation()
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
        updatePlayer()

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
        sceneManager.update(dt)
        onHud(HudState(level, collected, cores.size, score, remainingTime.toInt(), if (collected == cores.size) "عاد النور إلى المدار" else "وازن خطواتك واتبع شظايا النور"))
        Gdx.gl.glClearColor(0.02f, 0.05f, 0.11f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        batch.begin(camera)
        objects.forEach { batch.render(it, environment) }
        cores.filterNot { it.collected }.forEach { batch.render(it.instance, environment) }
        batch.end()
        sceneManager.renderColors()
    }

    override fun resize(width: Int, height: Int) {
        sceneManager.updateViewport(width.toFloat(), height.toFloat())
    }

    override fun dispose() {
        batch.dispose()
        clearWorld()
        playerScene?.animations?.stopAll()
        playerScene?.let(sceneManager::removeScene)
        sceneManager.dispose()
        heroAsset?.dispose()
        heroAsset = null
        playerScene = null
    }
}
