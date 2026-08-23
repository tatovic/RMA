const usersService = require('./users.service');
const asyncHandler = require('../../utils/asyncHandler');
const { serializeUser } = require('../../utils/serializer');

const getMe = asyncHandler(async (req, res) => {
  res.status(200).json(serializeUser(req.user));
});

const updateMe = asyncHandler(async (req, res) => {
  const user = await usersService.updateProfile(req.userId, req.body);
  res.status(200).json(serializeUser(user));
});

const changePassword = asyncHandler(async (req, res) => {
  await usersService.changePassword(req.userId, req.body);
  res.status(204).send();
});

module.exports = { getMe, updateMe, changePassword };
