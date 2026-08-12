/** Liveness probe for the container healthcheck. Deliberately touches no dependency. */
export default defineEventHandler(() => ({ ok: true }))
