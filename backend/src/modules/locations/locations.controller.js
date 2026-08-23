const locationsService = require('./locations.service');
const asyncHandler = require('../../utils/asyncHandler');
const { serializeLocation } = require('../../utils/serializer');

const list = asyncHandler(async (req, res) => {
  const locations = await locationsService.listLocations(req.userId);
  res.status(200).json({ locations: locations.map(serializeLocation) });
});

const create = asyncHandler(async (req, res) => {
  const { location, created } = await locationsService.createLocation(req.userId, req.body);
  res.status(created ? 201 : 200).json(serializeLocation(location));
});

const update = asyncHandler(async (req, res) => {
  const location = await locationsService.updateLocation(req.userId, req.params.id, req.body);
  res.status(200).json(serializeLocation(location));
});

const remove = asyncHandler(async (req, res) => {
  await locationsService.deleteLocation(req.userId, req.params.id);
  res.status(204).send();
});

module.exports = { list, create, update, remove };
