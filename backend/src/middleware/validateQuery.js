const AppError = require('../utils/AppError');

// Validira req.query prema zod semi i zamenjuje ga parsiranim (normalizovanim) podacima.
const validateQuery = (schema) => (req, res, next) => {
  const result = schema.safeParse(req.query);

  if (!result.success) {
    const details = result.error.issues.map((issue) => ({
      field: issue.path.join('.'),
      message: issue.message,
    }));
    return next(new AppError('VALIDATION_ERROR', details));
  }

  req.query = result.data;
  next();
};

module.exports = validateQuery;
