const AppError = require('../utils/AppError');
const ErrorCodes = require('../utils/errorCodes');

// Jedino mesto koje formira JSON odgovor sa greskom (tech.md sekcija 6.7)
// eslint-disable-next-line no-unused-vars
const errorHandler = (err, req, res, next) => {
  const isAppError = err instanceof AppError;
  const code = isAppError ? err.code : 'INTERNAL_ERROR';
  const status = isAppError ? err.status : ErrorCodes.INTERNAL_ERROR.status;
  const message = isAppError ? err.message : ErrorCodes.INTERNAL_ERROR.message;

  if (!isAppError) {
    console.error(err);
  }

  const body = { error: { code, message } };
  if (isAppError && err.details !== undefined) {
    body.error.details = err.details;
  }

  res.status(status).json(body);
};

module.exports = errorHandler;
