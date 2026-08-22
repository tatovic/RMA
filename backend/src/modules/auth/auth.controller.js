const authService = require('./auth.service');
const asyncHandler = require('../../utils/asyncHandler');
const { serializeUser } = require('../../utils/serializer');

const register = asyncHandler(async (req, res) => {
  const { name, email, password } = req.body;
  const { user, token } = await authService.register({ name, email, password });
  res.status(201).json({ token, user: serializeUser(user) });
});

const login = asyncHandler(async (req, res) => {
  const { email, password } = req.body;
  const { user, token } = await authService.login({ email, password });
  res.status(200).json({ token, user: serializeUser(user) });
});

module.exports = { register, login };
