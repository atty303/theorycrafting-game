import mill._
import mill.scalajslib.ScalaJSModule
import mill.scalajslib.api.ModuleKind
import mill.scalalib._
import $ivy.`io.indigoengine::mill-indigo:0.12.0`, millindigo._

object game extends ScalaJSModule with MillIndigo {
  def scalaVersion   = "3.1.1"
  def scalaJSVersion = "1.8.0"

  val gameAssetsDirectory: os.Path   = os.pwd / "assets"
  val showCursor: Boolean            = true
  val title: String                  = "Theorycrafting Game"
  val windowStartWidth: Int          = 720 // Width of Electron window, used with `indigoRun`.
  val windowStartHeight: Int         = 480 // Height of Electron window, used with `indigoRun`.
  val disableFrameRateLimit: Boolean = false

  override def ivyDeps = Agg(
    ivy"io.indigoengine::indigo::0.12.0",
    ivy"io.indigoengine::indigo-extras::0.12.0",
    ivy"io.indigoengine::indigo-json-circe::0.12.0",
    ivy"com.lihaoyi::pprint::0.7.0"
  )

  def buildGame() = T.command {
    T {
      compile()
      fastOpt()
      indigoBuild()()
    }
  }
}
