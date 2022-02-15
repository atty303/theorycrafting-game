import { defineConfig } from "vite";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig(({ command, mode }) => {
  const sjs = mode === "production"
    ? path.resolve(__dirname, "out/game/fullOpt.dest")
    : path.resolve(__dirname, "out/game/fastOpt.dest");

  return {
    resolve: {
      alias: [
        { find: "@sjs", replacement: sjs },
      ],
    },
    // server: {
    //   watch: {
    //     ignored: [
    //       function (_path) {
    //         return !(_path.includes("fullOpt.dest") || _path.includes("fastOpt.dest"));
    //       }
    //     ]
    //   }
    // },
    plugins: [],
  };
});
