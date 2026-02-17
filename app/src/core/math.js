/** Normalize angle in degrees to [-180, 180). */
export function normalizeAngleDeg180(angle) {
  const wrapped = ((angle + 180) % 360 + 360) % 360;
  return wrapped - 180;
}

/** Normalize angle in degrees to [0, 360). */
export function normalizeAngleDeg360(angle) {
  return ((angle % 360) + 360) % 360;
}
