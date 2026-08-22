const AppError = require('../utils/AppError');

const notFound = (req, res, next) => {
  next(new AppError('NOT_FOUND'));
};

module.exports = notFound;
