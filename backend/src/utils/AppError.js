const ErrorCodes = require('./errorCodes');

class AppError extends Error {
  constructor(code, details) {
    const known = ErrorCodes[code] || ErrorCodes.INTERNAL_ERROR;
    super(known.message);
    this.code = ErrorCodes[code] ? code : 'INTERNAL_ERROR';
    this.status = known.status;
    this.details = details;
  }
}

module.exports = AppError;
