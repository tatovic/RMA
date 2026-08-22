const { Router } = require('express');

const validate = require('../../middleware/validate');
const { registerSchema, loginSchema } = require('./auth.schema');
const authController = require('./auth.controller');

const router = Router();

router.post('/register', validate(registerSchema), authController.register);
router.post('/login', validate(loginSchema), authController.login);

module.exports = router;
