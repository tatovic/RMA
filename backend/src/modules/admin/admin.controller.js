const adminService = require('./admin.service');
const asyncHandler = require('../../utils/asyncHandler');
const { serializeAdminUser } = require('../../utils/serializer');

const getStats = asyncHandler(async (req, res) => {
  const stats = await adminService.getStats();
  res.status(200).json(stats);
});

const listUsers = asyncHandler(async (req, res) => {
  const users = await adminService.listUsers();
  res.status(200).json(users.map(serializeAdminUser));
});

const updateUserStatus = asyncHandler(async (req, res) => {
  const user = await adminService.updateUserStatus(req.userId, req.params.id, req.body.isActive);
  res.status(200).json(serializeAdminUser(user));
});

module.exports = { getStats, listUsers, updateUserStatus };
