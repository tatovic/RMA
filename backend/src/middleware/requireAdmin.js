const AppError = require('../utils/AppError');

// BR-003 — kreiranje/izmenu/brisanje kategorija sme samo ADMIN.
const requireAdmin = (req, res, next) => {
  if (req.user.role !== 'ADMIN') {
    throw new AppError('FORBIDDEN');
  }
  next();
};

module.exports = requireAdmin;
