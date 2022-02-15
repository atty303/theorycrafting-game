import mill._
import mill.scalajslib.ScalaJSModule
import mill.scalajslib.api.ModuleKind
import mill.scalalib._

object game extends ScalaJSModule {
  def scalaVersion   = "3.1.1"
  def scalaJSVersion = "1.8.0"

  override def ivyDeps = Agg(
    ivy"io.indigoengine::indigo::0.12.0",
    ivy"io.indigoengine::indigo-extras::0.12.0",
    ivy"io.indigoengine::indigo-json-circe::0.12.0",
    ivy"com.lihaoyi::pprint::0.7.0"
  )

  override def moduleKind = ModuleKind.ESModule

  def viteWorker = T.worker {
    os.proc("npx", "vite").spawn()
  }

  def dev() = T.command {
    T {
      viteWorker()
      compile()
      fastOpt()
    }
  }
}
