const toISOString = (date) => {
  if (!date) return null;
  return date instanceof Date ? date.toISOString() : new Date(date).toISOString();
};

// DATE kolone (bez vremena) — db.md sekcija 2.3A, format YYYY-MM-DD.
const toDateOnlyString = (date) => {
  if (!date) return null;
  if (typeof date === 'string') return date.slice(0, 10);
  return date.toISOString().slice(0, 10);
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

// Mapiranje kolona — db.md sekcija 7. Svako odstupanje je bag.
const serializeItem = (item) => ({
  id: item.id,
  userId: item.user_id,
  name: item.name,
  description: item.description,
  categoryId: item.category_id,
  locationId: item.location_id,
  manufacturer: item.manufacturer,
  model: item.model,
  serialNumber: item.serial_number,
  quantity: item.quantity,
  purchasePrice: item.purchase_price === null || item.purchase_price === undefined ? null : Number(item.purchase_price),
  estimatedValue:
    item.estimated_value === null || item.estimated_value === undefined ? null : Number(item.estimated_value),
  currency: item.currency,
  purchaseDate: toDateOnlyString(item.purchase_date),
  warrantyExpirationDate: toDateOnlyString(item.warranty_expiration_date),
  seller: item.seller,
  notes: item.notes,
  createdAt: toISOString(item.created_at),
  updatedAt: toISOString(item.updated_at),
  deletedAt: toISOString(item.deleted_at),
});

// BR-003 — itemCount je globalan (svi korisnici), kategorije su zajedničke.
const serializeCategory = (category) => ({
  id: category.id,
  name: category.name,
  description: category.description,
  iconKey: category.icon_key,
  sortOrder: category.sort_order,
  itemCount: category.item_count,
  createdAt: toISOString(category.created_at),
  updatedAt: toISOString(category.updated_at),
});

const serializeLocation = (location) => ({
  id: location.id,
  userId: location.user_id,
  name: location.name,
  description: location.description,
  itemCount: location.item_count,
  createdAt: toISOString(location.created_at),
  updatedAt: toISOString(location.updated_at),
});

module.exports = {
  toISOString,
  toDateOnlyString,
  serializeUser,
  serializeItem,
  serializeCategory,
  serializeLocation,
};
