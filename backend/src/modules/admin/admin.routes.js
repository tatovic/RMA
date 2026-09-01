const { Router } = require('express');

const requireAdmin = require('../../middleware/requireAdmin');
const validate = require('../../middleware/validate');
const { updateUserStatusSchema } = require('./admin.schema');
const adminController = require('./admin.controller');

const router = Router();

// FR-106 — sve administratorske rute su zaštićene rolom ADMIN, ostali dobijaju 403.
router.use(requireAdmin);

router.get('/stats', adminController.getStats);
router.get('/users', adminController.listUsers);
router.patch('/users/:id/status', validate(updateUserStatusSchema), adminController.updateUserStatus);

module.exports = router;
