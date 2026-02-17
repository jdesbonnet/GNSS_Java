import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';

export default defineConfig({
  // Use relative asset URLs so built files work when hosted from a
  // subdirectory (for example, GitHub Pages project sites).
  base: './',
  plugins: [svelte()],
  worker: {
    format: 'es'
  }
});
