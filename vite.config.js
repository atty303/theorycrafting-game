import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig(({ command, mode }) => {
  const sjs = mode === "production"
      ? path.resolve(__dirname, "out/game/fullOpt.dest")
      : path.resolve(__dirname, "out/game/fastOpt.dest");

  return {
    root: "webapp",
    resolve: {
      alias: [
        { find: "@sjs", replacement: sjs },
      ],
    },
    plugins: [react()],
  };
});
