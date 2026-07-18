/**
 * Copies the complete backend registration contract out of Vue form state.
 * @param {Object} form - The registration form data
 * @returns {Object} Payload ready for API submission
 */
export function prepareRegisterPayload(form) {
  return { ...form }
}
