package io.github.atty303.game

import indigo.IndigoSandbox
import indigo.platform.assets.AssetCollection
import indigo.shared.{FrameContext, Outcome, Startup}
import indigo.shared.animation.Animation
import indigo.shared.assets.{AssetName, AssetPath, AssetType}
import indigo.shared.config.GameConfig
import indigo.shared.datatypes.{FontInfo, Point, Rectangle}
import indigo.shared.dice.Dice
import indigo.shared.events.GlobalEvent
import indigo.shared.materials.Material
import indigo.shared.scenegraph.{Graphic, SceneUpdateFragment}
import indigo.shared.shader.Shader
import indigo.shared.temporal.Signal
import indigo.shared.time.Seconds

import scala.scalajs.js.annotation.JSExportTopLevel
import indigo.shared.events.FrameTick
import indigo.json.Json
import indigo.IndigoDemo
import indigo.shared.events.EventFilters
import indigo.BootResult
import indigo.shared.scenegraph.Sprite
import indigo.shared.materials.Material.Bitmap
import indigo.shared.formats.SpriteAndAnimations

val playerAssetName = AssetName("player")

case class StartupData(
    sprites: Map[AsepriteAsset, SpriteAndAnimations]
)

enum AsepriteAsset(name: String, path: String):
  def jsonAssetName  = AssetName(name + ":json")
  def jsonAsset      = AssetType.Text(jsonAssetName, AssetPath(s"assets/${path}.json"))
  def imageAssetName = AssetName(name + ":image")
  def imageAsset     = AssetType.Image(imageAssetName, AssetPath(s"assets/${path}.png"))

  def load(dice: Dice, assetCollection: AssetCollection): Outcome[SpriteAndAnimations] =
    (for {
      json <- assetCollection.findTextDataByName(jsonAssetName).toRight("failed to load json")
      aes  <- Json.asepriteFromJson(json).toRight("failed to parse json")
      saa  <- aes.toSpriteAndAnimations(dice, imageAssetName).toRight("failed to create sprite")
    } yield saa).fold(
      e => Outcome.raiseError(new RuntimeException(s"asset load error: $e")),
      x => Outcome(x)
    )

  case Player extends AsepriteAsset("player", "gothic-hero-run")

@JSExportTopLevel("IndigoGame")
object Game extends IndigoDemo[Unit, StartupData, Model, Unit] {
  val eventFilters: EventFilters = EventFilters.AllowAll

  override def boot(flags: Map[String, String]): Outcome[BootResult[Unit]] = {
    val r = BootResult.configOnly(GameConfig(1280, 800, 60).withMagnification(2))
    Outcome(
      r.addAssets(
        AssetType.Image(playerAssetName, AssetPath("assets/gothic-hero-idle.gif"))
      ).addAssets(AsepriteAsset.values.toSet.flatMap(x => Set(x.jsonAsset, x.imageAsset)))
    )
  }

  override def setup(
      bootData: Unit,
      assetCollection: AssetCollection,
      dice: Dice
  ): Outcome[Startup[StartupData]] = {
    val ss = AsepriteAsset.values.toList.map { aseprite =>
      aseprite.load(dice, assetCollection).map(aseprite -> _)
    }

    for {
      sprites <- Outcome.sequence(ss)
    } yield Startup
      .Success(StartupData(sprites.toMap))
      .addAnimations(sprites.map(_._2.animations))
  }

  override def initialModel(startupData: StartupData): Outcome[Model] =
    Outcome(Model.initial(startupData))

  override def initialViewModel(startupData: StartupData, model: Model): Outcome[Unit] =
    Outcome(())

  override def updateModel(
      context: FrameContext[StartupData],
      model: Model
  ): GlobalEvent => Outcome[Model] =
    model.update(context.delta)

  override def updateViewModel(
      context: FrameContext[StartupData],
      model: Model,
      viewModel: Unit
  ): GlobalEvent => Outcome[Unit] = { case _ =>
    Outcome(())
  }

  override def present(
      context: FrameContext[StartupData],
      model: Model,
      viewModel: Unit
  ): Outcome[SceneUpdateFragment] =
    Outcome(
      SceneUpdateFragment(
        // Graphic(Rectangle(0, 0, 38, 48), 1, ) +:
        context.startUpData.sprites(AsepriteAsset.Player).sprite.play()
          +:
            model.enemies.map { enemy =>
              Graphic(
                Rectangle(0, 0, 38, 48),
                1,
                Material.Bitmap(playerAssetName)
              )
                .moveTo(enemy.x.toInt, 0),
            }
      )
    )
}

case class Model(player: model.Player, enemies: List[Enemy]) {
  def update(timeDelta: Seconds): GlobalEvent => Outcome[Model] =
    case e =>
      for {
        player <- player.update(timeDelta)(e)
      } yield this.copy(player = player, enemies = enemies.map(_.update(timeDelta)))
}
object Model                                                 {
  def initial(startUpData: StartupData): Model = Model(
    player = model.Player(startUpData.sprites(AsepriteAsset.Player), model.DefaultAttack.init),
    enemies = List(Enemy(500))
  )
}

case class Enemy(x: Double) {
  def update(timeDelta: Seconds): Enemy =
    this.copy(x = x - (timeDelta.toDouble * 10))
}
