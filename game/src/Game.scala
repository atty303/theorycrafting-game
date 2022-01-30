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

val playerAssetName = AssetName("player")

@JSExportTopLevel("IndigoGame")
object Game extends IndigoSandbox[Unit, Model] {
  val config: GameConfig = GameConfig(1024, 200, 60).withMagnification(2)
  val animations: Set[Animation] = Set.empty
  val assets: Set[AssetType] = Set(
    AssetType.Image(playerAssetName, AssetPath("assets/gothic-hero-idle.gif"))
  )
  val fonts: Set[FontInfo] = Set.empty
  val shaders: Set[Shader] = Set.empty

  def setup(
      assetCollection: AssetCollection,
      dice: Dice
  ): Outcome[Startup[Unit]] = Outcome(Startup.Success(()))

  override def initialModel(startupData: Unit): Outcome[Model] =
     Outcome(Model.initial())

  override def updateModel(
      context: FrameContext[Unit],
      model: Model
  ): GlobalEvent => Outcome[Model] = {
    case FrameTick =>
      Outcome(model.update(context.delta))
    case _ =>
      Outcome(model)
  }

  override def present(
      context: FrameContext[Unit],
      model: Model
  ): Outcome[SceneUpdateFragment] =
    Outcome(SceneUpdateFragment(
      Graphic(Rectangle(0, 0, 38, 48), 1, Material.Bitmap(playerAssetName)) +:
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
