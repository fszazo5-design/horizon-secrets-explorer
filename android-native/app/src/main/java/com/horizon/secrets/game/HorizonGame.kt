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
    private val objects = mutableListOf<ModelInstance>()
    private val cores = mutableListOf<Core>()
    private val worldModels = mutableListOf<Model>()
    private val heroModels = mutableListOf<HeroModels>()
    private var playerRig: PlayerRig? = null
    private var selectedCharacter = 0
    private val position = Vector3(0f, 1.25f, 5f)
    private var velocityY = 0f
    private var level = 0
    private var score = 0
    private var elapsed = 0f
    private var remainingTime = 145f
    private var levelCompleteSent = false
    private val colors = listOf(
        Color(0.10f, 0.62f, 0.72f, 1f),
        Color(0.42f, 0.28f, 0.72f, 1f),
        Color(0.10f, 0.72f, 0.50f, 1f),
        Color(0.86f, 0.45f, 0.20f, 1f),
    )
    private val characterStyles = listOf(
        CharacterStyle(Color(0.12f, 0.42f, 0.66f, 1f), Color(0.10f, 0.84f, 1f, 1f), Color(0.98f, 0.78f, 0.38f, 1f), 0),
        CharacterStyle(Color(0.32f, 0.22f, 0.62f, 1f), Color(0.72f, 0.48f, 1f, 1f), Color(0.96f, 0.78f, 0.42f, 1f), 1),
        CharacterStyle(Color(0.62f, 0.32f, 0.16f, 1f), Color(1f, 0.62f, 0.20f, 1f), Color(0.96f, 0.82f, 0.58f, 1f), 2),
        CharacterStyle(Color(0.12f, 0.48f, 0.40f, 1f), Color(0.32f, 1f, 0.78f, 1f), Color(0.82f, 0.94f, 0.90f, 1f), 3),
    )

    private data class Core(val instance: ModelInstance, val position: Vector3, var collected: Boolean = false)
    private data class CharacterStyle(val body: Color, val accent: Color, val face: Color, val accessory: Int)
    private data class HeroModels(
        val torso: Model,
        val head: Model,
        val arm: Model,
        val leg: Model,
        val accessory: Model,
        val visor: Model,
        val accessoryY: Float,
        val accessoryZ: Float,
    )
    private data class PlayerRig(
        val torso: ModelInstance,
        val head: ModelInstance,
        val leftArm: ModelInstance,
        val rightArm: ModelInstance,
        val leftLeg: ModelInstance,
        val rightLeg: ModelInstance,
        val accessory: ModelInstance,
        val visor: ModelInstance,
    )

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
        createHeroModels()
        startLevel(0)
        selectCharacter(0)
    }

    /** Must be called on LibGDX's render thread. */
    fun selectCharacter(index: Int) {
        if (heroModels.isEmpty()) return
        selectedCharacter = index.coerceIn(0, heroModels.lastIndex)
        val models = heroModels[selectedCharacter]
        playerRig = PlayerRig(
            torso = ModelInstance(models.torso),
            head = ModelInstance(models.head),
            leftArm = ModelInstance(models.arm),
            rightArm = ModelInstance(models.arm),
            leftLeg = ModelInstance(models.leg),
            rightLeg = ModelInstance(models.leg),
            accessory = ModelInstance(models.accessory),
            visor = ModelInstance(models.visor),
        )
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

    private fun createHeroModels() {
        val builder = ModelBuilder()
        val attributes = 1L or 8L
        characterStyles.forEach { style ->
            val bodyMaterial = Material(ColorAttribute.createDiffuse(style.body))
            val accentMaterial = Material(ColorAttribute.createDiffuse(style.accent))
            val faceMaterial = Material(ColorAttribute.createDiffuse(style.face))
            val darkMaterial = Material(ColorAttribute.createDiffuse(Color(0.055f, 0.10f, 0.17f, 1f)))
            val torso = when (style.accessory) {
                1 -> builder.createBox(0.78f, 0.82f, 0.46f, bodyMaterial, attributes)
                2 -> builder.createCylinder(0.54f, 0.86f, 0.42f, 10, bodyMaterial, attributes)
                3 -> builder.createBox(0.60f, 0.88f, 0.40f, bodyMaterial, attributes)
                else -> builder.createBox(0.68f, 0.82f, 0.42f, bodyMaterial, attributes)
            }
            heroModels += HeroModels(
                torso = ownHero(torso),
                head = ownHero(builder.createSphere(0.64f, 0.64f, 0.64f, 12, 12, faceMaterial, attributes)),
                arm = ownHero(builder.createCylinder(0.22f, 0.72f, 0.22f, 8, accentMaterial, attributes)),
                leg = ownHero(builder.createBox(0.27f, 0.62f, 0.30f, darkMaterial, attributes)),
                accessory = ownHero(when (style.accessory) {
                    0 -> builder.createBox(0.82f, 0.96f, 0.14f, accentMaterial, attributes) // explorer's short energy cape
                    1 -> builder.createBox(1.02f, 0.36f, 0.56f, accentMaterial, attributes) // orbit guard shoulder armor
                    2 -> builder.createCone(0.86f, 0.62f, 0.86f, 8, accentMaterial, attributes) // desert wanderer hood
                    else -> builder.createCone(0.42f, 0.72f, 0.42f, 6, accentMaterial, attributes) // crystal crest
                }),
                visor = ownHero(builder.createBox(0.38f, 0.13f, 0.07f, accentMaterial, attributes)),
                accessoryY = if (style.accessory == 3) 1.20f else if (style.accessory == 2) 0.92f else if (style.accessory == 1) 0.34f else 0.02f,
                accessoryZ = if (style.accessory == 0) -0.31f else 0f,
            )
        }
    }

    private fun ownWorld(model: Model): Model = model.also(worldModels::add)
    private fun ownHero(model: Model): Model = model.also { heroModelsOwned += it }
    private val heroModelsOwned = mutableListOf<Model>()

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

    private fun place(instance: ModelInstance, x: Float, y: Float, z: Float, rotationZ: Float = 0f) {
        instance.transform.setToTranslation(x, y, z)
        if (rotationZ != 0f) instance.transform.rotate(Vector3.Z, rotationZ)
    }

    private fun updatePlayerRig() {
        val rig = playerRig ?: return
        val moving = kotlin.math.abs(touch.moveX) + kotlin.math.abs(touch.moveY) > 0.08f
        val swing = if (moving) sin(elapsed * 9f) * 22f else sin(elapsed * 2f) * 3f
        val bob = if (moving) kotlin.math.abs(sin(elapsed * 9f)) * 0.035f else 0f
        val y = position.y + bob
        place(rig.torso, position.x, y + 0.06f, position.z)
        place(rig.head, position.x, y + 0.79f, position.z)
        place(rig.leftArm, position.x - 0.48f, y + 0.10f, position.z, 34f + swing)
        place(rig.rightArm, position.x + 0.48f, y + 0.10f, position.z, -34f - swing)
        place(rig.leftLeg, position.x - 0.18f, y - 0.49f, position.z, -swing * 0.55f)
        place(rig.rightLeg, position.x + 0.18f, y - 0.49f, position.z, swing * 0.55f)
        val hero = heroModels[selectedCharacter]
        place(rig.accessory, position.x, y + hero.accessoryY, position.z + hero.accessoryZ)
        place(rig.visor, position.x, y + 0.81f, position.z + 0.33f)
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
        updatePlayerRig()

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
        playerRig?.let { rig ->
            batch.render(rig.accessory, environment)
            batch.render(rig.leftLeg, environment)
            batch.render(rig.rightLeg, environment)
            batch.render(rig.torso, environment)
            batch.render(rig.leftArm, environment)
            batch.render(rig.rightArm, environment)
            batch.render(rig.head, environment)
            batch.render(rig.visor, environment)
        }
        cores.filterNot { it.collected }.forEach { batch.render(it.instance, environment) }
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
    }

    override fun dispose() {
        batch.dispose()
        clearWorld()
        heroModelsOwned.forEach { it.dispose() }
        heroModelsOwned.clear()
        heroModels.clear()
    }
}
