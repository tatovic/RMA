const toISOString = (date) => {
  if (!date) return null;
  return date instanceof Date ? date.toISOString() : new Date(date).toISOString();
};

const serializeUser = (user) => ({
  id: user.id,
  name: user.name,
  email: user.email,
  role: user.role,
  isActive: Boolean(user.is_active),
  currency: user.currency,
  createdAt: toISOString(user.created_at),
});

module.exports = { toISOString, serializeUser };
