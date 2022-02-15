import { IndigoGame } from "@sjs/out.js";

IndigoGame.launch("root");

if (import.meta.hot) {
  import.meta.hot.accept();
}
