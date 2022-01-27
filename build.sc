import mill._
import mill.scalajslib.ScalaJSModule
import mill.scalajslib.api.ModuleKind
import mill.scalalib._

object game extends ScalaJSModule {
  def scalaVersion = "2.13.8"
  def scalaJSVersion = "1.8.0"

  def moduleKind = ModuleKind.ESModule

  override def ivyDeps = Agg(
    ivy"com.github.japgolly.scalajs-react::core::2.0.1"
  )
}
