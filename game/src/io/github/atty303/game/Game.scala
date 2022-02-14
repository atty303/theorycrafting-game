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
import indigo.shared.scenegraph.{Clip, Graphic, SceneUpdateFragment}
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
import io.github.atty303.game.model.DefaultAttack
import indigo.shared.animation.CycleLabel
import indigo.shared.time.GameTime

val playerAssetName = AssetName("player")

case class StartupData(
    sprites: Map[AsepriteAsset, Map[CycleLabel, Clip[Bitmap]]]
)

enum AsepriteAsset(name: String, path: String):
  def jsonAssetName  = AssetName(name + ":json")
  def jsonAsset      = AssetType.Text(jsonAssetName, AssetPath(s"assets/${path}.json"))
  def imageAssetName = AssetName(name + ":image")
  def imageAsset     = AssetType.Image(imageAssetName, AssetPath(s"assets/${path}.png"))

  def load(dice: Dice, assetCollection: AssetCollection): Outcome[Map[CycleLabel, Clip[Bitmap]]] =
    (for {
      json <- assetCollection.findTextDataByName(jsonAssetName).toRight("failed to load json")
      aes  <- Json.asepriteFromJson(json).toRight("failed to parse json")
      clip <- aes.toClips(imageAssetName).toRight("failed to create clip")
    } yield clip).fold(
      e => Outcome.raiseError(new RuntimeException(s"asset load error: $e")),
      x => Outcome(x)
    )

  case Player extends AsepriteAsset("player", "gothic-hero")

@JSExportTopLevel("IndigoGame")
object Game extends IndigoDemo[Unit, StartupData, Model, ViewModel] {
  val eventFilters: EventFilters = EventFilters.AllowAll

  override def boot(flags: Map[String, String]): Outcome[BootResult[Unit]] = {
    val r = BootResult.configOnly(GameConfig(1280, 800).withMagnification(2))
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
  }

  override def initialModel(startupData: StartupData): Outcome[Model] =
    Outcome(Model.initial(startupData))

  override def initialViewModel(startupData: StartupData, model: Model): Outcome[ViewModel] =
    Outcome(ViewModel.initial())

  override def updateModel(
      context: FrameContext[StartupData],
      model: Model
  ): GlobalEvent => Outcome[Model] =
    model.update(context.gameTime, context.delta)

  override def updateViewModel(
      context: FrameContext[StartupData],
      model: Model,
      viewModel: ViewModel
  ): GlobalEvent => Outcome[ViewModel] = {
    case DefaultAttack.Events.Ready =>
      Outcome(viewModel.copy(isDefaultAttackActivated = viewModel.isDefaultAttackActivated.reset()))
    case e @ FrameTick              =>
      for {
        a <- viewModel.isDefaultAttackActivated.update(timeDelta = context.delta)(e)
      } yield viewModel.copy(isDefaultAttackActivated = a)
    case _                          =>
      Outcome(viewModel)
  }

  override def present(
      context: FrameContext[StartupData],
      model: Model,
      viewModel: ViewModel
  ): Outcome[SceneUpdateFragment] =
    for {
      suf <- ModelView.draw(context.gameTime, model, viewModel)
    } yield suf

    // Outcome(
    //   SceneUpdateFragment(
    // context.startUpData.sprites(AsepriteAsset.Player).sprite.play()
    // +:
    //   model.enemies.map { enemy =>
    //     Graphic(Rectangle(0, 0, 38, 48), 1, Material.Bitmap(playerAssetName))
    //       .moveTo(enemy.x.toInt, 0),
    //   }
    //   )
    // )
}

case class Model(player: model.Player, enemies: List[Enemy]) {
  def update(gameTime: GameTime, timeDelta: Seconds): GlobalEvent => Outcome[Model] =
    case e =>
      for {
        player <- player.update(gameTime, timeDelta)(e)
      } yield this.copy(player = player, enemies = enemies.map(_.update(timeDelta)))
}

object ModelView:
  def draw(gameTime: GameTime, m: Model, viewModel: ViewModel): Outcome[SceneUpdateFragment] =
    model.PlayerView.draw(m.player, viewModel.isDefaultAttackActivated)

object Model {
  def initial(startUpData: StartupData): Model = Model(
    player = model.Player.initial(startUpData.sprites(AsepriteAsset.Player)),
    enemies = List(Enemy(500))
  )
}

case class Enemy(x: Double) {
  def update(timeDelta: Seconds): Enemy =
    this.copy(x = x - (timeDelta.toDouble * 10))
}

final case class ViewModel(
    isDefaultAttackActivated: Pulse = Pulse()
)

object ViewModel:
  def initial(): ViewModel = ViewModel()

final case class Pulse(count: Option[Int] = None):
  def reset(): Pulse                                            = Pulse(Some(2))
  def isActive: Boolean                                         = count.isDefined
  def update(timeDelta: Seconds): GlobalEvent => Outcome[Pulse] =
    case FrameTick =>
      val c = count.map(_ - 1) match {
        case Some(0) => None
        case x       => x
      }
      Outcome(this.copy(count = c))
