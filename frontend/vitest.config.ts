import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vitest/config'

/**
 * A plain Vitest setup, deliberately not a Nuxt one.
 *
 * Everything under `test/` exercises modules that are pure functions or that only touch
 * objects a test can hand them. None of it needs a Nuxt runtime, a DOM or a rendered
 * component, and pulling one in would trade a sub-second suite for a slow one that fails
 * for reasons unrelated to the code under test.
 *
 * Tests live outside `app/` on purpose: `srcDir` is `app/`, so `nuxt build` never sees
 * them and the build gate stays exactly as fast as it was.
 */
export default defineConfig({
  test: {
    include: ['test/**/*.spec.ts'],
    environment: 'node',
  },
  resolve: {
    alias: {
      '~': fileURLToPath(new URL('./app', import.meta.url)),
      '@': fileURLToPath(new URL('./app', import.meta.url)),
    },
  },
})
