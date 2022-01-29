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

val playerAssetName = AssetName("player")

@JSExportTopLevel("IndigoGame")
object Game extends IndigoSandbox[Unit, Unit] {
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

  override def initialModel(startupData: Unit): Outcome[Unit] = Outcome(())

  override def updateModel(
      context: FrameContext[Unit],
      model: Unit
  ): GlobalEvent => Outcome[Unit] = _ => Outcome(())

  override def present(
      context: FrameContext[Unit],
      model: Unit
  ): Outcome[SceneUpdateFragment] =
    Outcome(SceneUpdateFragment(
      Graphic(Rectangle(0, 0, 38, 48), 1, Material.Bitmap(playerAssetName))
        .moveTo(Signal.Linear(Seconds(10)).map(t => Point((t * 100).toInt, 0)).at(context.running))
    ))
}
