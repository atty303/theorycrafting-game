import { Game } from "@sjs/out.js";

Game.main();

if (import.meta.hot) {
    import.meta.hot.accept();
}
