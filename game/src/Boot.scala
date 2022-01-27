import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.document

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("Game")
object Boot {
  @JSExport
  def main(): Unit = {
    val HelloMessage = ScalaComponent
      .builder[String]
      .render($ => <.div("Hello ", $.props))
      .build

    HelloMessage("atty303").renderIntoDOM(document.getElementById("root"))
  }
}
