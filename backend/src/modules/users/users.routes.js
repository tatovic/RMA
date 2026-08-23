const { Router } = require('express');

const validate = require('../../middleware/validate');
const { updateProfileSchema, changePasswordSchema } = require('./users.schema');
const usersController = require('./users.controller');

const router = Router();

router.get('/me', usersController.getMe);
router.patch('/me', validate(updateProfileSchema), usersController.updateMe);
router.post('/me/password', validate(changePasswordSchema), usersController.changePassword);

module.exports = router;
