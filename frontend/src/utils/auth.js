/**
 * Prepares registration payload by excluding client-only fields.
 * Removes confirmPassword which is only used for client-side validation.
 * @param {Object} form - The registration form data
 * @returns {Object} Payload ready for API submission
 */
export function prepareRegisterPayload(form) {
  const { confirmPassword, ...payload } = form
  return payload
}
