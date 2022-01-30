//import indigo._
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

val playerAssetName = AssetName("player")


case class Aseprite(name: String, path: String) {
  def jsonAssetName = AssetName(name + ":json")
  def jsonAsset = AssetType.Text(jsonAssetName, AssetPath(path + ".json"))
  def imageAssetName = AssetName(name + ":image")
  def imageAsset = AssetType.Image(imageAssetName, AssetPath(path + ".png"))
}

case class StartupData(
  playerSprite: Sprite[Bitmap]
)

val player = Aseprite("player", "assets/gothic-hero-run")

@JSExportTopLevel("IndigoGame")
object Game extends IndigoDemo[Unit, StartupData, Model, Unit] {
  val eventFilters: EventFilters = EventFilters.AllowAll

  override def boot(flags: Map[String, String]): Outcome[BootResult[Unit]] = {
    val r = BootResult.configOnly(GameConfig(1024, 300, 60).withMagnification(2))
    Outcome(r.addAssets(
      AssetType.Image(playerAssetName, AssetPath("assets/gothic-hero-idle.gif")),
      player.jsonAsset,
       player.imageAsset,
    ))
  }

  override def setup(bootData: Unit, assetCollection: AssetCollection, dice: Dice): Outcome[Startup[StartupData]] = {
    val ss = List(player).map { aseprite =>
      val v = for {
        json <- assetCollection.findTextDataByName(aseprite.jsonAssetName).toRight("text")
        _ = println(json)
        sprite <- Json.asepriteFromJson(json).toRight("json")
        saa <- sprite.toSpriteAndAnimations(dice, aseprite.imageAssetName).toRight("sprite")
      } yield saa
      v.fold(e => Outcome.raiseError(new RuntimeException(s"asset error: $e")), x => Outcome(aseprite -> x))
    }

    for {
      sprites <- Outcome.sequence(ss)
    } yield {
      Startup.Success(StartupData(sprites.head._2.sprite))
        .addAnimations(sprites.map(_._2.animations))
    }
  }

  override def initialModel(startupData: StartupData): Outcome[Model] =
     Outcome(Model.initial())

  override def initialViewModel(startupData: StartupData, model: Model): Outcome[Unit] =
    Outcome(())

  override def updateModel(
      context: FrameContext[StartupData],
      model: Model
  ): GlobalEvent => Outcome[Model] = {
    case FrameTick =>
      Outcome(model.update(context.delta))
    case _ =>
      Outcome(model)
  }

  override def updateViewModel(context: FrameContext[StartupData], model: Model, viewModel: Unit): GlobalEvent => Outcome[Unit] = {
    case _ =>
      Outcome(())
  }

  override def present(
      context: FrameContext[StartupData],
      model: Model,
      viewModel: Unit
  ): Outcome[SceneUpdateFragment] =
    Outcome(SceneUpdateFragment(
      //Graphic(Rectangle(0, 0, 38, 48), 1, ) +:
      context.startUpData.playerSprite.play()
      +:
      model.enemies.map { enemy =>
        Graphic(Rectangle(0, 0, 38, 48), 1, Material.Bitmap(playerAssetName))
        .moveTo(enemy.x.toInt, 0),
      }
    ))
}

case class Model(enemies: List[Enemy]) {
  def update(timeDelta: Seconds): Model =
    this.copy(enemies = enemies.map(_.update(timeDelta)))
}
object Model {
  def initial(): Model = Model(List(Enemy(500)))
}

case class Enemy(x: Double) {
  def update(timeDelta: Seconds): Enemy =
    this.copy(x = x - (timeDelta.toDouble * 10))
}
