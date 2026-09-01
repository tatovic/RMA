const { z } = require('zod');

// BR-004 — telo nosi samo željeni status, server proverava self-deaktivaciju.
const updateUserStatusSchema = z.object({
  isActive: z.boolean({ required_error: 'isActive je obavezan', invalid_type_error: 'isActive mora biti tačno/netačno' }),
});

module.exports = { updateUserStatusSchema };
