import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { keycloakify } from "keycloakify/vite-plugin";

export default defineConfig({
  resolve: {
    alias: {
      "@lucide": fileURLToPath(new URL("./node_modules/lucide-static/icons", import.meta.url))
    }
  },
  plugins: [
    react(),
    keycloakify({
      themeName: "project-template-theme",
      accountThemeImplementation: "none"
    })
  ]
});
